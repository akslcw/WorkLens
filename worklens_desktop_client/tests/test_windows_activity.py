import unittest

from worklens_desktop_client.windows_activity import elapsed_millis_32bit


class WindowsActivityTests(unittest.TestCase):

    def test_elapsed_millis_handles_get_tick_count_wraparound(self) -> None:
        self.assertEqual(32, elapsed_millis_32bit(0x00000010, 0xFFFFFFF0))


if __name__ == "__main__":
    unittest.main()
