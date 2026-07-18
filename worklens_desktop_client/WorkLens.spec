# -*- mode: python ; coding: utf-8 -*-

from pathlib import Path


client_root = Path(SPECPATH).resolve()
project_root = client_root.parent

analysis = Analysis(
    [str(client_root / "tray_app.py")],
    pathex=[str(project_root)],
    binaries=[],
    datas=[(str(client_root / "__init__.py"), "worklens_desktop_client")],
    hiddenimports=["pystray._win32"],
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=[],
    noarchive=False,
    optimize=0,
)

pyz = PYZ(analysis.pure)

executable = EXE(
    pyz,
    analysis.scripts,
    [],
    exclude_binaries=True,
    name="WorkLens",
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=False,
    console=False,
)

bundle = COLLECT(
    executable,
    analysis.binaries,
    analysis.datas,
    strip=False,
    upx=False,
    name="WorkLens",
)
