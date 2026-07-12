"""Concept geometry generator for the RNA Training Anchor (Synchronization Node).

Draft only: clean box geometry laid out for a folded -> 4-block telescoping deploy.
The player is expected to open this in Blockbench and refine shapes, bevels, and UVs.
Coordinate frame matches the previous model: X/Z centred on 0 (block footprint
-8..8), Y up from 0 at the block floor. 16 units = 1 block, so a deployed height of
~62 units reads as ~4 blocks tall.
"""

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GEO = ROOT / "src/main/resources/assets/riftborne/geo/rna_training_anchor.geo.json"

TEX_W = 64
TEX_H = 64


def cube(origin, size):
    # Flat per-face UV pointing at the texture origin, matching the previous
    # placeholder model. Real UVs are meant to be authored in Blockbench.
    faces = {}
    for face in ("north", "east", "south", "west", "up", "down"):
        faces[face] = {"uv": [0, 0], "uv_size": [0, 0]}
    return {"origin": origin, "size": size, "uv": faces}


def bone(name, pivot, parent=None, cubes=None):
    b = {"name": name, "pivot": pivot}
    if parent:
        b["parent"] = parent
    if cubes:
        b["cubes"] = cubes
    return b


bones = [
    bone("root", [0, 0, 0]),

    # Heavy anchored base: a wide foot slab and a pedestal drum.
    bone("base", [0, 0, 0], "root", [
        cube([-7, 0, -7], [14, 2, 14]),
        cube([-5, 2, -5], [10, 4, 10]),
    ]),

    # Four stabiliser fins around the drum (existing anims spin/rotate these).
    bone("stabilizers", [0, 4, 0], "base", [
        cube([-1.5, 3, -7], [3, 3, 1.5]),
        cube([-1.5, 3, 5.5], [3, 3, 1.5]),
        cube([-7, 3, -1.5], [1.5, 3, 3]),
        cube([5.5, 3, -1.5], [1.5, 3, 3]),
    ]),

    # Always-visible central spine rising out of the pedestal (y6..16).
    bone("spine", [0, 6, 0], "base", [
        cube([-1.5, 6, -1.5], [3, 10, 3]),
    ]),

    # Three telescoping segments. Authored at full deployment; the folded/deploy
    # animations translate each one down into its parent to collapse the tower.
    bone("seg1", [0, 16, 0], "spine", [
        cube([-2.5, 16, -2.5], [5, 16, 5]),
    ]),
    bone("seg2", [0, 32, 0], "seg1", [
        cube([-2, 32, -2], [4, 16, 4]),
    ]),
    bone("seg3", [0, 48, 0], "seg2", [
        cube([-1.5, 48, -1.5], [3, 14, 3]),
    ]),

    # Emitter head at the top of the tower.
    bone("ring", [0, 56, 0], "seg3", [
        cube([-5, 55, -5], [10, 2, 10]),
    ]),
    bone("core", [0, 56, 0], "seg3", [
        cube([-2, 52, -2], [4, 8, 4]),
    ]),
    bone("glow", [0, 56, 0], "core", [
        cube([-2.6, 51, -2.6], [5.2, 10, 5.2]),
    ]),
]

model = {
    "format_version": "1.12.0",
    "minecraft:geometry": [
        {
            "description": {
                "identifier": "geometry.riftborne.rna_training_anchor",
                "texture_width": TEX_W,
                "texture_height": TEX_H,
                "visible_bounds_width": 4,
                "visible_bounds_height": 5,
                "visible_bounds_offset": [0, 2, 0],
            },
            "bones": bones,
        }
    ],
}

GEO.write_text(json.dumps(model, indent=2), encoding="utf-8")
print("wrote", GEO.relative_to(ROOT))
