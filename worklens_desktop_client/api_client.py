from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
from typing import Any

import requests


@dataclass(frozen=True)
class LoginResult:
    token: str
    username: str
    role: str


class WorkLensApiClient:
    def __init__(self, base_url: str, session: requests.Session | None = None) -> None:
        self._base_url = base_url.rstrip("/")
        self._session = session or requests.Session()

    def login(self, username: str, password: str) -> LoginResult:
        response = self._session.post(
            f"{self._base_url}/auth/login",
            json={
                "username": username,
                "password": password,
            },
            timeout=10,
        )
        response.raise_for_status()
        payload = response.json()
        return LoginResult(
            token=payload["token"],
            username=payload["username"],
            role=payload["role"],
        )

    def create_usage_record(self, token: str, app_name: str, started_at: datetime, ended_at: datetime) -> dict[str, Any]:
        response = self._session.post(
            f"{self._base_url}/usage-records",
            headers={
                "Authorization": f"Bearer {token}",
            },
            json={
                "appName": app_name,
                "startedAt": started_at.isoformat(timespec="seconds"),
                "endedAt": ended_at.isoformat(timespec="seconds"),
            },
            timeout=10,
        )
        response.raise_for_status()
        return response.json()
