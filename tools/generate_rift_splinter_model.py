from __future__ import annotations

import json
import random
import uuid
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
MODEL_NAME = "rift_splinter"
BBMODEL = ROOT / "bbmodels" / "Rift_Splinter.bbmodel"
GEO = ROOT / "src/main/resources/assets/riftborne/geo/entity/rift_splinter.geo.json"
ANIM = ROOT / "src/main/resources/assets/riftborne/animations/entity/rift_splinter.animation.json"
TEXTURE = ROOT / "src/main/resources/assets/riftborne/textures/entity/rift_splinter.png"


def uid() -> str:
    return str(uuid.uuid4())


def cube(name, origin, size, uv, *, rotation=None, pivot=None, glow=False, inflate=0):
    return {
        "name": name,
        "origin": list(origin),
        "size": list(size),
        "uv": list(uv),
        "rotation": list(rotation or (0, 0, 0)),
        "pivot": list(pivot or (
            origin[0] + size[0] / 2,
            origin[1] + size[1] / 2,
            origin[2] + size[2] / 2,
        )),
        "glow": glow,
        "inflate": inflate,
    }


BONES = [
    {
        "name": "root",
        "pivot": (0, 0, 0),
        "cubes": [],
    },
    {
        "name": "body",
        "parent": "root",
        "pivot": (0, 13.2, 0),
        "cubes": [
            cube("upper_body", (-2.9, 11.1, -2.0), (5.8, 6.7, 4.0), (0, 20), rotation=(-2, 0, 0), pivot=(0, 13.2, 0)),
            cube("waist", (-2.25, 8.4, -1.55), (4.5, 3.3, 3.1), (0, 38), rotation=(1, 0, 0), pivot=(0, 10.2, 0)),
            cube("left_shoulder_chip", (-3.45, 15.6, -1.55), (0.8, 2.0, 3.1), (28, 20), rotation=(0, 0, -8)),
            cube("right_shoulder_chip", (2.65, 15.6, -1.55), (0.8, 2.0, 3.1), (28, 20), rotation=(0, 0, 8)),
            cube("chest_crack", (-0.38, 12.2, -2.22), (0.76, 4.8, 0.28), (48, 0), glow=True),
            cube("chest_shard", (0.15, 10.7, -2.02), (0.68, 1.8, 0.26), (52, 0), rotation=(0, 0, -20), glow=True),
            cube("back_crack", (-0.42, 12.0, 1.97), (0.84, 5.1, 0.28), (48, 0), glow=True),
            cube("back_diamond_top", (-0.35, 16.8, 1.98), (0.7, 1.3, 0.27), (52, 0), rotation=(0, 0, 45), glow=True),
            cube("back_diamond_bottom", (-0.48, 10.9, 1.98), (0.96, 1.7, 0.27), (52, 0), rotation=(0, 0, 45), glow=True),
        ],
    },
    {
        "name": "head",
        "parent": "body",
        "pivot": (0, 17.7, 0),
        "cubes": [
            cube("head_core", (-4.0, 17.6, -4.0), (8.0, 8.0, 8.0), (0, 0)),
            cube("head_lower_chip", (-3.35, 16.95, -2.8), (6.7, 0.8, 5.6), (20, 0), rotation=(0, 0, -1)),
            cube("rift_eye_core", (-1.0, 20.35, -4.24), (2.0, 2.0, 0.3), (48, 8), rotation=(0, 0, 45), glow=True),
            cube("rift_eye_inner", (-0.48, 20.87, -4.29), (0.96, 0.96, 0.24), (56, 8), rotation=(0, 0, 45), glow=True),
            cube("rift_eye_top", (-0.28, 22.1, -4.25), (0.56, 0.85, 0.26), (52, 8), glow=True),
            cube("rift_eye_bottom", (-0.28, 19.72, -4.25), (0.56, 0.85, 0.26), (52, 8), glow=True),
            cube("rift_eye_left", (-1.63, 21.07, -4.25), (0.85, 0.56, 0.26), (52, 8), glow=True),
            cube("rift_eye_right", (0.78, 21.07, -4.25), (0.85, 0.56, 0.26), (52, 8), glow=True),
            cube("rear_rift", (-0.34, 19.2, 3.98), (0.68, 4.9, 0.28), (48, 0), glow=True),
            cube("rear_diamond", (-0.72, 20.7, 4.0), (1.44, 1.44, 0.27), (56, 8), rotation=(0, 0, 45), glow=True),
        ],
    },
    {
        "name": "left_arm",
        "parent": "body",
        "pivot": (-2.9, 16.2, 0),
        "cubes": [
            cube("left_upper_arm", (-4.8, 10.5, -1.05), (1.9, 6.1, 2.1), (32, 0), rotation=(0, 0, -12), pivot=(-2.9, 16.2, 0)),
            cube("left_forearm", (-5.8, 5.3, -0.95), (1.7, 5.8, 1.9), (32, 18), rotation=(0, 0, -5), pivot=(-4.55, 10.4, 0)),
            cube("left_arm_crack", (-5.92, 6.55, -1.12), (0.48, 3.5, 0.26), (48, 0), rotation=(0, 0, -5), glow=True),
            cube("left_palm", (-6.0, 3.85, -1.08), (1.9, 1.75, 2.16), (40, 32), rotation=(0, 0, -3)),
            cube("left_claw_a", (-6.35, 2.15, -1.15), (0.48, 1.95, 0.4), (32, 32), rotation=(0, 0, -10)),
            cube("left_claw_b", (-6.3, 1.95, -0.22), (0.48, 2.15, 0.44), (32, 32), rotation=(0, 0, -2)),
            cube("left_claw_c", (-6.2, 2.2, 0.75), (0.48, 1.9, 0.4), (32, 32), rotation=(0, 0, 8)),
        ],
    },
    {
        "name": "right_arm",
        "parent": "body",
        "pivot": (2.9, 16.2, 0),
        "cubes": [
            cube("right_upper_arm", (2.9, 10.5, -1.05), (1.9, 6.1, 2.1), (32, 0), rotation=(0, 0, 12), pivot=(2.9, 16.2, 0)),
            cube("right_forearm", (4.1, 5.3, -0.95), (1.7, 5.8, 1.9), (32, 18), rotation=(0, 0, 5), pivot=(4.55, 10.4, 0)),
            cube("right_arm_crack", (5.44, 6.55, -1.12), (0.48, 3.5, 0.26), (48, 0), rotation=(0, 0, 5), glow=True),
            cube("right_palm", (4.1, 3.85, -1.08), (1.9, 1.75, 2.16), (40, 32), rotation=(0, 0, 3)),
            cube("right_claw_a", (5.87, 2.15, -1.15), (0.48, 1.95, 0.4), (32, 32), rotation=(0, 0, 10)),
            cube("right_claw_b", (5.82, 1.95, -0.22), (0.48, 2.15, 0.44), (32, 32), rotation=(0, 0, 2)),
            cube("right_claw_c", (5.72, 2.2, 0.75), (0.48, 1.9, 0.4), (32, 32), rotation=(0, 0, -8)),
        ],
    },
    {
        "name": "left_leg",
        "parent": "root",
        "pivot": (-1.35, 8.8, 0),
        "cubes": [
            cube("left_thigh", (-2.65, 5.4, -1.2), (2.2, 3.8, 2.4), (16, 38), rotation=(0, 0, -3), pivot=(-1.35, 8.8, 0)),
            cube("left_shin", (-2.4, 1.6, -1.05), (1.8, 4.2, 2.1), (32, 42), rotation=(0, 0, 4), pivot=(-1.5, 5.5, 0)),
            cube("left_leg_glow", (-2.2, 2.0, -1.25), (0.8, 2.5, 0.3), (48, 0), rotation=(0, 0, 5), glow=True),
            cube("left_foot", (-2.85, 0.35, -1.7), (2.6, 1.55, 3.0), (16, 52), rotation=(0, 0, -2)),
            cube("left_foot_shard_outer", (-2.95, 0.0, -1.2), (0.55, 1.25, 0.55), (56, 0), rotation=(0, 0, -12), glow=True),
            cube("left_foot_shard_inner", (-0.8, 0.0, -0.25), (0.5, 1.0, 0.5), (56, 0), rotation=(0, 0, 10)),
            cube("left_foot_shard_back", (-1.7, 0.0, 1.18), (0.5, 0.85, 0.5), (56, 0), glow=True),
        ],
    },
    {
        "name": "right_leg",
        "parent": "root",
        "pivot": (1.35, 8.8, 0),
        "cubes": [
            cube("right_thigh", (0.45, 5.4, -1.2), (2.2, 3.8, 2.4), (16, 38), rotation=(0, 0, 3), pivot=(1.35, 8.8, 0)),
            cube("right_shin", (0.6, 1.6, -1.05), (1.8, 4.2, 2.1), (32, 42), rotation=(0, 0, -4), pivot=(1.5, 5.5, 0)),
            cube("right_leg_glow", (1.4, 2.0, -1.25), (0.8, 2.5, 0.3), (48, 0), rotation=(0, 0, -5), glow=True),
            cube("right_foot", (0.25, 0.35, -1.7), (2.6, 1.55, 3.0), (16, 52), rotation=(0, 0, 2)),
            cube("right_foot_shard_outer", (2.4, 0.0, -1.2), (0.55, 1.25, 0.55), (56, 0), rotation=(0, 0, 12), glow=True),
            cube("right_foot_shard_inner", (0.3, 0.0, -0.25), (0.5, 1.0, 0.5), (56, 0), rotation=(0, 0, -10)),
            cube("right_foot_shard_back", (1.2, 0.0, 1.18), (0.5, 0.85, 0.5), (56, 0), glow=True),
        ],
    },
    {
        "name": "rift_particles",
        "parent": "root",
        "pivot": (0, 13, 0),
        "cubes": [
            cube("particle_1", (-5.3, 22.8, -1.0), (0.55, 0.55, 0.55), (56, 0), rotation=(0, 0, 8), glow=True),
            cube("particle_2", (4.8, 20.4, 1.3), (0.45, 0.45, 0.45), (56, 0), rotation=(0, 0, -5), glow=True),
            cube("particle_3", (-4.7, 13.2, -2.8), (0.42, 0.42, 0.42), (56, 0), glow=True),
            cube("particle_4", (5.1, 9.0, 1.1), (0.5, 0.5, 0.5), (56, 0), rotation=(0, 0, 12), glow=True),
            cube("particle_5", (2.5, 24.9, 2.4), (0.38, 0.38, 0.38), (56, 0), glow=True),
            cube("particle_6", (-3.0, 6.9, 2.7), (0.35, 0.35, 0.35), (56, 0), glow=True),
        ],
    },
]


