import importlib.util
import json
import sqlite3
import tempfile
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location('prepare', Path(__file__).parents[1] / 'scripts' / 'prepare.py')
prepare = importlib.util.module_from_spec(spec)
spec.loader.exec_module(prepare)


class ImportTests(unittest.TestCase):
    def source(self, root, count=1):
        (root / 'manifest.json').write_text(json.dumps({'GeneratedAtUtc': '2026-10-24', 'PoetsCount': 1, 'PoemsCount': count, 'Poets': [{'Id': 2, 'Nickname': 'حافظ', 'FullUrl': '/hafez'}]}))
        folder = root / 'poets/hafez/ghazal'
        folder.mkdir(parents=True)
        (folder / '_cat.json').write_text(json.dumps({'Id': 24, 'Title': 'غزلیات', 'ParentId': 0}))
        (folder / 'sh1.json').write_text(json.dumps({'Id': 2130, 'CatId': 24, 'Title': 'غزل ۱', 'FullUrl': '/hafez/ghazal/sh1', 'SourceName': 'گنجور', 'Verses': [{'VOrder': 2, 'Text': 'دل'}, {'VOrder': 1, 'Text': 'كِتاب يار'}]}))

    def test_import_order_provenance_and_normalized_search(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.source(root)
            report = prepare.build(root, root / 'output/poetry.db')
            with sqlite3.connect(root / 'output/poetry.db') as db:
                self.assertEqual(db.execute('SELECT body FROM poems').fetchone()[0], 'كِتاب يار\nدل')
                self.assertEqual(db.execute('SELECT docid FROM search WHERE search MATCH ?', ('"کتاب" "یار"',)).fetchone()[0], 2130)
                prepare.check_database(db)
            self.assertEqual(report['poems'], 1)
            self.assertEqual(len(report['sha256']), 64)

    def test_incomplete_corpus_fails(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.source(root, 2)
            with self.assertRaisesRegex(ValueError, 'Incomplete export'):
                prepare.build(root, root / 'poetry.db')
            self.assertFalse((root / 'poetry.db').exists())

    def test_normalize(self):
        self.assertEqual(prepare.normalize(' كِتابِ\u200cيـار  '), 'کتاب یار')
        self.assertEqual(prepare.normalize('آسان، إِنسان!'), 'اسان انسان')

    def test_empty_source_entry_is_preserved_and_reported(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.source(root)
            file = root / 'poets/hafez/ghazal/sh1.json'
            entry = json.loads(file.read_text())
            entry['Verses'] = []
            file.write_text(json.dumps(entry))
            report = prepare.build(root, root / 'poetry.db')
            self.assertEqual(report['poems'], 1)
            self.assertEqual(report['empty_poems'], 1)


if __name__ == '__main__':
    unittest.main()
