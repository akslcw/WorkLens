from __future__ import annotations


def format_status_menu_text(status: str, display_name: str) -> str:
    state = "运行中" if status == "RUNNING" else "已停止"
    return f"状态: {state} / 用户: {display_name}"


def format_tray_title(status: str, display_name: str) -> str:
    return f"WorkLens ({status}) - {display_name}"
