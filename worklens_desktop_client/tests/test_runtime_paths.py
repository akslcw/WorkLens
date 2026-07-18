import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from worklens_desktop_client.runtime_paths import default_cache_path
from worklens_desktop_client.runtime_paths import default_log_path


class RuntimePathsTests(unittest.TestCase):

    def test_cache_and_log_paths_use_local_app_data(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory, patch.dict(
            "os.environ",
            {"LOCALAPPDATA": temporary_directory},
        ):
            cache_path = default_cache_path()
            log_path = default_log_path()

        expected_root = Path(temporary_directory) / "WorkLens"
        self.assertEqual(expected_root / "cache.sqlite3", cache_path)
        self.assertEqual(expected_root / "logs" / "worklens.log", log_path)


if __name__ == "__main__":
    unittest.main()
