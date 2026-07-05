import tempfile
import unittest
from datetime import datetime
from pathlib import Path

import requests

from worklens_desktop_client.activity_tracker import ActivityRecord
from worklens_desktop_client.local_store import LocalRecordStore
from worklens_desktop_client.sync_service import SyncService


class FakeApiClient:
    def __init__(self, failures_before_success: int = 0) -> None:
        self.failures_before_success = failures_before_success
        self.calls: list[tuple[str, str, datetime, datetime]] = []

    def create_usage_record(self, token: str, app_name: str, started_at: datetime, ended_at: datetime) -> dict:
        self.calls.append((token, app_name, started_at, ended_at))
        if self.failures_before_success > 0:
            self.failures_before_success -= 1
            raise requests.ConnectionError("network down")
        return {
            "id": len(self.calls),
            "appName": app_name,
            "startedAt": started_at.isoformat(timespec="seconds"),
            "endedAt": ended_at.isoformat(timespec="seconds"),
        }


class SyncServiceTests(unittest.TestCase):

    def test_failed_upload_caches_new_records(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            store = LocalRecordStore(str(Path(temp_dir) / "cache.sqlite3"))
            api_client = FakeApiClient(failures_before_success=1)
            service = SyncService(api_client, store)
            records = [
                ActivityRecord(
                    app_name="chrome.exe",
                    started_at=datetime.fromisoformat("2026-07-04T14:00:00"),
                    ended_at=datetime.fromisoformat("2026-07-04T14:05:00"),
                )
            ]

            report = service.upload_batch("token-1", records)

            self.assertEqual(0, report.uploaded_count)
            self.assertEqual(1, report.cached_count)
            pending_records = store.list_pending_records()
            self.assertEqual(1, len(pending_records))
            self.assertEqual("chrome.exe", pending_records[0].app_name)

    def test_successful_upload_flushes_pending_before_new_records(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            store = LocalRecordStore(str(Path(temp_dir) / "cache.sqlite3"))
            store.add_records([
                ActivityRecord(
                    app_name="Idle",
                    started_at=datetime.fromisoformat("2026-07-04T13:50:00"),
                    ended_at=datetime.fromisoformat("2026-07-04T13:55:00"),
                )
            ])
            api_client = FakeApiClient()
            service = SyncService(api_client, store)
            new_records = [
                ActivityRecord(
                    app_name="chrome.exe",
                    started_at=datetime.fromisoformat("2026-07-04T14:00:00"),
                    ended_at=datetime.fromisoformat("2026-07-04T14:05:00"),
                )
            ]

            report = service.upload_batch("token-2", new_records)

            self.assertEqual(2, report.uploaded_count)
            self.assertEqual(0, report.cached_count)
            self.assertEqual(0, len(store.list_pending_records()))
            self.assertEqual(
                [
                    ("token-2", "Idle", datetime.fromisoformat("2026-07-04T13:50:00"), datetime.fromisoformat("2026-07-04T13:55:00")),
                    ("token-2", "chrome.exe", datetime.fromisoformat("2026-07-04T14:00:00"), datetime.fromisoformat("2026-07-04T14:05:00")),
                ],
                api_client.calls,
            )

    def test_partial_failure_keeps_unsent_pending_and_caches_remaining_new_records(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            store = LocalRecordStore(str(Path(temp_dir) / "cache.sqlite3"))
            store.add_records([
                ActivityRecord(
                    app_name="Idle",
                    started_at=datetime.fromisoformat("2026-07-04T13:50:00"),
                    ended_at=datetime.fromisoformat("2026-07-04T13:55:00"),
                )
            ])
            api_client = FakeApiClient(failures_before_success=1)
            service = SyncService(api_client, store)
            new_records = [
                ActivityRecord(
                    app_name="chrome.exe",
                    started_at=datetime.fromisoformat("2026-07-04T14:00:00"),
                    ended_at=datetime.fromisoformat("2026-07-04T14:05:00"),
                )
            ]

            report = service.upload_batch("token-3", new_records)

            self.assertEqual(0, report.uploaded_count)
            self.assertEqual(2, report.cached_count)
            pending_records = store.list_pending_records()
            self.assertEqual(2, len(pending_records))
            self.assertEqual("Idle", pending_records[0].app_name)
            self.assertEqual("chrome.exe", pending_records[1].app_name)


if __name__ == "__main__":
    unittest.main()
