from __future__ import annotations

import json
import shutil
import uuid
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE_MODEL = Path(r"C:\Users\gamet\Downloads\Rift Splinter.bbmodel")
SOURCE_TEXTURE = Path(r"C:\Users\gamet\Downloads\texture_8453.png")

SOURCE_COPY = ROOT / "bbmodels" / "Rift_Splinter_Quinta_Source.bbmodel"
OUTPUT_MODEL = ROOT / "bbmodels" / "Rift_Splinter.bbmodel"
OUTPUT_TEXTURE = ROOT / "src/main/resources/assets/riftborne/textures/entity/rift_splinter.png"
OUTPUT_GEO = ROOT / "src/main/resources/assets/riftborne/geo/entity/rift_splinter.geo.json"
OUTPUT_ANIMATION = ROOT / "src/main/resources/assets/riftborne/animations/entity/rift_splinter.animation.json"

# Keep Quinta's geometry and box-UV pixel dimensions exactly 1:1. The entity
# is reduced to 1.6 blocks by the GeckoLib renderer instead. Scaling the cubes
# here changes face dimensions and causes Blockbench to rebuild box UVs.
SCALE = 1.0


def uid() -> str:
    return str(uuid.uuid4())


def scaled(values):
    return [round(float(value) * SCALE, 4) for value in values]


def group(name, origin, children):
    return {
        "name": name,
        "uuid": uid(),
        "export": True,
        "locked": False,
        "origin": list(origin),
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


def keyframes(channel, entries):
    return [{
        "channel": channel,
        "data_points": [{"x": str(value[0]), "y": str(value[1]), "z": str(value[2])}],
        "uuid": uid(),
        "time": time,
        "color": -1,
        "interpolation": "catmullrom",
    } for time, value in entries]


def animator(bone, channels):
    frames = []
    for channel, entries in channels.items():
        frames.extend(keyframes(channel, entries))
    return bone["uuid"], {
        "name": bone["name"],
        "type": "bone",
        "rotation_global": False,
        "quaternion_interpolation": False,
        "keyframes": frames,
    }


def animation(name, length, loop, animators):
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
        "animators": dict(animators),
    }


def face_to_geo(face):
    uv = face["uv"]
    return {
        "uv": [uv[0], uv[1]],
        "uv_size": [uv[2] - uv[0], uv[3] - uv[1]],
    }


