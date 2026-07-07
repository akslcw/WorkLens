from __future__ import annotations

from dataclasses import dataclass

import requests

from worklens_desktop_client.activity_tracker import ActivityRecord
from worklens_desktop_client.local_store import LocalRecordStore


@dataclass(frozen=True)
class UploadBatchReport:
    uploaded_count: int
    cached_count: int
    failure_code: str | None = None
    failure_message: str | None = None


class SyncService:
    def __init__(self, api_client, local_store: LocalRecordStore) -> None:
        self._api_client = api_client
        self._local_store = local_store

    def upload_batch(self, token: str, new_records: list[ActivityRecord]) -> UploadBatchReport:
        uploaded_count = 0
        pending_records = self._local_store.list_pending_records()
        completed_pending_ids: list[int] = []

        for pending_record in pending_records:
            try:
                self._api_client.create_usage_record(
                    token=token,
                    app_name=pending_record.app_name,
                    started_at=pending_record.started_at,
                    ended_at=pending_record.ended_at,
                )
            except requests.RequestException as error:
                self._local_store.delete_records(completed_pending_ids)
                self._local_store.add_records(new_records)
                failure = self._classify_failure(error)
                return UploadBatchReport(
                    uploaded_count=uploaded_count,
                    cached_count=len(self._local_store.list_pending_records()),
                    failure_code=failure[0],
                    failure_message=failure[1],
                )
            completed_pending_ids.append(pending_record.local_id)
            uploaded_count += 1

        self._local_store.delete_records(completed_pending_ids)

        for index, record in enumerate(new_records):
            try:
                self._api_client.create_usage_record(
                    token=token,
                    app_name=record.app_name,
                    started_at=record.started_at,
                    ended_at=record.ended_at,
                )
            except requests.RequestException as error:
                self._local_store.add_records(new_records[index:])
                failure = self._classify_failure(error)
                return UploadBatchReport(
                    uploaded_count=uploaded_count,
                    cached_count=len(self._local_store.list_pending_records()),
                    failure_code=failure[0],
                    failure_message=failure[1],
                )
            uploaded_count += 1

        return UploadBatchReport(
            uploaded_count=uploaded_count,
            cached_count=len(self._local_store.list_pending_records()),
        )

    def _classify_failure(self, error: requests.RequestException) -> tuple[str | None, str | None]:
        response = getattr(error, "response", None)
        if response is None:
            return None, None

        response_text = response.text or ""
        if response.status_code == 403 and "Password change required" in response_text:
            return (
                "PASSWORD_CHANGE_REQUIRED",
                "Current account must change password in the web app before desktop uploads can continue.",
            )
        return None, None
