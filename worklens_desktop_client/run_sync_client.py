from __future__ import annotations

import argparse
import getpass
import time
from datetime import datetime
from pathlib import Path

from worklens_desktop_client.activity_tracker import ActivityTracker
from worklens_desktop_client.api_client import WorkLensApiClient
from worklens_desktop_client.local_store import LocalRecordStore
from worklens_desktop_client.sync_service import SyncService
from worklens_desktop_client.windows_activity import Win32ActivityProbe


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Collect Windows app activity and upload it to WorkLens every 5 minutes."
    )
    parser.add_argument(
        "--base-url",
        default="http://localhost:8080",
        help="WorkLens backend base URL. Default: http://localhost:8080",
    )
    parser.add_argument(
        "--sample-interval-seconds",
        type=int,
        default=5,
        help="How often to sample the foreground app. Default: 5",
    )
    parser.add_argument(
        "--idle-threshold-seconds",
        type=int,
        default=300,
        help="How long without keyboard or mouse input counts as idle. Default: 300",
    )
    parser.add_argument(
        "--upload-interval-seconds",
        type=int,
        default=300,
        help="How often to upload merged records. Default: 300",
    )
    parser.add_argument(
        "--cache-db",
        default=str(Path(__file__).resolve().parent / "cache.sqlite3"),
        help="SQLite file used to cache records when upload fails.",
    )
    parser.add_argument(
        "--duration-seconds",
        type=int,
        default=None,
        help="Optional total runtime for smoke testing. If omitted, run until Ctrl+C.",
    )
    return parser.parse_args()


def flush_records(sync_service: SyncService, token: str, tracker: ActivityTracker, flush_at: datetime) -> None:
    records = tracker.cutoff(flush_at)
    report = sync_service.upload_batch(token, records)
    print(
        f"[{flush_at.isoformat(timespec='seconds')}] "
        f"flush complete: uploaded={report.uploaded_count}, cached={report.cached_count}"
    )


def main() -> None:
    args = parse_args()
    client = WorkLensApiClient(args.base_url)
    tracker = ActivityTracker()
    probe = Win32ActivityProbe(idle_threshold_seconds=args.idle_threshold_seconds)
    store = LocalRecordStore(args.cache_db)
    sync_service = SyncService(client, store)

    username = input("Username: ").strip()
    password = getpass.getpass("Password: ")
    login_result = client.login(username, password)
    if login_result.role != "EMPLOYEE":
        raise SystemExit("Only EMPLOYEE accounts can run the desktop collector.")

    print(f"Login succeeded for {login_result.username} ({login_result.role}).")
    startup_report = sync_service.upload_batch(login_result.token, [])
    print(f"Startup retry complete: uploaded={startup_report.uploaded_count}, cached={startup_report.cached_count}")

    started_at = time.time()
    next_upload_at = time.time() + args.upload_interval_seconds

    print(
        "Running sync client. "
        f"sample_interval={args.sample_interval_seconds}s, "
        f"idle_threshold={args.idle_threshold_seconds}s, "
        f"upload_interval={args.upload_interval_seconds}s. "
        "Press Ctrl+C to stop."
    )

    try:
        while True:
            observed_at = datetime.now().replace(microsecond=0)
            app_name = probe.sample_app_name()
            tracker.observe(app_name, observed_at)
            print(f"[{observed_at.isoformat(timespec='seconds')}] sampled {app_name}")

            if time.time() >= next_upload_at:
                flush_records(sync_service, login_result.token, tracker, observed_at)
                next_upload_at = time.time() + args.upload_interval_seconds

            if args.duration_seconds is not None and time.time() - started_at >= args.duration_seconds:
                break
            time.sleep(args.sample_interval_seconds)
    except KeyboardInterrupt:
        print("Stopping sync client.")

    finished_at = datetime.now().replace(microsecond=0)
    flush_records(sync_service, login_result.token, tracker, finished_at)


if __name__ == "__main__":
    main()
