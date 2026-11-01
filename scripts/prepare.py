#!/usr/bin/env python3
"""Build an offline SQLite corpus from an existing ganjoor-data checkout."""
import argparse
import hashlib
import json
import re
import sqlite3
import subprocess
from pathlib import Path


def normalize(text):
    text = text.translate(str.maketrans({'ي': 'ی', 'ى': 'ی', 'ك': 'ک', '\u200c': ' ', 'ـ': ''}))
    return re.sub(r'\s+', ' ', re.sub('[\u064b-\u065f\u0670]', '', text)).strip()


def build(source, output, strict=True):
    manifest = json.loads((source / 'manifest.json').read_text())
    output.parent.mkdir(parents=True, exist_ok=True)
    temporary = output.with_suffix('.tmp')
    temporary.unlink(missing_ok=True)
    db = sqlite3.connect(temporary)
    db.executescript('''
        CREATE TABLE poets(id INTEGER PRIMARY KEY, name TEXT NOT NULL, slug TEXT UNIQUE NOT NULL);
        CREATE TABLE categories(id INTEGER PRIMARY KEY, poet INTEGER NOT NULL, parent INTEGER, title TEXT NOT NULL);
        CREATE TABLE poems(id INTEGER PRIMARY KEY, poet INTEGER NOT NULL, category INTEGER NOT NULL, title TEXT NOT NULL, body TEXT NOT NULL, url TEXT NOT NULL, source TEXT NOT NULL);
        CREATE VIRTUAL TABLE search USING fts4(title,body,tokenize=unicode61);
        CREATE INDEX category_poet ON categories(poet,parent);
        CREATE INDEX poem_category ON poems(category,id);
    ''')
    for poet in manifest['Poets']:
        slug = poet['FullUrl'].strip('/')
        db.execute('INSERT INTO poets VALUES(?,?,?)', (poet['Id'], poet['Nickname'], slug))
        for path in sorted((source / 'poets' / slug).rglob('*.json')):
            item = json.loads(path.read_text())
            if path.name == '_cat.json':
                db.execute('INSERT INTO categories VALUES(?,?,?,?)',
                           (item['Id'], poet['Id'], item.get('ParentId'), item['Title']))
            elif 'Verses' in item:
                verses = sorted(item['Verses'], key=lambda v: v['VOrder'])
                body = '\n'.join(v['Text'] for v in verses)
                if not body.strip():
                    raise ValueError(f'Empty poem: {path}')
                db.execute('INSERT INTO poems VALUES(?,?,?,?,?,?,?)',
                           (item['Id'], poet['Id'], item['CatId'], item['Title'], body,
                            'https://ganjoor.net' + item['FullUrl'], item.get('SourceName', 'گنجور')))
                db.execute('INSERT INTO search(docid,title,body) VALUES(?,?,?)',
                           (item['Id'], normalize(item['Title']), normalize(body)))
    counts = {t: db.execute(f'SELECT count(*) FROM {t}').fetchone()[0] for t in ('poets', 'poems')}
    if strict and (counts['poets'] != manifest['PoetsCount'] or counts['poems'] != manifest['PoemsCount']):
        raise ValueError(f'Incomplete export: {counts}; expected {manifest["PoetsCount"]} poets / {manifest["PoemsCount"]} poems')
    if db.execute('SELECT count(*) FROM poems p LEFT JOIN categories c ON c.id=p.category WHERE c.id IS NULL').fetchone()[0]:
        raise ValueError('Poems with missing categories')
    db.execute("INSERT INTO search(search) VALUES('optimize')")
    db.commit()
    assert db.execute('PRAGMA integrity_check').fetchone()[0] == 'ok'
    db.execute('VACUUM')
    db.close()
    temporary.replace(output)
    try:
        revision = subprocess.check_output(['git', '-C', str(source), 'rev-parse', 'HEAD'], stderr=subprocess.DEVNULL, text=True).strip()
    except subprocess.CalledProcessError:
        revision = 'local-export'
    report = dict(counts, generated=manifest['GeneratedAtUtc'], source_revision=revision,
                  bytes=output.stat().st_size, sha256=hashlib.sha256(output.read_bytes()).hexdigest())
    (output.parent / 'data-version.json').write_text(json.dumps(report, ensure_ascii=False, indent=2))
    print(json.dumps(report, ensure_ascii=False))
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', required=True, type=Path)
    parser.add_argument('--output', type=Path, default=Path('app/src/main/assets/poetry.db'))
    args = parser.parse_args()
    build(args.source, args.output)
