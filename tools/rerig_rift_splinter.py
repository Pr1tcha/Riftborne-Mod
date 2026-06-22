from __future__ import annotations

import copy
import json
import uuid
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE = Path(r"C:\Users\gamet\Downloads\Rift Splinter.bbmodel")
BASE_MODEL = ROOT / "bbmodels/Rift_Splinter.bbmodel"
PREPARED = ROOT / "bbmodels/Rift_Splinter_Rerig.bbmodel"
GEO = ROOT / "src/main/resources/assets/riftborne/geo/entity/rift_splinter.geo.json"
ANIMATIONS = ROOT / "src/main/resources/assets/riftborne/animations/entity/rift_splinter.animation.json"


def uid():
    return str(uuid.uuid4())


def split_vertical(source, ranges, names):
    """Split a cube along Y while preserving every vertical UV pixel."""
    y0 = float(source["from"][1])
    y1 = float(source["to"][1])
    result = []

    for (part_y0, part_y1), name in zip(ranges, names):
        part = copy.deepcopy(source)
        part["uuid"] = uid()
        part["name"] = name
        part["from"][1] = part_y0
        part["to"][1] = part_y1
        part["origin"] = [
            (float(part["from"][0]) + float(part["to"][0])) / 2,
            (part_y0 + part_y1) / 2,
            (float(part["from"][2]) + float(part["to"][2])) / 2,
        ]

        for direction in ("north", "east", "south", "west"):
            face = part["faces"][direction]
            u1, v1, u2, v2 = source["faces"][direction]["uv"]
            # The side-face UV rectangle runs from the physical top of the
            # original limb to its bottom. Upper arm/thigh therefore receives
            # the first part of the strip and hand/foot the last part.
            top_fraction = (y1 - part_y1) / (y1 - y0)
            bottom_fraction = (y1 - part_y0) / (y1 - y0)
            face["uv"] = [
                u1,
                round(v1 + (v2 - v1) * top_fraction, 4),
                u2,
                round(v1 + (v2 - v1) * bottom_fraction, 4),
            ]
            face["texture"] = 0

        # Original exterior caps remain exact. Internal caps use the nearest
        # authored cap and are only visible while the joint bends.
        part["faces"]["up"]["uv"] = source["faces"]["up"]["uv"]
        part["faces"]["down"]["uv"] = source["faces"]["down"]["uv"]
        for direction in ("up", "down"):
            part["faces"][direction]["texture"] = 0

        result.append(part)

    return result


def group(name, pivot, children):
    return {
        "name": name,
        "uuid": uid(),
        "export": True,
        "locked": False,
        "origin": list(pivot),
        "rotation": [0, 0, 0],
        "color": 0,
        "children": children,
        "reset": False,
        "shade": True,
        "mirror_uv": False,
        "visibility": True,
        "autouv": 0,
        "isOpen": True,
    }


def frame(channel, time, value, interpolation="catmullrom"):
    return {
        "channel": channel,
        "data_points": [{"x": str(value[0]), "y": str(value[1]), "z": str(value[2])}],
        "uuid": uid(),
        "time": time,
        "color": -1,
        "interpolation": interpolation,
    }


def animator(bone, channels):
    frames = []
    for channel, entries in channels.items():
        for entry in entries:
            time, value = entry[:2]
            interpolation = entry[2] if len(entry) > 2 else "catmullrom"
            frames.append(frame(channel, time, value, interpolation))
    return bone["uuid"], {
        "name": bone["name"],
        "type": "bone",
        "rotation_global": False,
        "quaternion_interpolation": False,
        "keyframes": frames,
    }


def animation(name, length, loop, entries):
    return {
        "uuid": uid(),
        "name": name,
        "loop": "loop" if loop else "once",
        "override": False,
        "length": length,
        "snapping": 24,
        "selected": False,
        "saved": True,
        "path": "",
        "animators": dict(entries),
    }


def face_to_geo(face):
    u1, v1, u2, v2 = face["uv"]
    return {"uv": [u1, v1], "uv_size": [u2 - u1, v2 - v1]}