ANIMATIONS = {
    "animation.rift_splinter.idle": {
        "loop": True,
        "animation_length": 2.0,
        "bones": {
            "body": {"position": {"0.0": [0, 0, 0], "1.0": [0, 0.22, 0], "2.0": [0, 0, 0]}},
            "head": {"rotation": {"0.0": [0, -2, 0], "1.0": [1.5, 2, -1], "2.0": [0, -2, 0]}},
            "left_arm": {"rotation": {"0.0": [2, 0, -2], "1.0": [-2, 0, 1], "2.0": [2, 0, -2]}},
            "right_arm": {"rotation": {"0.0": [-2, 0, 2], "1.0": [2, 0, -1], "2.0": [-2, 0, 2]}},
            "rift_particles": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 9, 0], "2.0": [0, 0, 0]}},
        },
    },
    "animation.rift_splinter.walk": {
        "loop": True,
        "animation_length": 0.8,
        "bones": {
            "root": {"position": {"0.0": [0, 0, 0], "0.2": [0, 0.16, 0], "0.4": [0, 0, 0], "0.6": [0, 0.16, 0], "0.8": [0, 0, 0]}},
            "left_leg": {"rotation": {"0.0": [-24, 0, 0], "0.4": [24, 0, 0], "0.8": [-24, 0, 0]}},
            "right_leg": {"rotation": {"0.0": [24, 0, 0], "0.4": [-24, 0, 0], "0.8": [24, 0, 0]}},
            "left_arm": {"rotation": {"0.0": [20, 0, -5], "0.4": [-24, 0, 3], "0.8": [20, 0, -5]}},
            "right_arm": {"rotation": {"0.0": [-24, 0, 5], "0.4": [20, 0, -3], "0.8": [-24, 0, 5]}},
        },
    },
    "animation.rift_splinter.dash_attack": {
        "animation_length": 0.65,
        "bones": {
            "root": {"rotation": {"0.0": [0, 0, 0], "0.18": [22, 0, 0], "0.42": [42, 0, 0], "0.65": [0, 0, 0]}},
            "body": {"position": {"0.0": [0, 0, 0], "0.18": [0, -0.4, -0.8], "0.42": [0, -0.1, -2.2], "0.65": [0, 0, 0]}},
            "left_arm": {"rotation": {"0.0": [0, 0, 0], "0.22": [-48, 0, -15], "0.5": [18, 0, -5], "0.65": [0, 0, 0]}},
            "right_arm": {"rotation": {"0.0": [0, 0, 0], "0.22": [-48, 0, 15], "0.5": [18, 0, 5], "0.65": [0, 0, 0]}},
            "rift_particles": {"position": {"0.0": [0, 0, 0], "0.25": [0, 0, 2.0], "0.5": [0, 0, 0.6], "0.65": [0, 0, 0]}},
        },
    },
    "animation.rift_splinter.hurt": {
        "animation_length": 0.35,
        "bones": {
            "root": {"rotation": {"0.0": [0, 0, 0], "0.12": [-12, 0, 7], "0.35": [0, 0, 0]}},
            "head": {"rotation": {"0.0": [0, 0, 0], "0.12": [7, -8, -4], "0.35": [0, 0, 0]}},
        },
    },
    "animation.rift_splinter.death": {
        "animation_length": 1.25,
        "bones": {
            "root": {
                "rotation": {"0.0": [0, 0, 0], "0.6": [0, 0, 18], "1.25": [0, 0, 82]},
                "position": {"0.0": [0, 0, 0], "0.6": [0, -1.0, 0], "1.25": [0, -5.2, 0]},
                "scale": {"0.0": [1, 1, 1], "0.9": [1, 1, 1], "1.25": [0.18, 0.18, 0.18]},
            },
            "rift_particles": {"scale": {"0.0": [1, 1, 1], "0.55": [1.4, 1.4, 1.4], "1.25": [3.0, 3.0, 3.0]}},
        },
    },
}


