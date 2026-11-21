#!/usr/bin/env python3
"""Static QA gate for an *optional* Minecraft 1.20.1 external preview pack.

No Forge/Minecraft/multiplayer claim is implied by a successful check.
Requires only Python 3.9+ standard library.
"""
import argparse
import json
import re
import struct
from pathlib import Path
from zipfile import BadZipFile, ZipFile

NS_RE = re.compile(r"^[a-z0-9_.-]+$")
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
MAX_ENTRIES = 4096
MAX_UNCOMPRESSED = 512 * 1024 * 1024
MAX_FILE = 128 * 1024 * 1024


def check_pack(path: Path) -> list[str]:
    errors: list[str] = []
    try:
        with ZipFile(path) as archive:
            entries = archive.infolist()
            files = [info for info in entries if not info.is_dir()]
            if len(entries) > MAX_ENTRIES:
                errors.append("too many ZIP entries")
            if sum(info.file_size for info in files) > MAX_UNCOMPRESSED:
                errors.append("uncompressed pack exceeds 512 MiB")
            seen: set[str] = set()
            names: set[str] = set()
            for info in entries:
                name = info.filename
                parts = name.rstrip("/").split("/")
                if (not name or name.startswith("/") or "\\" in name or
                        any(part in ("", ".", "..") for part in parts) or
                        ":" in name or "\x00" in name):
                    errors.append(f"unsafe ZIP path: {name!r}")
                    continue
                folded = name.casefold().rstrip("/")
                if folded in seen:
                    errors.append(f"duplicate ZIP path: {name}")
                seen.add(folded)
                if info.is_dir():
                    continue
                names.add(name)
                if info.flag_bits & 1:
                    errors.append(f"encrypted ZIP entry: {name}")
                if info.file_size > MAX_FILE:
                    errors.append(f"file exceeds 128 MiB: {name}")
                if info.file_size > 16 * 1024 * 1024 and info.file_size > 250 * max(info.compress_size, 1):
                    errors.append(f"extreme compression ratio: {name}")
                if name not in ("pack.mcmeta", "pack.png", "LICENSE.txt", "README.txt"):
                    if not name.startswith("assets/"):
                        errors.append(f"entry outside resource-pack assets: {name}")
                    elif len(parts) < 4 or not NS_RE.fullmatch(parts[1]):
                        errors.append(f"invalid assets namespace/path: {name}")
                    elif parts[1] in ("minecraft", "forge"):
                        errors.append(f"preview overrides reserved namespace: {name}")
            if "pack.mcmeta" not in names:
                errors.append("missing root pack.mcmeta")
            else:
                meta_info = archive.getinfo("pack.mcmeta")
                if meta_info.file_size > 65536:
                    errors.append("pack.mcmeta exceeds 64 KiB")
                else:
                    try:
                        meta = json.loads(archive.read("pack.mcmeta").decode("utf-8-sig"))
                        data = meta["pack"]
                        if type(data.get("pack_format")) is not int or data["pack_format"] != 15:
                            errors.append("pack_format must be integer 15 for Minecraft 1.20.1")
                        if not isinstance(data.get("description"), (str, dict)) or not data["description"]:
                            errors.append("pack description is missing")
                    except (ValueError, KeyError, TypeError, UnicodeError):
                        errors.append("invalid pack.mcmeta JSON/structure")
            for name in sorted(n for n in names if n.lower().endswith(".png")):
                with archive.open(name) as stream:
                    header = stream.read(24)
                if len(header) < 24 or not header.startswith(PNG_SIGNATURE) or header[12:16] != b"IHDR":
                    errors.append(f"invalid PNG header: {name}")
                else:
                    width, height = struct.unpack(">II", header[16:24])
                    if width == 0 or height == 0 or width > 16384 or height > 16384:
                        errors.append(f"invalid PNG dimensions: {name}")
            # Verifies decompression/ZIP CRC; not a complete PNG CRC/decoder check.
            if not errors:
                corrupted = archive.testzip()
                if corrupted:
                    errors.append(f"ZIP CRC failed: {corrupted}")
    except (BadZipFile, OSError, RuntimeError, ValueError, KeyError) as exc:
        errors.append(f"cannot inspect ZIP: {type(exc).__name__}: {exc}")
    return errors


def check_mod_tree(root: Path) -> list[str]:
    resources = root / "src" / "main" / "resources"
    if not resources.is_dir():
        return ["mod resources tree missing; cannot verify no embedded HD ZIP"]
    return [f"archive bundled in mod resources: {p.relative_to(root)}"
            for p in sorted(resources.rglob("*")) if p.is_file() and p.suffix.lower() in (".zip", ".jar")]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pack", type=Path, help="external preview ZIP")
    parser.add_argument("--mod-root", type=Path, help="optional Forge project root")
    args = parser.parse_args()
    if not args.pack and not args.mod_root:
        parser.error("provide --pack and/or --mod-root")
    problems = []
    if args.pack:
        problems.extend(check_pack(args.pack))
    if args.mod_root:
        problems.extend(check_mod_tree(args.mod_root))
    if problems:
        for issue in problems:
            print(f"FAIL: {issue}")
        return 2
    print("PASS: static preview-pack gate (not Forge/Minecraft runtime validation)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
