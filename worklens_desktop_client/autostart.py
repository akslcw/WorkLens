from __future__ import annotations

import subprocess
import sys
import winreg
from pathlib import Path


RUN_KEY = r"Software\Microsoft\Windows\CurrentVersion\Run"
VALUE_NAME = "WorkLens"


class AutostartError(RuntimeError):
    """An autostart failure that is safe to display to the user."""


def packaged_executable_path() -> Path:
    if not getattr(sys, "frozen", False):
        raise AutostartError("开机自启动只能在打包后的 WorkLens 客户端中设置。")
    return Path(sys.executable).resolve()


def build_autostart_command(executable_path: Path) -> str:
    return subprocess.list2cmdline([str(executable_path.resolve())])


def is_autostart_enabled(executable_path: Path | None = None) -> bool:
    expected_command = build_autostart_command(executable_path or packaged_executable_path())
    try:
        with winreg.OpenKey(winreg.HKEY_CURRENT_USER, RUN_KEY, 0, winreg.KEY_READ) as key:
            configured_command, _ = winreg.QueryValueEx(key, VALUE_NAME)
    except FileNotFoundError:
        return False
    return configured_command == expected_command


def set_autostart_enabled(enabled: bool, executable_path: Path | None = None) -> None:
    resolved_executable = executable_path or packaged_executable_path()
    if enabled:
        command = build_autostart_command(resolved_executable)
        with winreg.CreateKeyEx(
            winreg.HKEY_CURRENT_USER,
            RUN_KEY,
            0,
            winreg.KEY_SET_VALUE,
        ) as key:
            winreg.SetValueEx(key, VALUE_NAME, 0, winreg.REG_SZ, command)
        return

    try:
        with winreg.OpenKey(
            winreg.HKEY_CURRENT_USER,
            RUN_KEY,
            0,
            winreg.KEY_SET_VALUE,
        ) as key:
            winreg.DeleteValue(key, VALUE_NAME)
    except FileNotFoundError:
        return