def write_texture():
    random.seed(7126)
    image = Image.new("RGBA", (64, 64), (7, 5, 11, 255))
    pixels = image.load()

    for y in range(64):
        for x in range(64):
            grain = random.randint(-3, 5)
            pixels[x, y] = (
                max(5, 12 + grain),
                max(4, 8 + grain // 2),
                max(8, 18 + grain),
                255,
            )

    # Head atlas: nearly black obsidian with the same sparse purple fractures
    # visible on the front and side views in the reference sheet.
    for y in range(0, 20):
        for x in range(0, 32):
            plate = ((x // 4) * 7 + (y // 3) * 5) % 11
            pixels[x, y] = (10 + plate // 3, 7 + plate // 5, 17 + plate, 255)

    head_cracks = [
        (3, 2), (3, 3), (4, 4), (4, 5), (5, 5),
        (11, 1), (10, 2), (10, 3), (9, 4),
        (19, 4), (20, 5), (20, 6), (21, 7),
        (27, 2), (26, 3), (26, 4), (25, 5),
        (6, 13), (7, 12), (8, 12), (9, 11),
        (17, 15), (18, 14), (18, 13), (19, 12),
    ]
    for x, y in head_cracks:
        pixels[x, y] = (72, 30, 104, 255)
        if x + 1 < 32:
            pixels[x + 1, y] = (37, 17, 57, 255)

    # Body and limbs: layered black-violet strands and broken vertical veins.
    for y in range(20, 64):
        for x in range(0, 48):
            band = 8 if ((x // 2 + y // 5) % 7 == 0) else 0
            pixels[x, y] = (12 + band // 4, 8, 19 + band, 255)

    limb_veins = [
        (4, 24), (5, 25), (5, 26), (6, 27), (6, 28), (7, 29),
        (14, 32), (14, 33), (15, 34), (15, 35), (16, 36),
        (23, 40), (22, 41), (22, 42), (21, 43), (21, 44),
        (34, 22), (35, 23), (35, 24), (36, 25), (36, 26),
        (41, 43), (40, 44), (40, 45), (39, 46), (39, 47),
        (8, 54), (9, 53), (10, 53), (11, 52), (12, 52),
        (25, 57), (26, 56), (27, 55), (28, 55), (29, 54),
    ]
    for x, y in limb_veins:
        pixels[x, y] = (83, 32, 126, 255)

    # Bright rift marks and particles. These pixels are intentionally much
    # brighter than the body so the eye and seams read clearly in-game.
    for y in range(0, 16):
        for x in range(48, 64):
            cx, cy = 55.5, 7.5
            dist = abs(x - cx) + abs(y - cy)
            if dist <= 1:
                color = (255, 205, 255, 255)
            elif dist <= 3:
                color = (205, 91, 255, 255)
            elif dist <= 6:
                color = (126, 39, 205, 255)
            else:
                color = (49, 18, 76, 255)
            pixels[x, y] = color

    # Thin vertical seam source used by chest, back, arms, and legs.
    for y in range(16, 64):
        for x in range(48, 56):
            center = abs(x - 51.5)
            flicker = (y * 5 + x * 3) % 9
            if center < 1:
                pixels[x, y] = (225, 112 + flicker * 5, 255, 255)
            elif center < 2.5:
                pixels[x, y] = (126, 39, 203, 255)
            else:
                pixels[x, y] = (42, 16, 68, 255)

    # Small shard/particle source.
    for y in range(16, 64):
        for x in range(56, 64):
            sparkle = (x * 11 + y * 7) % 13
            pixels[x, y] = (
                180 + sparkle * 4 if sparkle < 5 else 80,
                75 + sparkle * 5 if sparkle < 5 else 27,
                255 if sparkle < 5 else 135,
                255,
            )

    TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    image.save(TEXTURE)


def geo_cube(c):
    data = {
        "origin": c["origin"],
        "size": c["size"],
        "uv": c["uv"],
    }
    if any(c["rotation"]):
        data["rotation"] = c["rotation"]
        data["pivot"] = c["pivot"]
    if c["inflate"]:
        data["inflate"] = c["inflate"]
    return data


def write_geo():
    geometry = {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.rift_splinter",
                "texture_width": 64,
                "texture_height": 64,
                "visible_bounds_width": 2.0,
                "visible_bounds_height": 2.0,
                "visible_bounds_offset": [0, 0.8, 0],
            },
            "bones": [
                {
                    "name": b["name"],
                    **({"parent": b["parent"]} if b.get("parent") else {}),
                    "pivot": list(b["pivot"]),
                    **({"cubes": [geo_cube(c) for c in b["cubes"]]} if b["cubes"] else {}),
                }
                for b in BONES
            ],
        }],
    }
    GEO.parent.mkdir(parents=True, exist_ok=True)
    GEO.write_text(json.dumps(geometry, indent=2), encoding="utf-8")


def write_animations():
    data = {"format_version": "1.8.0", "animations": ANIMATIONS}
    ANIM.parent.mkdir(parents=True, exist_ok=True)
    ANIM.write_text(json.dumps(data, indent=2), encoding="utf-8")


def bb_faces(uv):
    u, v = uv
    return {
        "north": {"uv": [u, v, min(u + 4, 64), min(v + 4, 64)]},
        "east": {"uv": [u, v, min(u + 4, 64), min(v + 4, 64)]},
        "south": {"uv": [u, v, min(u + 4, 64), min(v + 4, 64)]},
        "west": {"uv": [u, v, min(u + 4, 64), min(v + 4, 64)]},
        "up": {"uv": [u, v, min(u + 4, 64), min(v + 4, 64)]},
        "down": {"uv": [u, v, min(u + 4, 64), min(v + 4, 64)]},
    }


def write_bbmodel():
    bone_ids = {b["name"]: uid() for b in BONES}
    elements = []
    group_children = {b["name"]: [] for b in BONES}

    for bone in BONES:
        for c in bone["cubes"]:
            eid = uid()
            elements.append({
                "name": c["name"],
                "box_uv": False,
                "render_order": "default",
                "locked": False,
                "export": True,
                "from": c["origin"],
                "to": [c["origin"][i] + c["size"][i] for i in range(3)],
                "autouv": 0,
                "color": 5 if c["glow"] else 0,
                "rotation": c["rotation"],
                "origin": c["pivot"],
                "faces": bb_faces(c["uv"]),
                "type": "cube",
                "uuid": eid,
            })
            group_children[bone["name"]].append(eid)

    def group_node(bone):
        children = list(group_children[bone["name"]])
        for child in BONES:
            if child.get("parent") == bone["name"]:
                children.append(group_node(child))
        return {
            "name": bone["name"],
            "uuid": bone_ids[bone["name"]],
            "export": True,
            "locked": False,
            "origin": list(bone["pivot"]),
            "rotation": [0, 0, 0],
            "color": 5 if bone["name"] == "rift_particles" else 0,
            "children": children,
            "reset": False,
            "shade": True,
            "mirror_uv": False,
            "visibility": True,
            "autouv": 0,
            "isOpen": True,
        }

    def bb_keyframes(channel, values):
        frames = []
        for time, value in values.items():
            frames.append({
                "channel": channel,
                "data_points": [{"x": str(value[0]), "y": str(value[1]), "z": str(value[2])}],
                "uuid": uid(),
                "time": float(time),
                "color": -1,
                "interpolation": "catmullrom",
            })
        return frames

    bb_animations = []
    for name, animation in ANIMATIONS.items():
        animators = {}
        for bone_name, channels in animation["bones"].items():
            frames = []
            for channel, values in channels.items():
                frames.extend(bb_keyframes(channel, values))
            animators[bone_ids[bone_name]] = {
                "name": bone_name,
                "type": "bone",
                "rotation_global": False,
                "quaternion_interpolation": False,
                "keyframes": frames,
            }
        bb_animations.append({
            "uuid": uid(),
            "name": name.removeprefix("animation.rift_splinter."),
            "loop": "loop" if animation.get("loop") else "once",
            "override": False,
            "length": animation["animation_length"],
            "snapping": 24,
            "selected": False,
            "saved": True,
            "path": "",
            "animators": animators,
        })

    texture_uuid = uid()
    model = {
        "meta": {"format_version": "5.0", "model_format": "geckolib_model", "box_uv": False},
        "name": "Rift_Splinter",
        "model_identifier": "rift_splinter",
        "visible_box": [2, 2, 0],
        "geckolib_modid": "riftborne",
        "resolution": {"width": 64, "height": 64},
        "elements": elements,
        "groups": [],
        "outliner": [group_node(next(b for b in BONES if b["name"] == "root"))],
        "textures": [{
            "path": "../src/main/resources/assets/riftborne/textures/entity/rift_splinter.png",
            "name": "rift_splinter.png",
            "folder": "entity",
            "namespace": "riftborne",
            "id": "0",
            "particle": False,
            "render_mode": "default",
            "render_sides": "auto",
            "frame_time": 1,
            "frame_order_type": "loop",
            "frame_order": "",
            "frame_interpolate": False,
            "visible": True,
            "internal": False,
            "saved": True,
            "uuid": texture_uuid,
            "relative_path": "entity/rift_splinter.png",
            "source": str(TEXTURE).replace("\\", "/"),
        }],
        "animations": bb_animations,
        "geckolib_model_type": "Entity",
    }
    BBMODEL.parent.mkdir(parents=True, exist_ok=True)
    BBMODEL.write_text(json.dumps(model, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")


if __name__ == "__main__":
    write_texture()
    write_geo()
    write_animations()
    write_bbmodel()
    print(f"Generated {BBMODEL.relative_to(ROOT)}")
    print(f"Generated {GEO.relative_to(ROOT)}")
    print(f"Generated {ANIM.relative_to(ROOT)}")
    print(f"Generated {TEXTURE.relative_to(ROOT)}")
