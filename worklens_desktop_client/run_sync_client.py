from __future__ import annotations

import argparse
import getpass
from pathlib import Path

from worklens_desktop_client.sync_runtime import SyncRuntime
from worklens_desktop_client.sync_runtime import SyncRuntimeConfig


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

def main() -> None:
    args = parse_args()
    username = input("Username: ").strip()
    password = getpass.getpass("Password: ")
    runtime = SyncRuntime(
        SyncRuntimeConfig(
            base_url=args.base_url,
            sample_interval_seconds=args.sample_interval_seconds,
            idle_threshold_seconds=args.idle_threshold_seconds,
            upload_interval_seconds=args.upload_interval_seconds,
            cache_db=args.cache_db,
        )
    )
    try:
        import threading

        runtime.run(
            username=username,
            password=password,
            stop_event=threading.Event(),
            duration_seconds=args.duration_seconds,
        )
    except KeyboardInterrupt:
        print("Stopping sync client.")


if __name__ == "__main__":
    main()
