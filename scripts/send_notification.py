#!/usr/bin/env python3
"""Send an authenticated FCM data message; credentials stay on the operator's machine.

pip install google-auth requests
Use Application Default Credentials with Firebase Messaging permissions.
Defaults to validation only. --send is required to deliver the message.
"""
import argparse
import json
import re


def payload(title, body, deliver=False):
    if not title.strip() or not body.strip():
        raise ValueError('Title and body are required')
    data = {'validate_only': not deliver, 'message': {
        'topic': 'parseh-news', 'data': {'title': title, 'body': body},
        'android': {'priority': 'normal', 'ttl': '86400s'}}}
    if len(json.dumps(data, ensure_ascii=False).encode()) > 3500:
        raise ValueError('Notification text is too long')
    return data


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--project', required=True)
    parser.add_argument('--title', required=True)
    parser.add_argument('--body', required=True)
    parser.add_argument('--send', action='store_true')
    args = parser.parse_args()
    if not re.fullmatch(r'[a-z][a-z0-9-]{4,61}[a-z0-9]', args.project):
        parser.error('Invalid Firebase project ID')
    import google.auth
    from google.auth.transport.requests import AuthorizedSession
    credentials, _ = google.auth.default(scopes=['https://www.googleapis.com/auth/firebase.messaging'])
    response = AuthorizedSession(credentials).post(
        f'https://fcm.googleapis.com/v1/projects/{args.project}/messages:send',
        json=payload(args.title, args.body, args.send), timeout=30)
    response.raise_for_status()
    print('Message sent.' if args.send else 'Validation succeeded; no message sent.')
