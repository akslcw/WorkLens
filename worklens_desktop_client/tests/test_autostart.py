import unittest
from pathlib import Path
from unittest.mock import MagicMock
from unittest.mock import patch

from worklens_desktop_client import autostart


class AutostartTests(unittest.TestCase):

    def test_command_quotes_executable_path_with_spaces(self) -> None:
        command = autostart.build_autostart_command(
            Path(r"C:\Users\Employee\WorkLens Client\WorkLens.exe")
        )

        self.assertEqual(
            r'"C:\Users\Employee\WorkLens Client\WorkLens.exe"',
            command,
        )

    @patch.object(autostart.winreg, "OpenKey", side_effect=FileNotFoundError)
    def test_missing_registry_value_means_disabled(self, open_key: MagicMock) -> None:
        enabled = autostart.is_autostart_enabled(Path(r"C:\WorkLens\WorkLens.exe"))

        self.assertFalse(enabled)
        open_key.assert_called_once()

    @patch.object(autostart.winreg, "SetValueEx")
    @patch.object(autostart.winreg, "CreateKeyEx")
    def test_enabling_writes_current_user_run_value(
        self,
        create_key: MagicMock,
        set_value: MagicMock,
    ) -> None:
        key = MagicMock()
        create_key.return_value.__enter__.return_value = key

        autostart.set_autostart_enabled(True, Path(r"C:\WorkLens\WorkLens.exe"))

        set_value.assert_called_once_with(
            key,
            autostart.VALUE_NAME,
            0,
            autostart.winreg.REG_SZ,
            r"C:\WorkLens\WorkLens.exe",
        )

    @patch.object(autostart.winreg, "DeleteValue")
    @patch.object(autostart.winreg, "OpenKey")
    def test_disabling_deletes_current_user_run_value(
        self,
        open_key: MagicMock,
        delete_value: MagicMock,
    ) -> None:
        key = MagicMock()
        open_key.return_value.__enter__.return_value = key

        autostart.set_autostart_enabled(False, Path(r"C:\WorkLens\WorkLens.exe"))

        delete_value.assert_called_once_with(key, autostart.VALUE_NAME)


if __name__ == "__main__":
    unittest.main()