def main():
    source_text = SOURCE_MODEL.read_text(encoding="utf-8-sig")
    model = json.loads(source_text)

    SOURCE_COPY.parent.mkdir(parents=True, exist_ok=True)
    SOURCE_COPY.write_text(source_text, encoding="utf-8")

    # The exact supplied texture is copied byte-for-byte. No color correction,
    # sharpening, alpha edits, or generated pixels.
    OUTPUT_TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(SOURCE_TEXTURE, OUTPUT_TEXTURE)

    names = [
        "right_leg_base",
        "left_leg_base",
        "pelvis",
        "lower_torso",
        "upper_torso",
        "head",
        "left_arm_base",
        "right_arm_base",
        "left_arm_detail",
        "right_arm_detail",
        "left_leg_detail",
        "right_leg_detail",
    ]

    elements = model["elements"]
    for index, element in enumerate(elements):
        element["name"] = names[index]
        element["from"] = scaled(element["from"])
        element["to"] = scaled(element["to"])
        element["origin"] = scaled(element.get("origin", [0, 0, 0]))
        element["rotation"] = element.get("rotation") or [0, 0, 0]
        # GeckoLib supports one entity texture. Quinta's first embedded image
        # (687.png) is a temporary paint/reference sheet, while texture_8453
        # is the actual finished skin. Make the finished skin the sole texture
        # and remap every otherwise unchanged face to index zero.
        for face in element["faces"].values():
            face["texture"] = 0

    ids = {element["name"]: element["uuid"] for element in elements}

    root = group("root", (0, 0, 0), [])
    body = group("body", (0, 22, 0), [
        ids["pelvis"],
        ids["lower_torso"],
        ids["upper_torso"],
    ])
    head = group("head", (0, 37, 0), [ids["head"]])
    left_arm = group("left_arm", (-5, 37, 0), [
        ids["left_arm_base"],
        ids["left_arm_detail"],
    ])
    right_arm = group("right_arm", (5, 37, 0), [
        ids["right_arm_base"],
        ids["right_arm_detail"],
    ])
    left_leg = group("left_leg", (-3, 22, 0), [
        ids["left_leg_base"],
        ids["left_leg_detail"],
    ])
    right_leg = group("right_leg", (3, 22, 0), [
        ids["right_leg_base"],
        ids["right_leg_detail"],
    ])

    body["children"].extend([head, left_arm, right_arm])
    root["children"].extend([body, left_leg, right_leg])
    groups = [root, body, head, left_arm, right_arm, left_leg, right_leg]

    animations = [
        animation("idle", 2.0, True, [
            animator(body, {
                "position": [(0, (0, 0, 0)), (1, (0, 0.12, 0)), (2, (0, 0, 0))],
            }),
            animator(head, {
                "rotation": [(0, (0, -1, 0)), (1, (1, 1, -0.5)), (2, (0, -1, 0))],
            }),
        ]),
        animation("walk", 0.8, True, [
            animator(left_leg, {
                "rotation": [(0, (-18, 0, 0)), (0.4, (18, 0, 0)), (0.8, (-18, 0, 0))],
            }),
            animator(right_leg, {
                "rotation": [(0, (18, 0, 0)), (0.4, (-18, 0, 0)), (0.8, (18, 0, 0))],
            }),
            animator(left_arm, {
                "rotation": [(0, (14, 0, 0)), (0.4, (-14, 0, 0)), (0.8, (14, 0, 0))],
            }),
            animator(right_arm, {
                "rotation": [(0, (-14, 0, 0)), (0.4, (14, 0, 0)), (0.8, (-14, 0, 0))],
            }),
        ]),
        animation("dash_attack", 0.65, False, [
            animator(root, {
                "rotation": [(0, (0, 0, 0)), (0.22, (32, 0, 0)), (0.65, (0, 0, 0))],
            }),
            animator(left_arm, {
                "rotation": [(0, (0, 0, 0)), (0.22, (-35, 0, -8)), (0.65, (0, 0, 0))],
            }),
            animator(right_arm, {
                "rotation": [(0, (0, 0, 0)), (0.22, (-35, 0, 8)), (0.65, (0, 0, 0))],
            }),
        ]),
        animation("hurt", 0.35, False, [
            animator(root, {
                "rotation": [(0, (0, 0, 0)), (0.12, (-8, 0, 6)), (0.35, (0, 0, 0))],
            }),
        ]),
        animation("death", 1.2, False, [
            animator(root, {
                "rotation": [(0, (0, 0, 0)), (1.2, (0, 0, 78))],
                "position": [(0, (0, 0, 0)), (1.2, (0, -4.2, 0))],
            }),
        ]),
    ]

    # Keep only the actual skin. Leaving 687.png first makes GeckoLib display
    # its cyan/green/white placeholder regions instead of the finished model.
    actual_texture = model["textures"][1]
    actual_texture["path"] = "../src/main/resources/assets/riftborne/textures/entity/rift_splinter.png"
    actual_texture["relative_path"] = "entity/rift_splinter.png"
    actual_texture["name"] = "rift_splinter.png"
    actual_texture["namespace"] = "riftborne"
    actual_texture["folder"] = "entity"
    actual_texture["id"] = "0"
    textures = [actual_texture]

    model["meta"] = {
        "format_version": "5.0",
        "model_format": "geckolib_model",
        "box_uv": True,
    }
    model["name"] = "Rift_Splinter"
    model["model_identifier"] = "rift_splinter"
    model["geckolib_modid"] = "riftborne"
    model["geckolib_model_type"] = "Entity"
    model["visible_box"] = [2.0, 3.125, 0]
    model["groups"] = groups
    model["outliner"] = [root]
    model["textures"] = textures
    model["animations"] = animations
    model.pop("modded_entity_entity_class", None)
    model.pop("modded_entity_version", None)
    model.pop("modded_entity_flip_y", None)

    OUTPUT_MODEL.write_text(
        json.dumps(model, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )

    parent = {
        "body": "root",
        "head": "body",
        "left_arm": "body",
        "right_arm": "body",
        "left_leg": "root",
        "right_leg": "root",
    }
    element_by_id = {element["uuid"]: element for element in elements}
    geo_bones = []
    for bone in groups:
        geo_bone = {
            "name": bone["name"],
            "pivot": bone["origin"],
        }
        if bone["name"] in parent:
            geo_bone["parent"] = parent[bone["name"]]

        cubes = []
        for child in bone["children"]:
            if not isinstance(child, str):
                continue
            element = element_by_id[child]
            cubes.append({
                "origin": element["from"],
                "size": [
                    round(element["to"][axis] - element["from"][axis], 4)
                    for axis in range(3)
                ],
                "pivot": element["origin"],
                "rotation": element["rotation"],
                "uv": {
                    direction: face_to_geo(face)
                    for direction, face in element["faces"].items()
                },
            })
        if cubes:
            geo_bone["cubes"] = cubes
        geo_bones.append(geo_bone)

    OUTPUT_GEO.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT_GEO.write_text(json.dumps({
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

    runtime_animations = {}
    for item in animations:
        bones = {}
        for animator_data in item["animators"].values():
            channels = {}
            for frame in animator_data["keyframes"]:
                point = frame["data_points"][0]
                channels.setdefault(frame["channel"], {})[str(frame["time"])] = [
                    float(point["x"]),
                    float(point["y"]),
                    float(point["z"]),
                ]
            bones[animator_data["name"]] = channels

        runtime_animations[f"animation.rift_splinter.{item['name']}"] = {
            **({"loop": True} if item["loop"] == "loop" else {}),
            "animation_length": item["length"],
            "bones": bones,
        }

    OUTPUT_ANIMATION.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT_ANIMATION.write_text(json.dumps({
        "format_version": "1.8.0",
        "animations": runtime_animations,
    }, ensure_ascii=False, indent=2), encoding="utf-8")

    print("Restored Quinta geometry and texture without visual edits.")
    print("Prepared seven animation bones and five animation tracks.")


if __name__ == "__main__":
    main()
