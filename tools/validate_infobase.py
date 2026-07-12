"""Validate Riftborne Infobase article JSON files."""

from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ENTRY_DIR = ROOT / "src/main/resources/data/riftborne/codex_entries"
SCAN_TARGET_DIR = ROOT / "src/main/resources/data/riftborne/codex_scan_targets"
CATEGORIES = {
    "RIFTS", "MOBS", "DIMENSIONS", "RNA", "ASPECTS", "TECHNIQUES",
    "ITEMS", "DEVICES", "SIGNALS", "FIELD_ARCHIVE", "ARCHIVE", "SYSTEM",
}
STATES = {"LOCKED", "PARTIAL", "UNLOCKED", "DAMAGED", "ENCRYPTED", "NEEDS_DECRYPTION"}


def main() -> None:
    errors: list[str] = []
    entries: dict[str, tuple[Path, dict]] = {}

    for path in sorted(ENTRY_DIR.glob("*.json")):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as error:
            errors.append(f"{path.name}: invalid JSON: {error}")
            continue

        entry_id = data.get("id", f"riftborne:{path.stem}")
        if entry_id in entries:
            errors.append(f"{path.name}: duplicate id {entry_id!r}")
        entries[entry_id] = (path, data)
        if not isinstance(data.get("title"), str) or not data["title"].strip():
            errors.append(f"{path.name}: title must be a non-empty string")
        if data.get("category") not in CATEGORIES:
            errors.append(f"{path.name}: unsupported category {data.get('category')!r}")
        if data.get("state", "LOCKED") not in STATES:
            errors.append(f"{path.name}: unsupported state {data.get('state')!r}")
        threat = data.get("threat", 0)
        if not isinstance(threat, int) or not 0 <= threat <= 5:
            errors.append(f"{path.name}: threat must be an integer from 0 to 5")
        if not isinstance(data.get("order", 1000), int):
            errors.append(f"{path.name}: order must be an integer")
        for index, section in enumerate(data.get("sections", [])):
            if not isinstance(section, dict) or not isinstance(section.get("body"), str):
                errors.append(f"{path.name}: sections[{index}] must contain a text body")

    known_ids = set(entries)
    for path, data in entries.values():
        for related in data.get("related", []):
            if related not in known_ids:
                errors.append(f"{path.name}: related entry {related!r} does not exist")

    scan_targets = 0
    scan_ids: set[str] = set()
    for path in sorted(SCAN_TARGET_DIR.glob("*.json")):
        scan_targets += 1
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as error:
            errors.append(f"{path.name}: invalid scan target JSON: {error}")
            continue
        scan_id = data.get("id", f"riftborne:{path.stem}")
        if scan_id in scan_ids:
            errors.append(f"{path.name}: duplicate scan target id {scan_id!r}")
        scan_ids.add(scan_id)
        if data.get("entry") not in known_ids:
            errors.append(f"{path.name}: scan entry {data.get('entry')!r} does not exist")
        if data.get("type") not in {"BLOCK", "ENTITY"}:
            errors.append(f"{path.name}: type must be BLOCK or ENTITY")
        targets = data.get("targets")
        if not isinstance(targets, list) or not targets or not all(isinstance(value, str) for value in targets):
            errors.append(f"{path.name}: targets must be a non-empty string list")

    if errors:
        raise SystemExit("\n".join(errors))
    print(f"Infobase validation: OK ({len(entries)} articles, {scan_targets} scan targets)")


if __name__ == "__main__":
    main()
