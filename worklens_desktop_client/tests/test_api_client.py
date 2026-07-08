import unittest
from datetime import datetime
from unittest.mock import Mock

from worklens_desktop_client.api_client import WorkLensApiClient


class WorkLensApiClientTests(unittest.TestCase):

    def test_login_posts_credentials_and_returns_token(self) -> None:
        session = Mock()
        response = Mock()
        response.json.return_value = {
            "token": "abc123",
            "username": "employee.alice",
            "displayName": "Alice Chen",
            "role": "EMPLOYEE",
        }
        session.post.return_value = response
        client = WorkLensApiClient("http://localhost:8080", session=session)

        login_result = client.login("employee.alice", "Password123!")

        self.assertEqual("abc123", login_result.token)
        self.assertEqual("employee.alice", login_result.username)
        self.assertEqual("Alice Chen", login_result.display_name)
        self.assertEqual("EMPLOYEE", login_result.role)
        session.post.assert_called_once_with(
            "http://localhost:8080/auth/login",
            json={
                "username": "employee.alice",
                "password": "Password123!",
            },
            timeout=10,
        )
        response.raise_for_status.assert_called_once()

    def test_login_falls_back_to_username_when_display_name_is_missing(self) -> None:
        session = Mock()
        response = Mock()
        response.json.return_value = {
            "token": "abc123",
            "username": "employee.alice",
            "role": "EMPLOYEE",
        }
        session.post.return_value = response
        client = WorkLensApiClient("http://localhost:8080", session=session)

        login_result = client.login("employee.alice", "Password123!")

        self.assertEqual("employee.alice", login_result.display_name)

    def test_create_usage_record_posts_without_employee_id(self) -> None:
        session = Mock()
        response = Mock()
        response.json.return_value = {
            "id": 7,
            "appName": "Manual Test App",
            "startedAt": "2026-07-04T12:00:00",
            "endedAt": "2026-07-04T12:05:00",
            "createdAt": "2026-07-04T12:05:01",
        }
        session.post.return_value = response
        client = WorkLensApiClient("http://localhost:8080", session=session)
        started_at = datetime.fromisoformat("2026-07-04T12:00:00")
        ended_at = datetime.fromisoformat("2026-07-04T12:05:00")

        usage_record = client.create_usage_record(
            token="abc123",
            app_name="Manual Test App",
            started_at=started_at,
            ended_at=ended_at,
        )

        self.assertEqual(7, usage_record["id"])
        session.post.assert_called_once_with(
            "http://localhost:8080/usage-records",
            headers={
                "Authorization": "Bearer abc123",
            },
            json={
                "appName": "Manual Test App",
                "startedAt": "2026-07-04T12:00:00",
                "endedAt": "2026-07-04T12:05:00",
            },
            timeout=10,
        )
        response.raise_for_status.assert_called_once()


if __name__ == "__main__":
    unittest.main()
