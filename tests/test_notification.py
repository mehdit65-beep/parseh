import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('send_notification', Path(__file__).parents[1] / 'scripts/send_notification.py')
sender = importlib.util.module_from_spec(spec)
spec.loader.exec_module(sender)


class NotificationPayloadTests(unittest.TestCase):
    def test_dry_run_by_default_and_data_only(self):
        payload = sender.payload('پارسه', 'فرصتی برای شعر')
        self.assertTrue(payload['validate_only'])
        self.assertNotIn('notification', payload['message'])
        self.assertEqual(payload['message']['data']['title'], 'پارسه')

    def test_explicit_delivery(self):
        self.assertFalse(sender.payload('پارسه', 'پیام', True)['validate_only'])

    def test_reject_blank_or_oversized_messages(self):
        with self.assertRaises(ValueError):
            sender.payload('', 'متن')
        with self.assertRaises(ValueError):
            sender.payload('عنوان', 'ش' * 3000)
