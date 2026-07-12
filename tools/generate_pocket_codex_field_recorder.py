"""Generate the redesigned Codex field recorder without overwriting the old artist bbmodel."""

from __future__ import annotations

import json

from generate_pocket_codex_assets import (
    ASSETS,
    CASE,
    CYAN,
    DARK,
    METAL,
    RUBBER,
    SCREEN,
    Mesh,
    cube,
    make_texture,
    write_bbmodel,
    write_geo,
)


DISPLAY = {
    "thirdperson_righthand": {
        "rotation": [0, -8, -12],
        "translation": [0.5, 3.2, 1.0],
        "scale": [0.44, 0.44, 0.44],
    },
    "thirdperson_lefthand": {
        "rotation": [0, 8, 12],
        "translation": [-0.5, 3.2, 1.0],
        "scale": [0.44, 0.44, 0.44],
    },
    "firstperson_righthand": {
        "rotation": [-13, 137, -90],
        "translation": [2.25, 2, -3.5],
        "scale": [0.69, 0.68, 0.48],
    },
    "firstperson_lefthand": {
        "rotation": [-180, 36, -90],
        "translation": [2.25, 2, -3.5],
        "scale": [0.69, 0.68, 0.48],
    },
    "ground": {
        "rotation": [90, 0, 0],
        "translation": [0, 2.0, 0],
        "scale": [0.46, 0.46, 0.46],
    },
    "fixed": {
        "translation": [0, 0, -1.0],
        "scale": [0.88, 0.88, 0.55],
    },
    "gui": {"rotation": [180, 0, 180]},
    "on_shelf": {"rotation": [-180, 0, 180]},
}


def build_geometry():
    shell = Mesh()
    shell.chamfered_box(-4.7, 4.7, 0.0, 1.25, -7.0, 6.65, 0.75, CASE)
    shell.chamfered_box(-5.15, -3.95, 0.18, 1.55, -5.8, 4.4, 0.28, RUBBER)
    shell.chamfered_box(3.75, 5.35, 0.15, 1.72, -4.9, 3.85, 0.34, RUBBER)
    shell.chamfered_box(-3.9, 3.55, 0.95, 1.48, -3.95, 3.65, 0.38, DARK)
    shell.chamfered_box(-4.55, -1.2, 0.92, 1.72, 4.55, 7.35, 0.42, METAL)
    shell.chamfered_box(1.75, 4.1, 0.88, 1.48, 4.9, 6.2, 0.28, CASE)
    shell.chamfered_box(-3.75, 3.7, 0.22, 0.65, -6.55, -5.35, 0.3, METAL)

    screen = Mesh()
    screen.quad(
        [
            (-3.62, 1.53, -3.58),
            (-3.62, 1.53, 3.28),
            (3.27, 1.53, 3.28),
            (3.27, 1.53, -3.58),
        ],
        SCREEN,
    )

    scan_bar = Mesh()
    scan_bar.chamfered_box(-3.35, 3.0, 1.55, 1.68, -0.42, -0.18, 0.06, CYAN)

    sensor = Mesh()
    sensor.chamfered_box(-3.95, -1.7, 1.52, 2.18, 5.05, 6.9, 0.3, DARK)
    sensor.chamfered_box(-3.45, -2.2, 2.08, 2.38, 5.48, 6.52, 0.2, CYAN)

    bones = [
        {"name": "root", "pivot": [0, 0, 0]},
        {"name": "device", "pivot": [0, 0, 0], "rotation": [-90, 0, 0], "parent": "root"},
        {"name": "shell", "pivot": [0, 0, 0], "parent": "device", "poly_mesh": shell.payload()},
        {"name": "sensor", "pivot": [-2.8, 1.7, 5.9], "parent": "device", "poly_mesh": sensor.payload()},
        {
            "name": "antenna",
            "pivot": [-4.1, 1.2, 6.2],
            "parent": "device",
            "cubes": [
                cube([-4.38, 0.7, 6.1], [0.55, 0.72, 2.65], METAL),
                cube([-4.5, 0.62, 8.55], [0.8, 0.88, 0.55], RUBBER),
            ],
        },
        {
            "name": "controls",
            "pivot": [0.4, 1.5, -5.3],
            "parent": "device",
            "cubes": [
                cube([-2.9, 1.24, -5.95], [1.35, 0.52, 0.82], DARK),
                cube([-1.15, 1.24, -5.95], [1.35, 0.52, 0.82], DARK),
                cube([1.15, 1.18, -6.05], [2.15, 0.62, 1.0], METAL),
                cube([4.18, 0.75, -2.4], [0.68, 1.32, 2.25], DARK),
            ],
        },
        {
            "name": "indicator_glow",
            "pivot": [2.3, 1.5, 5.55],
            "parent": "device",
            "cubes": [
                cube([2.0, 1.42, 5.45], [1.65, 0.28, 0.38], CYAN),
                cube([4.36, 1.58, 0.4], [0.3, 0.24, 2.4], CYAN),
            ],
        },
        {"name": "screen_glow", "pivot": [-0.17, 1.53, -0.15], "parent": "device", "poly_mesh": screen.payload()},
        {"name": "scan_bar_glow", "pivot": [-0.17, 1.61, -0.3], "parent": "device", "poly_mesh": scan_bar.payload()},
    ]
    return bones


