"""Find translation keys the code asks for that no language file provides.

A missing key does not crash anything: Minecraft simply draws the raw key, and if several keys
share a long prefix they all look identical on screen. That is exactly how the primitive selector
ended up showing nine identical labels. This check makes the mismatch visible without launching.

Two sources of keys are handled:
  * literals, e.g. Component.translatable("hud.riftborne.power.title")
  * enum builders, e.g. `return "rna.riftborne.primitive." + id();` inside an enum — the constants
    declared in that same file are expanded against the prefix.
"""

import glob
import io
import json
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAVA = os.path.join(ROOT, "src/main/java")
LANG = os.path.join(ROOT, "src/main/resources/assets/riftborne/lang")

LITERAL = re.compile(r'translatable\(\s*"([a-z0-9_.]+riftborne[a-z0-9_.]*)"')
BUILDER = re.compile(r'return\s+"([a-z0-9_.]+\.)"\s*\+\s*id\(\)')
ENUM_CONST = re.compile(r'^\s{4}([A-Z][A-Z0-9_]*)\s*[(,;]', re.MULTILINE)

# Keys assembled from runtime values (result names, block ids, paths) cannot be resolved
# statically; they are verified by the screens that use them instead.
DYNAMIC_PREFIXES = (
    "hud.riftborne.power.result.",
    "message.riftborne.training.start.",
    "rna.riftborne.formation_path.",
    "rna.riftborne.meta_wear_stage.",
    "screen.riftborne.codex.stage.",
    "screen.riftborne.codex.condition.",
    "facet.riftborne.",
    "physical.riftborne.",
)


def wanted_keys():
    keys = {}
    for path in glob.glob(os.path.join(JAVA, "**", "*.java"), recursive=True):
        source = io.open(path, encoding="utf-8").read()
        name = os.path.relpath(path, JAVA).replace("\\", "/")
        for key in LITERAL.findall(source):
            keys.setdefault(key, name)
        for prefix in BUILDER.findall(source):
            # No constants in this file means the enum is declared elsewhere; the prefix alone
            # is not a key, so expanding it would only produce noise.
            for constant in ENUM_CONST.findall(source):
                keys.setdefault(prefix + constant.lower(), name)
    return keys


def main():
    english = json.load(io.open(os.path.join(LANG, "en_us.json"), encoding="utf-8"))
    russian = json.load(io.open(os.path.join(LANG, "ru_ru.json"), encoding="utf-8"))

    missing_en, missing_ru = [], []
    for key, where in sorted(wanted_keys().items()):
        if key.endswith(".") or any(key.startswith(p) for p in DYNAMIC_PREFIXES):
            continue
        if key not in english:
            missing_en.append((key, where))
        elif key not in russian:
            missing_ru.append((key, where))

    for title, rows in (("missing in en_us", missing_en), ("missing in ru_ru", missing_ru)):
        if rows:
            print("%s (%d):" % (title, len(rows)))
            for key, where in rows:
                print("   %-52s  %s" % (key, where))

    untranslated = sorted(k for k in english if k not in russian)
    if untranslated:
        print("present in en_us but not ru_ru (%d):" % len(untranslated))
        for key in untranslated:
            print("   " + key)

    if not (missing_en or missing_ru or untranslated):
        print("OK: every static translation key resolves in both languages")
        return 0
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
