from __future__ import annotations

import argparse
import getpass
from datetime import datetime

from worklens_desktop_client.api_client import WorkLensApiClient


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Log in to WorkLens and manually report one usage record."
    )
    parser.add_argument(
        "--base-url",
        default="http://localhost:8080",
        help="WorkLens backend base URL. Default: http://localhost:8080",
    )
    return parser.parse_args()


def prompt_datetime(label: str) -> datetime:
    raw_value = input(f"{label} (YYYY-MM-DDTHH:MM:SS): ").strip()
    return datetime.fromisoformat(raw_value)


def main() -> None:
    args = parse_args()
    client = WorkLensApiClient(args.base_url)

    username = input("Username: ").strip()
    password = getpass.getpass("Password: ")
    login_result = client.login(username, password)

    app_name = input("App name: ").strip()
    started_at = prompt_datetime("Started at")
    ended_at = prompt_datetime("Ended at")
    usage_record = client.create_usage_record(
        token=login_result.token,
        app_name=app_name,
        started_at=started_at,
        ended_at=ended_at,
    )

    print(f"Login succeeded for {login_result.username} ({login_result.role}).")
    print(
        "Usage record created: "
        f"id={usage_record['id']}, appName={usage_record['appName']}, "
        f"startedAt={usage_record['startedAt']}, endedAt={usage_record['endedAt']}"
    )


if __name__ == "__main__":
    main()
