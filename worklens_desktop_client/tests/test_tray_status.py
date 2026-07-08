import unittest

from worklens_desktop_client.tray_status import format_status_menu_text
from worklens_desktop_client.tray_status import format_tray_title


class TrayStatusTests(unittest.TestCase):

    def test_status_menu_text_includes_running_state_and_display_name(self) -> None:
        self.assertEqual(
            "状态: 运行中 / 用户: Alice Chen",
            format_status_menu_text("RUNNING", "Alice Chen"),
        )

    def test_status_menu_text_includes_stopped_state_and_display_name(self) -> None:
        self.assertEqual(
            "状态: 已停止 / 用户: Alice Chen",
            format_status_menu_text("STOPPED", "Alice Chen"),
        )

    def test_tray_title_includes_display_name(self) -> None:
        self.assertEqual(
            "WorkLens (RUNNING) - Alice Chen",
            format_tray_title("RUNNING", "Alice Chen"),
        )


if __name__ == "__main__":
    unittest.main()
