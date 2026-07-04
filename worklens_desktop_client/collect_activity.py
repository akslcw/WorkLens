from __future__ import annotations

import argparse
import json
import time
from datetime import datetime
from pathlib import Path

from worklens_desktop_client.activity_tracker import ActivityRecord, ActivityTracker
from worklens_desktop_client.windows_activity import Win32ActivityProbe


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Collect foreground app activity on Windows and merge it into usage records."
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
        "--duration-seconds",
        type=int,
        default=None,
        help="Optional total runtime for the collector. If omitted, run until Ctrl+C.",
    )
    parser.add_argument(
        "--output-json",
        default=None,
        help="Optional path to write merged records as JSON.",
    )
    return parser.parse_args()


def record_to_dict(record: ActivityRecord) -> dict[str, str]:
    return {
        "appName": record.app_name,
        "startedAt": record.started_at.isoformat(timespec="seconds"),
        "endedAt": record.ended_at.isoformat(timespec="seconds"),
    }


def print_records(records: list[ActivityRecord]) -> None:
    if not records:
        print("No activity records collected.")
        return

    print("Merged activity records:")
    for record in records:
        print(
            f"- {record.app_name}: "
            f"{record.started_at.isoformat(timespec='seconds')} -> "
            f"{record.ended_at.isoformat(timespec='seconds')}"
        )


def write_records(records: list[ActivityRecord], output_path: str) -> None:
    path = Path(output_path)
    payload = [record_to_dict(record) for record in records]
    path.write_text(json.dumps(payload, indent=2), encoding="utf-8")
    print(f"Wrote {len(records)} records to {path}")


def main() -> None:
    args = parse_args()
    tracker = ActivityTracker()
    probe = Win32ActivityProbe(idle_threshold_seconds=args.idle_threshold_seconds)
    started_at = time.time()

    print(
        "Collecting activity. "
        f"sample_interval={args.sample_interval_seconds}s, "
        f"idle_threshold={args.idle_threshold_seconds}s. "
        "Press Ctrl+C to stop."
    )

    try:
        while True:
            observed_at = datetime.now().replace(microsecond=0)
            app_name = probe.sample_app_name()
            tracker.observe(app_name, observed_at)
            print(f"[{observed_at.isoformat(timespec='seconds')}] {app_name}")

            if args.duration_seconds is not None and time.time() - started_at >= args.duration_seconds:
                break
            time.sleep(args.sample_interval_seconds)
    except KeyboardInterrupt:
        print("Stopping activity collection.")

    finished_at = datetime.now().replace(microsecond=0)
    records = tracker.finish(finished_at)
    print_records(records)
    if args.output_json:
        write_records(records, args.output_json)


if __name__ == "__main__":
    main()
