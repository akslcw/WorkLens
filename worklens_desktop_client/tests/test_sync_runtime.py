import threading
import unittest
from types import SimpleNamespace
from unittest.mock import patch

from worklens_desktop_client.api_client import LoginResult
from worklens_desktop_client.sync_runtime import SyncRuntime
from worklens_desktop_client.sync_runtime import SyncRuntimeConfig


class FakeApiClient:
    def __init__(self, base_url: str) -> None:
        self.base_url = base_url

    def login(self, username: str, password: str) -> LoginResult:
        return LoginResult(
            token="token-1",
            username=username,
            display_name="Alice Chen",
            role="EMPLOYEE",
        )


class FakeSyncService:
    def __init__(self, client, store) -> None:
        self.client = client
        self.store = store

    def upload_batch(self, token, records):
        return SimpleNamespace(
            uploaded_count=0,
            cached_count=0,
            failure_code=None,
            failure_message=None,
        )


class FakeActivityTracker:
    def observe(self, app_name, observed_at) -> None:
        pass

    def cutoff(self, flush_at):
        return []


class FakeActivityProbe:
    def __init__(self, idle_threshold_seconds: int) -> None:
        self.idle_threshold_seconds = idle_threshold_seconds

    def sample_app_name(self) -> str:
        return "Chrome"


class SyncRuntimeTests(unittest.TestCase):

    def test_run_reports_logged_in_display_name(self) -> None:
        login_results: list[LoginResult] = []
        runtime = SyncRuntime(
            SyncRuntimeConfig(
                base_url="http://localhost:8080",
                sample_interval_seconds=1,
                idle_threshold_seconds=300,
                upload_interval_seconds=300,
                cache_db=":memory:",
            ),
            logger=lambda message: None,
            on_login=login_results.append,
        )

        with patch("worklens_desktop_client.sync_runtime.WorkLensApiClient", FakeApiClient), \
                patch("worklens_desktop_client.sync_runtime.SyncService", FakeSyncService), \
                patch("worklens_desktop_client.sync_runtime.ActivityTracker", FakeActivityTracker), \
                patch("worklens_desktop_client.sync_runtime.Win32ActivityProbe", FakeActivityProbe), \
                patch("worklens_desktop_client.sync_runtime.LocalRecordStore", lambda cache_db: object()):
            runtime.run(
                username="employee.alice",
                password="Password123!",
                stop_event=threading.Event(),
                duration_seconds=0,
            )

        self.assertEqual(1, len(login_results))
        self.assertEqual("Alice Chen", login_results[0].display_name)


if __name__ == "__main__":
    unittest.main()