def write_animations():
    payload = {
        "format_version": "1.8.0",
        "animations": {
            "animation.pocket_codex.idle": {
                "loop": True,
                "animation_length": 3.0,
                "bones": {
                    "indicator_glow": {
                        "scale": {"0.0": [1, 1, 1], "1.5": [1.02, 1.02, 1.02], "3.0": [1, 1, 1]}
                    }
                },
            },
            "animation.pocket_codex.scan": {
                "loop": False,
                "animation_length": 0.72,
                "bones": {
                    "root": {
                        "rotation": {"0.0": [0, 0, 0], "0.16": [-4, 0, 0], "0.72": [0, 0, 0]}
                    },
                    "scan_bar_glow": {
                        "position": {"0.0": [0, 0, -3.0], "0.58": [0, 0, 3.0], "0.72": [0, 0, -3.0]}
                    },
                    "sensor": {
                        "scale": {"0.0": [1, 1, 1], "0.16": [1.05, 1.05, 1.05], "0.72": [1, 1, 1]}
                    },
                },
            },
            "animation.pocket_codex.pulse": {
                "loop": False,
                "animation_length": 0.9,
                "bones": {
                    "antenna": {
                        "rotation": {"0.0": [0, 0, 0], "0.22": [0, 0, -18], "0.7": [0, 0, 8], "0.9": [0, 0, 0]}
                    },
                    "indicator_glow": {
                        "scale": {"0.0": [1, 1, 1], "0.28": [1.18, 1.18, 1.18], "0.9": [1, 1, 1]}
                    },
                },
            },
            "animation.pocket_codex.warning": {
                "loop": False,
                "animation_length": 0.42,
                "bones": {
                    "root": {
                        "rotation": {"0.0": [0, 0, 0], "0.1": [0, 0, -3], "0.2": [0, 0, 3], "0.3": [0, 0, -2], "0.42": [0, 0, 0]}
                    }
                },
            },
        },
    }
    path = ASSETS / "animations/pocket_codex.animation.json"
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def write_item_model():
    path = ASSETS / "models/item/pocket_codex.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps({"parent": "minecraft:builtin/entity", "display": DISPLAY}, indent=2) + "\n", encoding="utf-8")


def main():
    bones = build_geometry()
    texture_path, _, image = make_texture("pocket_codex")
    write_geo("pocket_codex", bones, 1.55, 1.85, [0, 0.1, 0])
    write_animations()
    write_item_model()
    write_bbmodel("Pocket_Codex_Field_Recorder", "Item", bones, texture_path, image, display=DISPLAY)
    print("Generated Pocket Codex field recorder assets without touching Pocket_Codex.bbmodel")


if __name__ == "__main__":
    main()
