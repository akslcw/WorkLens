import threading
import unittest

from worklens_desktop_client.background_runner import BackgroundRunner


class BackgroundRunnerTests(unittest.TestCase):

    def test_runner_reports_running_and_stopped_for_normal_completion(self) -> None:
        statuses: list[str] = []
        release_event = threading.Event()

        def worker(stop_event: threading.Event) -> None:
            release_event.wait(timeout=1)

        runner = BackgroundRunner(worker=worker, on_status_change=statuses.append)

        runner.start()
        self.assertTrue(runner.wait_until_running(timeout=1))
        release_event.set()
        runner.join(timeout=1)

        self.assertEqual(["RUNNING", "STOPPED"], statuses)
        self.assertIsNone(runner.last_error)

    def test_runner_reports_stopped_and_captures_error_when_worker_fails(self) -> None:
        statuses: list[str] = []

        def worker(stop_event: threading.Event) -> None:
            raise RuntimeError("boom")

        runner = BackgroundRunner(worker=worker, on_status_change=statuses.append)

        runner.start()
        runner.join(timeout=1)

        self.assertEqual(["RUNNING", "STOPPED"], statuses)
        self.assertIsInstance(runner.last_error, RuntimeError)
        self.assertEqual("boom", str(runner.last_error))


if __name__ == "__main__":
    unittest.main()
