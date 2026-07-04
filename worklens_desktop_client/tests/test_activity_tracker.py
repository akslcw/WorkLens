import unittest
from datetime import datetime

from worklens_desktop_client.activity_tracker import ActivityTracker


class ActivityTrackerTests(unittest.TestCase):

    def test_consecutive_samples_for_same_app_merge_into_one_record(self) -> None:
        tracker = ActivityTracker()

        tracker.observe("chrome.exe", datetime.fromisoformat("2026-07-04T13:00:00"))
        tracker.observe("chrome.exe", datetime.fromisoformat("2026-07-04T13:00:05"))
        tracker.observe("chrome.exe", datetime.fromisoformat("2026-07-04T13:00:10"))
        records = tracker.finish(datetime.fromisoformat("2026-07-04T13:00:15"))

        self.assertEqual(1, len(records))
        self.assertEqual("chrome.exe", records[0].app_name)
        self.assertEqual(datetime.fromisoformat("2026-07-04T13:00:00"), records[0].started_at)
        self.assertEqual(datetime.fromisoformat("2026-07-04T13:00:15"), records[0].ended_at)

    def test_switching_apps_closes_previous_record_and_starts_next(self) -> None:
        tracker = ActivityTracker()

        tracker.observe("chrome.exe", datetime.fromisoformat("2026-07-04T13:00:00"))
        tracker.observe("slack.exe", datetime.fromisoformat("2026-07-04T13:00:05"))
        records = tracker.finish(datetime.fromisoformat("2026-07-04T13:00:10"))

        self.assertEqual(2, len(records))
        self.assertEqual("chrome.exe", records[0].app_name)
        self.assertEqual(datetime.fromisoformat("2026-07-04T13:00:00"), records[0].started_at)
        self.assertEqual(datetime.fromisoformat("2026-07-04T13:00:05"), records[0].ended_at)
        self.assertEqual("slack.exe", records[1].app_name)
        self.assertEqual(datetime.fromisoformat("2026-07-04T13:00:05"), records[1].started_at)
        self.assertEqual(datetime.fromisoformat("2026-07-04T13:00:10"), records[1].ended_at)

    def test_idle_samples_are_recorded_as_idle(self) -> None:
        tracker = ActivityTracker()

        tracker.observe("chrome.exe", datetime.fromisoformat("2026-07-04T13:00:00"))
        tracker.observe("Idle", datetime.fromisoformat("2026-07-04T13:05:00"))
        tracker.observe("Idle", datetime.fromisoformat("2026-07-04T13:05:05"))
        records = tracker.finish(datetime.fromisoformat("2026-07-04T13:05:10"))

        self.assertEqual(2, len(records))
        self.assertEqual("chrome.exe", records[0].app_name)
        self.assertEqual(datetime.fromisoformat("2026-07-04T13:05:00"), records[0].ended_at)
        self.assertEqual("Idle", records[1].app_name)
        self.assertEqual(datetime.fromisoformat("2026-07-04T13:05:00"), records[1].started_at)
        self.assertEqual(datetime.fromisoformat("2026-07-04T13:05:10"), records[1].ended_at)


if __name__ == "__main__":
    unittest.main()
