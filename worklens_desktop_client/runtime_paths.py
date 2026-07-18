from __future__ import annotations

import os
from pathlib import Path


APP_DIRECTORY_NAME = "WorkLens"


def local_data_directory() -> Path:
    local_app_data = os.environ.get("LOCALAPPDATA")
    if local_app_data:
        root = Path(local_app_data)
    else:
        root = Path.home() / "AppData" / "Local"
    app_directory = root / APP_DIRECTORY_NAME
    app_directory.mkdir(parents=True, exist_ok=True)
    return app_directory


def default_cache_path() -> Path:
    return local_data_directory() / "cache.sqlite3"


def default_log_path() -> Path:
    log_directory = local_data_directory() / "logs"
    log_directory.mkdir(parents=True, exist_ok=True)
    return log_directory / "worklens.log"
