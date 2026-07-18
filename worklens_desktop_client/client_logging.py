from __future__ import annotations

import logging
from logging.handlers import RotatingFileHandler
from pathlib import Path

from worklens_desktop_client.runtime_paths import default_log_path


LOGGER_NAME = "worklens.desktop"


def create_client_logger(log_path: Path | None = None) -> logging.Logger:
    resolved_path = log_path or default_log_path()
    resolved_path.parent.mkdir(parents=True, exist_ok=True)

    logger = logging.getLogger(LOGGER_NAME)
    logger.setLevel(logging.INFO)
    logger.propagate = False

    for existing_handler in list(logger.handlers):
        existing_handler.close()
        logger.removeHandler(existing_handler)

    handler = RotatingFileHandler(
        resolved_path,
        maxBytes=1_000_000,
        backupCount=3,
        encoding="utf-8",
    )
    handler.setFormatter(
        logging.Formatter("%(asctime)s %(levelname)s %(message)s", "%Y-%m-%d %H:%M:%S")
    )
    logger.addHandler(handler)
    return logger