def main():
    source = json.loads(SOURCE.read_text(encoding="utf-8-sig"))
    current = json.loads(BASE_MODEL.read_text(encoding="utf-8"))
    originals = copy.deepcopy(source["elements"])
    for element in originals:
        element["rotation"] = element.get("rotation") or [0, 0, 0]
        for face in element["faces"].values():
            face["texture"] = 0

    # Unsplit torso and head retain the exact source geometry and UV.
    pelvis, lower_torso, upper_torso, head = [
        copy.deepcopy(originals[index]) for index in (2, 3, 4, 5)
    ]
    pelvis["name"] = "pelvis_cube"
    lower_torso["name"] = "torso_cube"
    upper_torso["name"] = "chest_cube"
    head["name"] = "head_cube"

    # Arms: hand 17..20, forearm 20..27, upper arm 27..37.
    arm_ranges = [(17, 20), (20, 27), (27, 37)]
    left_arm_base = split_vertical(
        originals[6], arm_ranges,
        ["left_hand_base", "left_forearm_base", "left_upper_arm_base"],
    )
    right_arm_base = split_vertical(
        originals[7], arm_ranges,
        ["right_hand_base", "right_forearm_base", "right_upper_arm_base"],
    )
    left_arm_detail = split_vertical(
        originals[8], arm_ranges,
        ["left_hand_detail", "left_forearm_detail", "left_upper_arm_detail"],
    )
    right_arm_detail = split_vertical(
        originals[9], arm_ranges,
        ["right_hand_detail", "right_forearm_detail", "right_upper_arm_detail"],
    )

    # Legs: foot 0..3, shin 3..11, thigh 11..22.
    leg_ranges = [(0, 3), (3, 11), (11, 22)]
    right_leg_base = split_vertical(
        originals[0], leg_ranges,
        ["right_foot_base", "right_shin_base", "right_thigh_base"],
    )
    left_leg_base = split_vertical(
        originals[1], leg_ranges,
        ["left_foot_base", "left_shin_base", "left_thigh_base"],
    )
    left_leg_detail = split_vertical(
        originals[10], leg_ranges,
        ["left_foot_detail", "left_shin_detail", "left_thigh_detail"],
    )
    right_leg_detail = split_vertical(
        originals[11], leg_ranges,
        ["right_foot_detail", "right_shin_detail", "right_thigh_detail"],
    )

    # Preserve Quinta's actual second layer. The source detail cubes use
    # inflate 0.25, giving the purple fracture skin visible thickness and
    # preventing it from collapsing into the base layer.
    for detail in (
        *left_arm_detail, *right_arm_detail,
        *left_leg_detail, *right_leg_detail,
    ):
        detail["inflate"] = 0.25

    elements = [
        pelvis, lower_torso, upper_torso, head,
        *left_arm_base, *right_arm_base,
        *left_arm_detail, *right_arm_detail,
        *left_leg_base, *right_leg_base,
        *left_leg_detail, *right_leg_detail,
    ]
    # Split cubes cannot remain in Box UV mode: Blockbench recalculates their
    # islands from uv_offset and the new cube dimensions every time the file is
    # opened. Per-face UV locks the authored coordinates below exactly.
    for element in elements:
        element["box_uv"] = False
        element.pop("uv_offset", None)
        element["autouv"] = 0
    by_name = {element["name"]: element["uuid"] for element in elements}

    root = group("root", (0, 0, 0), [])
    pelvis_bone = group("pelvis", (0, 22, 0), [by_name["pelvis_cube"]])
    torso = group("torso", (0, 27, 0), [by_name["torso_cube"]])
    chest = group("chest", (0, 31, 0), [by_name["chest_cube"]])
    head_bone = group("head", (0, 37, 0), [by_name["head_cube"]])

    left_upper_arm = group("left_upper_arm", (-5, 37, 0), [
        by_name["left_upper_arm_base"], by_name["left_upper_arm_detail"],
    ])
    left_forearm = group("left_forearm", (-7, 27, 0.5), [
        by_name["left_forearm_base"], by_name["left_forearm_detail"],
    ])
    left_hand = group("left_hand", (-7, 20, 0.5), [
        by_name["left_hand_base"], by_name["left_hand_detail"],
    ])

    right_upper_arm = group("right_upper_arm", (5, 37, 0), [
        by_name["right_upper_arm_base"], by_name["right_upper_arm_detail"],
    ])
    right_forearm = group("right_forearm", (7, 27, 0.5), [
        by_name["right_forearm_base"], by_name["right_forearm_detail"],
    ])
    right_hand = group("right_hand", (7, 20, 0.5), [
        by_name["right_hand_base"], by_name["right_hand_detail"],
    ])

    left_thigh = group("left_thigh", (-3, 22, 0), [
        by_name["left_thigh_base"], by_name["left_thigh_detail"],
    ])
    left_shin = group("left_shin", (-3, 11, 0), [
        by_name["left_shin_base"], by_name["left_shin_detail"],
    ])
    left_foot = group("left_foot", (-3, 3, 0), [
        by_name["left_foot_base"], by_name["left_foot_detail"],
    ])

    right_thigh = group("right_thigh", (3, 22, 0), [
        by_name["right_thigh_base"], by_name["right_thigh_detail"],
    ])
    right_shin = group("right_shin", (3, 11, 0), [
        by_name["right_shin_base"], by_name["right_shin_detail"],
    ])
    right_foot = group("right_foot", (3, 3, 0), [
        by_name["right_foot_base"], by_name["right_foot_detail"],
    ])

    left_forearm["children"].append(left_hand)
    left_upper_arm["children"].append(left_forearm)
    right_forearm["children"].append(right_hand)
    right_upper_arm["children"].append(right_forearm)
    left_shin["children"].append(left_foot)
    left_thigh["children"].append(left_shin)
    right_shin["children"].append(right_foot)
    right_thigh["children"].append(right_shin)
    chest["children"].extend([head_bone, left_upper_arm, right_upper_arm])
    torso["children"].append(chest)
    pelvis_bone["children"].extend([torso, left_thigh, right_thigh])
    root["children"].append(pelvis_bone)

    groups = [
        root, pelvis_bone, torso, chest, head_bone,
        left_upper_arm, left_forearm, left_hand,
        right_upper_arm, right_forearm, right_hand,
        left_thigh, left_shin, left_foot,
        right_thigh, right_shin, right_foot,
    ]

    # Lightweight test motions prove every new joint is wired correctly.
    animations = [
        animation("idle", 2.0, True, [
            animator(pelvis_bone, {
                "position": [(0, (0, -1.15, 0)), (1, (0, -1.05, 0)), (2, (0, -1.15, 0))],
                "rotation": [(0, (-8, 0, 0)), (1, (-7, 0, 0)), (2, (-8, 0, 0))],
            }),
            animator(torso, {
                "rotation": [(0, (11, 0, 0)), (1, (9.5, 0, 0)), (2, (11, 0, 0))],
            }),
            animator(chest, {
                "rotation": [(0, (8, 0, 0)), (1, (10, 0, 0)), (2, (8, 0, 0))],
            }),
            animator(head_bone, {
                # Small pauses followed by fast, suspicious snaps.
                "rotation": [
                    (0.00, (-6, -18, -2), "linear"),
                    (0.38, (-6, -18, -2), "linear"),
                    (0.47, (-3, 20, 2), "linear"),
                    (0.92, (-3, 20, 2), "linear"),
                    (1.00, (-8, 4, -1), "linear"),
                    (1.42, (-8, 4, -1), "linear"),
                    (1.50, (-5, -18, -2), "linear"),
                    (2.00, (-6, -18, -2), "linear"),
                ],
            }),
            animator(left_upper_arm, {
                "rotation": [(0, (7, 0, -5)), (1, (5, 0, -4)), (2, (7, 0, -5))],
            }),
            animator(right_upper_arm, {
                "rotation": [(0, (7, 0, 5)), (1, (5, 0, 4)), (2, (7, 0, 5))],
            }),
            animator(left_forearm, {
                "rotation": [(0, (-18, 0, -2)), (1, (-22, 0, 0)), (2, (-18, 0, -2))],
            }),
            animator(right_forearm, {
                "rotation": [(0, (-18, 0, 2)), (1, (-22, 0, 0)), (2, (-18, 0, 2))],
            }),
            animator(left_thigh, {
                "rotation": [(0, (7, 0, -3)), (1, (6, 0, -3)), (2, (7, 0, -3))],
            }),
            animator(right_thigh, {
                "rotation": [(0, (7, 0, 3)), (1, (6, 0, 3)), (2, (7, 0, 3))],
            }),
            animator(left_shin, {
                "rotation": [(0, (14, 0, 0)), (1, (12, 0, 0)), (2, (14, 0, 0))],
            }),
            animator(right_shin, {
                "rotation": [(0, (14, 0, 0)), (1, (12, 0, 0)), (2, (14, 0, 0))],
            }),
        ]),
        animation("walk", 0.8, True, [
            animator(pelvis_bone, {
                "position": [(0, (-0.18, -0.65, 0)), (0.2, (0, -0.25, 0)),
                             (0.4, (0.18, -0.65, 0)), (0.6, (0, -0.25, 0)),
                             (0.8, (-0.18, -0.65, 0))],
                "rotation": [(0, (-5, -3, -2)), (0.2, (-7, 0, 0)),
                             (0.4, (-5, 3, 2)), (0.6, (-7, 0, 0)),
                             (0.8, (-5, -3, -2))],
            }),
            animator(torso, {
                "rotation": [(0, (10, 3, 2)), (0.2, (8, 0, 0)),
                             (0.4, (10, -3, -2)), (0.6, (8, 0, 0)),
                             (0.8, (10, 3, 2))],
            }),
            animator(chest, {
                "rotation": [(0, (5, 4, 1)), (0.4, (5, -4, -1)), (0.8, (5, 4, 1))],
            }),
            animator(head_bone, {
                "rotation": [(0, (-5, -3, -1)), (0.2, (-8, 0, 0)),
                             (0.4, (-5, 3, 1)), (0.6, (-8, 0, 0)),
                             (0.8, (-5, -3, -1))],
            }),
            animator(left_thigh, {
                "rotation": [(0, (-30, 0, -2)), (0.2, (-6, 0, 0)),
                             (0.4, (27, 0, 2)), (0.6, (8, 0, 0)),
                             (0.8, (-30, 0, -2))],
            }),
            animator(right_thigh, {
                "rotation": [(0, (27, 0, 2)), (0.2, (8, 0, 0)),
                             (0.4, (-30, 0, -2)), (0.6, (-6, 0, 0)),
                             (0.8, (27, 0, 2))],
            }),
            animator(left_shin, {
                "rotation": [(0, (38, 0, 0)), (0.2, (10, 0, 0)),
                             (0.4, (8, 0, 0)), (0.6, (42, 0, 0)),
                             (0.8, (38, 0, 0))],
            }),
            animator(right_shin, {
                "rotation": [(0, (8, 0, 0)), (0.2, (42, 0, 0)),
                             (0.4, (38, 0, 0)), (0.6, (10, 0, 0)),
                             (0.8, (8, 0, 0))],
            }),
            animator(left_foot, {
                "rotation": [(0, (-12, 0, 0)), (0.2, (7, 0, 0)),
                             (0.4, (-5, 0, 0)), (0.6, (-16, 0, 0)),
                             (0.8, (-12, 0, 0))],
            }),
            animator(right_foot, {
                "rotation": [(0, (-5, 0, 0)), (0.2, (-16, 0, 0)),
                             (0.4, (-12, 0, 0)), (0.6, (7, 0, 0)),
                             (0.8, (-5, 0, 0))],
            }),
            animator(left_upper_arm, {
                "rotation": [(0, (24, 0, -4)), (0.2, (5, 0, -2)),
                             (0.4, (-25, 0, 3)), (0.6, (-5, 0, 1)),
                             (0.8, (24, 0, -4))],
            }),
            animator(right_upper_arm, {
                "rotation": [(0, (-25, 0, 4)), (0.2, (-5, 0, 2)),
                             (0.4, (24, 0, -3)), (0.6, (5, 0, -1)),
                             (0.8, (-25, 0, 4))],
            }),
            animator(left_forearm, {
                "rotation": [(0, (-24, 0, 0)), (0.2, (-12, 0, 0)),
                             (0.4, (-6, 0, 0)), (0.6, (-18, 0, 0)),
                             (0.8, (-24, 0, 0))],
            }),
            animator(right_forearm, {
                "rotation": [(0, (-6, 0, 0)), (0.2, (-18, 0, 0)),
                             (0.4, (-24, 0, 0)), (0.6, (-12, 0, 0)),
                             (0.8, (-6, 0, 0))],
            }),
        ]),
        animation("dash_attack", 0.82, False, [
            # 0.00-0.28: drops onto all fours and holds tension.
            # 0.28-0.52: launches into the horizontal pounce.
            # 0.52-0.82: catches the landing and returns control.
            animator(pelvis_bone, {
                "position": [(0.00, (0, 0, 0), "linear"),
                             (0.12, (0, -4.4, 0.35), "linear"),
                             (0.28, (0, -5.5, 0.65), "linear"),
                             (0.42, (0, 1.6, -0.8), "linear"),
                             (0.56, (0, 2.4, -1.2), "linear"),
                             (0.70, (0, -1.2, 0), "linear"),
                             (0.82, (0, 0, 0), "linear")],
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.12, (-14, 0, 0), "linear"),
                             (0.28, (-20, 0, 0), "linear"),
                             (0.56, (-4, 0, 0), "linear"),
                             (0.82, (0, 0, 0), "linear")],
            }),
            animator(torso, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.12, (28, 0, 0), "linear"),
                             (0.28, (42, 0, 0), "linear"),
                             (0.42, (62, 0, 0), "linear"),
                             (0.56, (70, 0, 0), "linear"),
                             (0.70, (24, 0, 0), "linear"),
                             (0.82, (0, 0, 0), "linear")],
            }),
            animator(chest, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.12, (17, 0, 0), "linear"),
                             (0.28, (24, 0, 0), "linear"),
                             (0.56, (15, 0, 0), "linear"),
                             (0.82, (0, 0, 0), "linear")],
            }),
            animator(head_bone, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.12, (-12, 0, 0), "linear"),
                             (0.28, (-24, 0, 0), "linear"),
                             (0.42, (-45, 0, 0), "linear"),
                             (0.56, (-52, 0, 0), "linear"),
                             (0.70, (-16, 0, 0), "linear"),
                             (0.82, (0, 0, 0), "linear")],
            }),
            animator(left_upper_arm, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.12, (-48, 0, -8), "linear"),
                             (0.28, (-58, 0, -10), "linear"),
                             (0.42, (-82, 0, -4), "linear"),
                             (0.56, (-90, 0, 0), "linear"),
                             (0.70, (-36, 0, -5), "linear"),
                             (0.82, (0, 0, 0), "linear")],
            }),
            animator(right_upper_arm, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.12, (-48, 0, 8), "linear"),
                             (0.28, (-58, 0, 10), "linear"),
                             (0.42, (-82, 0, 4), "linear"),
                             (0.56, (-90, 0, 0), "linear"),
                             (0.70, (-36, 0, 5), "linear"),
                             (0.82, (0, 0, 0), "linear")],
            }),
            animator(left_forearm, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.12, (62, 0, 0), "linear"),
                             (0.28, (74, 0, 0), "linear"),
                             (0.42, (24, 0, 0), "linear"),
                             (0.56, (8, 0, 0), "linear"),
                             (0.70, (48, 0, 0), "linear"),
                             (0.82, (0, 0, 0), "linear")],
            }),
            animator(right_forearm, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.12, (62, 0, 0), "linear"),
                             (0.28, (74, 0, 0), "linear"),
                             (0.42, (24, 0, 0), "linear"),
                             (0.56, (8, 0, 0), "linear"),
                             (0.70, (48, 0, 0), "linear"),
                             (0.82, (0, 0, 0), "linear")],
            }),
            animator(left_thigh, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.12, (-34, 0, -4), "linear"),
                             (0.28, (-48, 0, -5), "linear"),
                             (0.42, (18, 0, -3), "linear"),
                             (0.56, (26, 0, 0), "linear"),
                             (0.70, (-18, 0, -2), "linear"),
                             (0.82, (0, 0, 0), "linear")],
            }),
            animator(right_thigh, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.12, (-34, 0, 4), "linear"),
                             (0.28, (-48, 0, 5), "linear"),
                             (0.42, (18, 0, 3), "linear"),
                             (0.56, (26, 0, 0), "linear"),
                             (0.70, (-18, 0, 2), "linear"),
                             (0.82, (0, 0, 0), "linear")],
            }),
            animator(left_shin, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.12, (66, 0, 0), "linear"),
                             (0.28, (82, 0, 0), "linear"),
                             (0.42, (70, 0, 0), "linear"),
                             (0.56, (58, 0, 0), "linear"),
                             (0.70, (38, 0, 0), "linear"),
                             (0.82, (0, 0, 0), "linear")],
            }),
            animator(right_shin, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.12, (66, 0, 0), "linear"),
                             (0.28, (82, 0, 0), "linear"),
                             (0.42, (70, 0, 0), "linear"),
                             (0.56, (58, 0, 0), "linear"),
                             (0.70, (38, 0, 0), "linear"),
                             (0.82, (0, 0, 0), "linear")],
            }),
        ]),
        animation("hurt", 0.52, False, [
            animator(root, {
                "position": [(0.00, (0, 0, 0), "linear"),
                             (0.08, (0, 0, 0.75), "linear"),
                             (0.18, (0, -0.35, 1.15), "linear"),
                             (0.34, (0, -0.15, 0.35), "linear"),
                             (0.52, (0, 0, 0), "linear")],
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.08, (-13, -4, 8), "linear"),
                             (0.18, (-18, -7, 11), "linear"),
                             (0.34, (5, 2, -4), "linear"),
                             (0.52, (0, 0, 0), "linear")],
            }),
            animator(chest, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.10, (-17, -8, 6), "linear"),
                             (0.24, (8, 4, -3), "linear"),
                             (0.52, (0, 0, 0), "linear")],
            }),
            animator(head_bone, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.10, (21, 13, -9), "linear"),
                             (0.22, (-8, -5, 4), "linear"),
                             (0.52, (0, 0, 0), "linear")],
            }),
            animator(left_upper_arm, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.13, (25, 0, -18), "linear"),
                             (0.30, (-9, 0, 5), "linear"),
                             (0.52, (0, 0, 0), "linear")],
            }),
            animator(right_upper_arm, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.13, (31, 0, 15), "linear"),
                             (0.30, (-7, 0, -4), "linear"),
                             (0.52, (0, 0, 0), "linear")],
            }),
            animator(left_thigh, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.16, (-14, 0, -5), "linear"),
                             (0.34, (8, 0, 2), "linear"),
                             (0.52, (0, 0, 0), "linear")],
            }),
            animator(right_thigh, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.16, (17, 0, 4), "linear"),
                             (0.34, (-6, 0, -2), "linear"),
                             (0.52, (0, 0, 0), "linear")],
            }),
        ]),
        animation("death", 1.65, False, [
            animator(root, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.28, (-8, 0, 3), "linear"),
                             (0.62, (-20, 0, -7), "linear"),
                             (0.95, (-48, 0, -16), "linear"),
                             (1.20, (-70, 0, -25), "linear"),
                             (1.65, (-78, 0, -31), "linear")],
                "position": [(0.00, (0, 0, 0), "linear"),
                             (0.42, (0, -1.2, 0), "linear"),
                             (0.78, (0, -4.0, -0.5), "linear"),
                             (1.20, (0, -7.2, -1.2), "linear"),
                             (1.65, (0, -8.0, -1.6), "linear")],
            }),
            animator(pelvis_bone, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.45, (-12, 0, 5), "linear"),
                             (0.82, (-28, 0, 12), "linear"),
                             (1.65, (-34, 0, 18), "linear")],
            }),
            animator(torso, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.45, (18, 0, -5), "linear"),
                             (0.82, (39, 0, -13), "linear"),
                             (1.65, (52, 0, -20), "linear")],
                "position": [(0.00, (0, 0, 0), "linear"),
                             (0.82, (0, 0.4, 0), "linear"),
                             (1.20, (0.5, 1.0, -0.5), "linear"),
                             (1.65, (0.8, 1.6, -0.8), "linear")],
            }),
            animator(head_bone, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.32, (-14, 8, 5), "linear"),
                             (0.70, (28, -18, -13), "linear"),
                             (1.05, (65, -45, -28), "linear"),
                             (1.65, (92, -74, -43), "linear")],
                "position": [(0.00, (0, 0, 0), "linear"),
                             (0.70, (0, 0, 0), "linear"),
                             (1.05, (1.5, 1.5, -0.5), "linear"),
                             (1.65, (4.2, 4.8, -2.3), "linear")],
            }),
            animator(left_upper_arm, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.45, (34, 0, -20), "linear"),
                             (0.82, (61, 14, -42), "linear"),
                             (1.65, (103, 28, -71), "linear")],
                "position": [(0.00, (0, 0, 0), "linear"),
                             (0.82, (0, 0, 0), "linear"),
                             (1.20, (-1.8, 0.5, 0.7), "linear"),
                             (1.65, (-4.8, 2.2, 2.0), "linear")],
            }),
            animator(right_upper_arm, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.45, (29, 0, 18), "linear"),
                             (0.82, (57, -12, 39), "linear"),
                             (1.65, (96, -25, 67), "linear")],
                "position": [(0.00, (0, 0, 0), "linear"),
                             (0.82, (0, 0, 0), "linear"),
                             (1.20, (1.5, 0.8, -0.4), "linear"),
                             (1.65, (4.4, 2.8, -1.3), "linear")],
            }),
            animator(left_forearm, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.45, (-38, 0, 16), "linear"),
                             (0.82, (-72, 0, 31), "linear"),
                             (1.65, (-112, 0, 54), "linear")],
            }),
            animator(right_forearm, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.45, (-34, 0, -14), "linear"),
                             (0.82, (-67, 0, -28), "linear"),
                             (1.65, (-106, 0, -50), "linear")],
            }),
            animator(left_thigh, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.45, (-22, 0, -4), "linear"),
                             (0.82, (-46, 0, -13), "linear"),
                             (1.65, (-69, 0, -24), "linear")],
                "position": [(0.00, (0, 0, 0), "linear"),
                             (1.00, (0, 0, 0), "linear"),
                             (1.65, (-1.8, -0.3, 1.2), "linear")],
            }),
            animator(right_thigh, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.45, (-18, 0, 5), "linear"),
                             (0.82, (-41, 0, 15), "linear"),
                             (1.65, (-63, 0, 27), "linear")],
                "position": [(0.00, (0, 0, 0), "linear"),
                             (1.00, (0, 0, 0), "linear"),
                             (1.65, (1.5, 0.2, -0.8), "linear")],
            }),
            animator(left_shin, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.45, (24, 0, 0), "linear"),
                             (0.82, (53, 0, 0), "linear"),
                             (1.65, (79, 0, 12), "linear")],
            }),
            animator(right_shin, {
                "rotation": [(0.00, (0, 0, 0), "linear"),
                             (0.45, (28, 0, 0), "linear"),
                             (0.82, (58, 0, 0), "linear"),
                             (1.65, (84, 0, -14), "linear")],
            }),
        ]),
        animation("rig_test", 2.0, True, [
            animator(left_forearm, {
                "rotation": [(0, (0, 0, 0)), (0.5, (-38, 0, 0)), (1, (0, 0, 0)), (2, (0, 0, 0))],
            }),
            animator(right_forearm, {
                "rotation": [(0, (0, 0, 0)), (0.5, (-38, 0, 0)), (1, (0, 0, 0)), (2, (0, 0, 0))],
            }),
            animator(left_shin, {
                "rotation": [(0, (0, 0, 0)), (1.5, (42, 0, 0)), (2, (0, 0, 0))],
            }),
            animator(right_shin, {
                "rotation": [(0, (0, 0, 0)), (1.5, (42, 0, 0)), (2, (0, 0, 0))],
            }),
        ]),
    ]

    # Blockbench/GeckoLib's X rotation direction for this imported model is
    # opposite to the pose convention used while drafting the motions. Flip
    # every joint's X rotation once so the torso hunches forward, elbows fold
    # inward, and knees bend backwards instead of producing an arched pose.
    for item in animations:
        for animator_data in item["animators"].values():
            for keyframe in animator_data["keyframes"]:
                if keyframe["channel"] != "rotation":
                    continue
                point = keyframe["data_points"][0]
                point["x"] = str(-float(point["x"]))

    model = current
    model["meta"]["box_uv"] = False
    model["elements"] = elements
    model["groups"] = groups
    model["outliner"] = [root]
    model["animations"] = animations
    PREPARED.write_text(
        json.dumps(model, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )

    parents = {
        "pelvis": "root", "torso": "pelvis", "chest": "torso", "head": "chest",
        "left_upper_arm": "chest", "left_forearm": "left_upper_arm", "left_hand": "left_forearm",
        "right_upper_arm": "chest", "right_forearm": "right_upper_arm", "right_hand": "right_forearm",
        "left_thigh": "pelvis", "left_shin": "left_thigh", "left_foot": "left_shin",
        "right_thigh": "pelvis", "right_shin": "right_thigh", "right_foot": "right_shin",
    }
    element_by_id = {element["uuid"]: element for element in elements}
    geo_bones = []
    for bone in groups:
        geo_bone = {"name": bone["name"], "pivot": bone["origin"]}
        if bone["name"] in parents:
            geo_bone["parent"] = parents[bone["name"]]
        cubes = []
        for child in bone["children"]:
            if not isinstance(child, str):
                continue
            element = element_by_id[child]
            cube = {
                "origin": element["from"],
                "size": [
                    element["to"][axis] - element["from"][axis]
                    for axis in range(3)
                ],
                "pivot": element["origin"],
                "rotation": element["rotation"],
                "uv": {
                    direction: face_to_geo(face)
                    for direction, face in element["faces"].items()
                },
            }
            if element.get("inflate"):
                cube["inflate"] = element["inflate"]
            cubes.append(cube)
        if cubes:
            geo_bone["cubes"] = cubes
        geo_bones.append(geo_bone)

    GEO.write_text(json.dumps({
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.rift_splinter",
                "texture_width": 128,
                "texture_height": 128,
                "visible_bounds_width": 2.0,
                "visible_bounds_height": 3.125,
                "visible_bounds_offset": [0, 1.5625, 0],
            },
            "bones": geo_bones,
        }],
    }, ensure_ascii=False, indent=2), encoding="utf-8")

    runtime = {}
    for item in animations:
        bones = {}
        for data in item["animators"].values():
            channels = {}
            for keyframe in data["keyframes"]:
                point = keyframe["data_points"][0]
                channels.setdefault(keyframe["channel"], {})[str(keyframe["time"])] = [
                    float(point["x"]), float(point["y"]), float(point["z"]),
                ]
            bones[data["name"]] = channels
        runtime[f"animation.rift_splinter.{item['name']}"] = {
            **({"loop": True} if item["loop"] == "loop" else {}),
            "animation_length": item["length"],
            "bones": bones,
        }
    ANIMATIONS.write_text(json.dumps({
        "format_version": "1.8.0",
        "animations": runtime,
    }, ensure_ascii=False, indent=2), encoding="utf-8")

    print(f"Re-rigged {len(elements)} cubes into {len(groups)} bones.")


if __name__ == "__main__":
    main()
