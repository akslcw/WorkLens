from __future__ import annotations

import sqlite3
from contextlib import closing
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path

from worklens_desktop_client.activity_tracker import ActivityRecord


@dataclass(frozen=True)
class PendingRecord:
    local_id: int
    app_name: str
    started_at: datetime
    ended_at: datetime


class LocalRecordStore:
    def __init__(self, database_path: str) -> None:
        self._database_path = Path(database_path)
        self._database_path.parent.mkdir(parents=True, exist_ok=True)
        self._initialize()

    def add_records(self, records: list[ActivityRecord]) -> None:
        if not records:
            return
        with closing(sqlite3.connect(self._database_path)) as connection:
            connection.executemany(
                """
                INSERT INTO pending_usage_records (app_name, started_at, ended_at)
                VALUES (?, ?, ?)
                """,
                [
                    (
                        record.app_name,
                        record.started_at.isoformat(timespec="seconds"),
                        record.ended_at.isoformat(timespec="seconds"),
                    )
                    for record in records
                ],
            )
            connection.commit()

    def list_pending_records(self) -> list[PendingRecord]:
        with closing(sqlite3.connect(self._database_path)) as connection:
            rows = connection.execute(
                """
                SELECT id, app_name, started_at, ended_at
                FROM pending_usage_records
                ORDER BY id ASC
                """
            ).fetchall()
        return [
            PendingRecord(
                local_id=row[0],
                app_name=row[1],
                started_at=datetime.fromisoformat(row[2]),
                ended_at=datetime.fromisoformat(row[3]),
            )
            for row in rows
        ]

    def delete_records(self, local_ids: list[int]) -> None:
        if not local_ids:
            return
        placeholders = ",".join("?" for _ in local_ids)
        with closing(sqlite3.connect(self._database_path)) as connection:
            connection.execute(
                f"DELETE FROM pending_usage_records WHERE id IN ({placeholders})",
                local_ids,
            )
            connection.commit()

    def _initialize(self) -> None:
        with closing(sqlite3.connect(self._database_path)) as connection:
            connection.execute(
                """
                CREATE TABLE IF NOT EXISTS pending_usage_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    app_name TEXT NOT NULL,
                    started_at TEXT NOT NULL,
                    ended_at TEXT NOT NULL
                )
                """
            )
            connection.commit()
