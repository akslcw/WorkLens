from __future__ import annotations

import threading
import time
from dataclasses import dataclass
from datetime import datetime

from worklens_desktop_client.activity_tracker import ActivityTracker
from worklens_desktop_client.api_client import LoginError
from worklens_desktop_client.api_client import LoginResult
from worklens_desktop_client.api_client import WorkLensApiClient
from worklens_desktop_client.local_store import LocalRecordStore
from worklens_desktop_client.sync_service import SyncService
from worklens_desktop_client.windows_activity import Win32ActivityProbe


@dataclass(frozen=True)
class SyncRuntimeConfig:
    base_url: str
    sample_interval_seconds: int
    idle_threshold_seconds: int
    upload_interval_seconds: int
    cache_db: str


class SyncRuntime:
    def __init__(self, config: SyncRuntimeConfig, logger=None, on_login=None) -> None:
        self._config = config
        self._logger = logger or (lambda message: print(message))
        self._on_login = on_login or (lambda login_result: None)

    def login(self, username: str, password: str) -> LoginResult:
        login_result = WorkLensApiClient(self._config.base_url).login(username, password)
        self._validate_login_result(login_result)
        return login_result

    def run(
        self,
        username: str,
        password: str,
        stop_event: threading.Event,
        duration_seconds: int | None = None,
        login_result: LoginResult | None = None,
    ) -> None:
        client = WorkLensApiClient(self._config.base_url)
        tracker = ActivityTracker()
        probe = Win32ActivityProbe(idle_threshold_seconds=self._config.idle_threshold_seconds)
        store = LocalRecordStore(self._config.cache_db)
        sync_service = SyncService(client, store)

        if login_result is None:
            login_result = client.login(username, password)
        self._validate_login_result(login_result)

        self._on_login(login_result)
        self._logger(f"Login succeeded for {login_result.username} ({login_result.role}).")
        startup_report = sync_service.upload_batch(login_result.token, [])
        self._logger(
            f"Startup retry complete: uploaded={startup_report.uploaded_count}, cached={startup_report.cached_count}"
        )
        self._log_upload_failure(startup_report)

        started_at = time.time()
        next_upload_at = time.time() + self._config.upload_interval_seconds

        self._logger(
            "Running sync client. "
            f"sample_interval={self._config.sample_interval_seconds}s, "
            f"idle_threshold={self._config.idle_threshold_seconds}s, "
            f"upload_interval={self._config.upload_interval_seconds}s."
        )

        while not stop_event.is_set():
            observed_at = datetime.now().replace(microsecond=0)
            app_name = probe.sample_app_name()
            tracker.observe(app_name, observed_at)
            self._logger(f"[{observed_at.isoformat(timespec='seconds')}] sampled {app_name}")

            if time.time() >= next_upload_at:
                self._flush_records(sync_service, login_result.token, tracker, observed_at)
                next_upload_at = time.time() + self._config.upload_interval_seconds

            if duration_seconds is not None and time.time() - started_at >= duration_seconds:
                break
            if stop_event.wait(self._config.sample_interval_seconds):
                break

        finished_at = datetime.now().replace(microsecond=0)
        self._flush_records(sync_service, login_result.token, tracker, finished_at)

    def _flush_records(
        self,
        sync_service: SyncService,
        token: str,
        tracker: ActivityTracker,
        flush_at: datetime,
    ) -> None:
        records = tracker.cutoff(flush_at)
        report = sync_service.upload_batch(token, records)
        self._logger(
            f"[{flush_at.isoformat(timespec='seconds')}] "
            f"flush complete: uploaded={report.uploaded_count}, cached={report.cached_count}"
        )
        self._log_upload_failure(report)

    def _log_upload_failure(self, report) -> None:
        if report.failure_code == "PASSWORD_CHANGE_REQUIRED":
            self._logger(
                "Upload blocked: current account must change password in the web app before desktop uploads can continue. "
                "Records remain cached locally and will retry after the password is changed."
            )

    @staticmethod
    def _validate_login_result(login_result: LoginResult) -> None:
        if login_result.must_change_password:
            raise LoginError("该账号必须先在网页端修改密码，然后才能启动桌面采集。")
        if login_result.role != "EMPLOYEE":
            raise LoginError("只有员工账号可以运行桌面采集客户端。")
