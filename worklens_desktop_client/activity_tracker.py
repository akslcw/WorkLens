from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime


@dataclass(frozen=True)
class ActivityRecord:
    app_name: str
    started_at: datetime
    ended_at: datetime


class ActivityTracker:
    def __init__(self) -> None:
        self._completed_records: list[ActivityRecord] = []
        self._current_app_name: str | None = None
        self._current_started_at: datetime | None = None

    def observe(self, app_name: str, observed_at: datetime) -> None:
        if self._current_app_name is None:
            self._current_app_name = app_name
            self._current_started_at = observed_at
            return

        if app_name == self._current_app_name:
            return

        self._completed_records.append(
            ActivityRecord(
                app_name=self._current_app_name,
                started_at=self._current_started_at,
                ended_at=observed_at,
            )
        )
        self._current_app_name = app_name
        self._current_started_at = observed_at

    def finish(self, finished_at: datetime) -> list[ActivityRecord]:
        records = list(self._completed_records)
        if self._current_app_name is not None and self._current_started_at is not None:
            records.append(
                ActivityRecord(
                    app_name=self._current_app_name,
                    started_at=self._current_started_at,
                    ended_at=finished_at,
                )
            )
        return records
