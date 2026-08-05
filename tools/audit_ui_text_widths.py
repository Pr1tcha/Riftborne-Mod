"""Estimate rendered width of Codex strings against the containers that hold them.

Minecraft's default font advances 6px for most glyphs (5px glyph + 1px spacing), including
Cyrillic. A handful of ASCII glyphs are narrower. This is an approximation, but it is accurate
enough to tell a label that comfortably fits from one that will be cut with an ellipsis.
"""
import io
import json

NARROW = {
    'i': 2, 'l': 3, 't': 4, 'f': 5, 'k': 5, '.': 2, ',': 2, ':': 2, ';': 2,
    '!': 2, '|': 2, "'": 2, '`': 3, '[': 4, ']': 4, '(': 5, ')': 5, '{': 5, '}': 5,
    ' ': 4, '"': 4, '*': 5, '<': 5, '>': 5, 'I': 4, 'j': 6, '1': 6,
}


def width(text):
    return sum(NARROW.get(ch, 6) for ch in text)


# container -> (available px, [lang keys drawn in it])
CONTAINERS = {
    "System menu item (102px box, icon+padding)": (71, [
        "screen.riftborne.codex.infobase",
        "screen.riftborne.codex.explorer_icon",
        "screen.riftborne.codex.sync",
        "screen.riftborne.codex.decryptor",
        "screen.riftborne.codex.synapsis",
        "screen.riftborne.codex.physical",
    ]),
    "Synapsis card (174px)": (154, [
        "screen.riftborne.codex.synapsis_subject",
        "screen.riftborne.codex.synapsis_source",
        "screen.riftborne.codex.synapsis_path",
        "screen.riftborne.codex.technique",
        "rna.riftborne.formation_path.training",
        "rna.riftborne.formation_path.stress",
        "rna.riftborne.formation_path.artificial_born",
        "rna.riftborne.formation_path.technological",
        "rna.riftborne.formation_path.interspatial",
    ]),
    "Progression panel (in synapsis card)": (154, [
        "screen.riftborne.codex.primitive_profile",
        "screen.riftborne.codex.primitive_profile.empty",
        "screen.riftborne.codex.axis_practice",
        "screen.riftborne.codex.axis_practice.empty",
        "screen.riftborne.codex.facet",
        "screen.riftborne.codex.facet.empty",
    ]),
    "Physical card (266px)": (248, [
        "physical.riftborne.strength",
        "physical.riftborne.endurance",
        "physical.riftborne.resilience",
        "physical.riftborne.recovery",
        "screen.riftborne.codex.physical_step_full",
        "screen.riftborne.codex.physical_step_high",
        "screen.riftborne.codex.physical_step_low",
        "screen.riftborne.codex.physical_step_none",
    ]),
    "Physical summary tile (266px)": (246, [
        "screen.riftborne.codex.physical_overall",
        "screen.riftborne.codex.physical_overload",
    ]),
    "Pocket Codex info column": (160, [
        "screen.riftborne.pocket_codex.last_contact",
        "screen.riftborne.pocket_codex.threat",
        "screen.riftborne.pocket_codex.observation",
    ]),
    "Pocket Codex pulse column": (147, [
        "screen.riftborne.pocket_codex.signatures",
        "screen.riftborne.pocket_codex.nearest",
        "screen.riftborne.pocket_codex.distance",
    ]),
    "Power HUD armed row (160px bar, shares with cost)": (110, [
        "rna.riftborne.primitive.p1_reading",
        "rna.riftborne.primitive.p2_anchor",
        "rna.riftborne.primitive.p3_shift",
        "rna.riftborne.primitive.p4_shape",
        "rna.riftborne.primitive.p5_stabilize",
        "rna.riftborne.primitive.v1_tempo_read",
        "rna.riftborne.primitive.v2_phase_entry",
        "rna.riftborne.primitive.v3_tempo_shift",
        "rna.riftborne.primitive.v4_phase_hold",
    ]),
    "Power selector spoke (104px box)": (94, [
        "rna.riftborne.primitive.p1_reading",
        "rna.riftborne.primitive.p2_anchor",
        "rna.riftborne.primitive.p3_shift",
        "rna.riftborne.primitive.p4_shape",
        "rna.riftborne.primitive.p5_stabilize",
        "rna.riftborne.primitive.v1_tempo_read",
        "rna.riftborne.primitive.v2_phase_entry",
        "rna.riftborne.primitive.v3_tempo_shift",
        "rna.riftborne.primitive.v4_phase_hold",
    ]),
}


def load(name):
    with io.open("src/main/resources/assets/riftborne/lang/%s.json" % name, encoding="utf-8") as fh:
        return json.load(fh)


def sample(text):
    """Replace format placeholders with plausible values so widths are realistic."""
    return text.replace("%s", "100").replace("%1$s", "100").replace("%2$s", "100")


ru = load("ru_ru")
en = load("en_us")

problems = 0
for container, (avail, keys) in CONTAINERS.items():
    rows = []
    for key in keys:
        for lang, table in (("ru", ru), ("en", en)):
            text = table.get(key)
            if text is None:
                continue
            w = width(sample(text))
            if w > avail:
                rows.append((lang, key, sample(text), w))
    if rows:
        problems += len(rows)
        print("\n%s  — доступно %dpx" % (container, avail))
        for lang, key, text, w in sorted(rows, key=lambda r: -r[3]):
            print("   %s  %4dpx (+%3d)  %-46s %s" % (lang, w, w - avail, key.split(".")[-1], text))

print("\nстрок, которые будут обрезаны: %d" % problems)
