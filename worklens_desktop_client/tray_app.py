from __future__ import annotations

import argparse
import threading
from pathlib import Path
from tkinter import Tk, simpledialog

import pystray
from PIL import Image
from PIL import ImageDraw

from worklens_desktop_client.background_runner import BackgroundRunner
from worklens_desktop_client.sync_runtime import SyncRuntime
from worklens_desktop_client.sync_runtime import SyncRuntimeConfig
from worklens_desktop_client.tray_status import format_status_menu_text
from worklens_desktop_client.tray_status import format_tray_title


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Run the WorkLens desktop collector in the Windows system tray."
    )
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--sample-interval-seconds", type=int, default=5)
    parser.add_argument("--idle-threshold-seconds", type=int, default=300)
    parser.add_argument("--upload-interval-seconds", type=int, default=300)
    parser.add_argument(
        "--cache-db",
        default=str(Path(__file__).resolve().parent / "cache.sqlite3"),
    )
    parser.add_argument("--username", default=None)
    parser.add_argument("--password", default=None)
    return parser.parse_args()


def create_status_image(status: str) -> Image.Image:
    color = "#2e7d32" if status == "RUNNING" else "#c62828"
    image = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.ellipse((8, 8, 56, 56), fill=color)
    draw.ellipse((18, 18, 46, 46), fill="white")
    return image


def prompt_credentials(initial_username: str | None, initial_password: str | None) -> tuple[str, str]:
    if initial_username and initial_password:
        return initial_username, initial_password

    root = Tk()
    root.withdraw()
    username = initial_username or simpledialog.askstring("WorkLens", "Username:", parent=root)
    password = initial_password or simpledialog.askstring("WorkLens", "Password:", parent=root, show="*")
    root.destroy()

    if not username or not password:
        raise SystemExit("Username and password are required.")
    return username.strip(), password


def main() -> None:
    args = parse_args()
    username, password = prompt_credentials(args.username, args.password)

    status_holder = {"value": "STOPPED"}
    display_name_holder = {"value": username}
    icon_holder: dict[str, pystray.Icon] = {}

    def refresh_icon() -> None:
        icon = icon_holder.get("icon")
        if icon is not None:
            status = status_holder["value"]
            icon.icon = create_status_image(status)
            icon.title = format_tray_title(status, display_name_holder["value"])
            icon.update_menu()

    def update_login_user(login_result) -> None:
        display_name_holder["value"] = login_result.display_name
        refresh_icon()

    runtime = SyncRuntime(
        SyncRuntimeConfig(
            base_url=args.base_url,
            sample_interval_seconds=args.sample_interval_seconds,
            idle_threshold_seconds=args.idle_threshold_seconds,
            upload_interval_seconds=args.upload_interval_seconds,
            cache_db=args.cache_db,
        ),
        on_login=update_login_user,
    )

    def update_status(status: str) -> None:
        status_holder["value"] = status
        refresh_icon()

    def worker(stop_event: threading.Event) -> None:
        runtime.run(username=username, password=password, stop_event=stop_event)

    runner = BackgroundRunner(worker=worker, on_status_change=update_status)

    def status_text(_) -> str:
        return format_status_menu_text(status_holder["value"], display_name_holder["value"])

    def quit_app(icon: pystray.Icon, item) -> None:
        runner.request_stop()
        runner.join(timeout=10)
        icon.stop()

    menu = pystray.Menu(
        pystray.MenuItem(status_text, None, enabled=False),
        pystray.MenuItem("退出", quit_app),
    )
    icon = pystray.Icon(
        "worklens-desktop-client",
        create_status_image("STOPPED"),
        format_tray_title("STOPPED", display_name_holder["value"]),
        menu,
    )
    icon_holder["icon"] = icon
    runner.start()
    icon.run()


if __name__ == "__main__":
    main()
