import base64
import io
import json
import uuid
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
BBMODEL = ROOT / "bbmodels" / "RNA_Training_Anchor.bbmodel"
GEO = ROOT / "src/main/resources/assets/riftborne/geo/rna_training_anchor.geo.json"
ANIMATION = ROOT / "src/main/resources/assets/riftborne/animations/rna_training_anchor.animation.json"
TEXTURE = ROOT / "src/main/resources/assets/riftborne/textures/block/rna_training_anchor.png"
GLOWMASK = ROOT / "src/main/resources/assets/riftborne/textures/block/rna_training_anchor_glowmask.png"
ITEM_TEXTURE = ROOT / "src/main/resources/assets/riftborne/textures/item/rna_training_anchor.png"

WIDTH = 64
HEIGHT = 64

SWATCHES = {
    "dark": (0, 0),
    "metal": (8, 0),
    "highlight": (16, 0),
    "teal": (24, 0),
    "cyan": (32, 0),
    "amber": (40, 0),
}


def stable_uuid(name):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, f"riftborne:rna_training_anchor:{name}"))


def face_uv(swatch):
    u, v = SWATCHES[swatch]
    return {
        direction: {"uv": [u, v, u + 6, v + 6], "texture": 0}
        for direction in ("north", "east", "south", "west", "up", "down")
    }


class Model:
    def __init__(self):
        self.elements = []
        self.bones = {}

    def bone(self, name, pivot, parent=None):
        self.bones[name] = {
            "name": name,
            "pivot": pivot,
            "parent": parent,
            "uuid": stable_uuid(f"bone:{name}"),
            "children": [],
            "geo_cubes": [],
        }

    def cube(self, name, bone, origin, size, swatch, rotation=None, pivot=None, inflate=0.0):
        cube_uuid = stable_uuid(f"cube:{name}")
        x, y, z = origin
        sx, sy, sz = size
        faces = face_uv(swatch)
        element = {
            "name": name,
            "box_uv": False,
            "render_order": "default",
            "locked": False,
            "export": True,
            "scope": 0,
            "allow_mirror_modeling": True,
            "from": list(origin),
            "to": [x + sx, y + sy, z + sz],
            "autouv": 0,
            "color": len(self.elements) % 8,
            "origin": list(pivot or [x + sx / 2, y + sy / 2, z + sz / 2]),
            "faces": faces,
            "type": "cube",
            "uuid": cube_uuid,
        }
        if rotation:
            element["rotation"] = list(rotation)
        if inflate:
            element["inflate"] = inflate
        self.elements.append(element)
        self.bones[bone]["children"].append(cube_uuid)

        geo_cube = {
            "origin": list(origin),
            "size": list(size),
            "uv": {
                direction: {"uv": [data["uv"][0], data["uv"][1]], "uv_size": [6, 6]}
                for direction, data in faces.items()
            },
        }
        if rotation:
            geo_cube["rotation"] = list(rotation)
            geo_cube["pivot"] = list(pivot or element["origin"])
        if inflate:
            geo_cube["inflate"] = inflate
        self.bones[bone]["geo_cubes"].append(geo_cube)


model = Model()
model.bone("root", [0, 0, 0])
model.bone("base", [0, 0, 0], "root")
model.bone("stabilizers", [0, 4.5, 0], "root")
model.bone("ring", [0, 7.2, 0], "root")
model.bone("core", [0, 7.2, 0], "root")
model.bone("glow", [0, 7.2, 0], "core")

# Heavy grounded silhouette.
model.cube("base_plate", "base", [-6, 0, -6], [12, 2, 12], "dark")
model.cube("base_upper", "base", [-5.35, 2, -5.35], [10.7, 1.2, 10.7], "metal")
model.cube("central_housing", "base", [-3.8, 3.2, -3.8], [7.6, 2.4, 7.6], "metal")
for name, x, z in (
    ("nw", -5.1, -5.1), ("ne", 3.5, -5.1), ("sw", -5.1, 3.5), ("se", 3.5, 3.5)
):
    model.cube(f"corner_{name}", "base", [x, 2.4, z], [1.6, 3.6, 1.6], "highlight")
model.cube("front_status", "base", [-2.4, 2.75, -5.55], [4.8, 0.55, 0.45], "amber")
model.cube("base_glow_n", "base", [-4.5, 3.0, -5.42], [9, 0.35, 0.22], "cyan")
model.cube("base_glow_s", "base", [-4.5, 3.0, 5.2], [9, 0.35, 0.22], "cyan")

# Four physical field projectors rotate as one assembly when active.
model.cube("projector_n", "stabilizers", [-1.0, 4.25, -6.2], [2, 1.6, 3.5], "teal")
model.cube("projector_s", "stabilizers", [-1.0, 4.25, 2.7], [2, 1.6, 3.5], "teal")
model.cube("projector_w", "stabilizers", [-6.2, 4.25, -1.0], [3.5, 1.6, 2], "teal")
model.cube("projector_e", "stabilizers", [2.7, 4.25, -1.0], [3.5, 1.6, 2], "teal")
for name, x, z in (
    ("n", 0, -5.9), ("s", 0, 5.55), ("w", -5.9, 0), ("e", 5.55, 0)
):
    model.cube(f"projector_glow_{name}", "stabilizers", [x - 0.35, 5.85, z - 0.35], [0.7, 0.45, 0.7], "cyan")

# Rotating square ring keeps a Minecraft-readable hard-surface shape.
model.cube("ring_n", "ring", [-4.8, 6.85, -5.05], [9.6, 0.7, 0.7], "highlight")
model.cube("ring_s", "ring", [-4.8, 6.85, 4.35], [9.6, 0.7, 0.7], "highlight")
model.cube("ring_w", "ring", [-5.05, 6.85, -4.35], [0.7, 0.7, 8.7], "highlight")
model.cube("ring_e", "ring", [4.35, 6.85, -4.35], [0.7, 0.7, 8.7], "highlight")

# Floating RNA calibration core.
model.cube("core_shell", "core", [-2.35, 5.55, -2.35], [4.7, 4.7, 4.7], "teal", inflate=0.05)
model.cube("core_inner", "glow", [-1.55, 6.35, -1.55], [3.1, 3.1, 3.1], "cyan")
model.cube("core_cap", "glow", [-0.8, 10.25, -0.8], [1.6, 1.25, 1.6], "amber")


def group_payload(name):
    bone = model.bones[name]
    return {
        "name": name,
        "uuid": bone["uuid"],
        "export": True,
        "locked": False,
        "scope": 0,
        "selected": False,
        "origin": bone["pivot"],
        "rotation": [0, 0, 0],
        "color": 0,
        "children": [],
        "reset": False,
        "shade": True,
        "mirror_uv": False,
        "visibility": True,
        "autouv": 0,
        "isOpen": True,
    }


def outliner_group(name):
    bone = model.bones[name]
    children = list(bone["children"])
    children.extend(
        outliner_group(child["name"])
        for child in model.bones.values()
        if child["parent"] == name
    )
    return {"uuid": bone["uuid"], "isOpen": True, "children": children}


def bb_keyframe(animation, bone, channel, time, values):
    return {
        "channel": channel,
        "data_points": [{"x": str(values[0]), "y": str(values[1]), "z": str(values[2])}],
        "uuid": stable_uuid(f"{animation}:{bone}:{channel}:{time}"),
        "time": time,
        "color": -1,
        "interpolation": "linear",
    }


def bb_animation(name, length, tracks):
    animators = {}
    for bone_name, channels in tracks.items():
        keyframes = []
        for channel, frames in channels.items():
            keyframes.extend(bb_keyframe(name, bone_name, channel, time, values) for time, values in frames)
        animators[model.bones[bone_name]["uuid"]] = {
            "name": bone_name,
            "type": "bone",
            "rotation_global": False,
            "quaternion_interpolation": False,
            "keyframes": keyframes,
        }
    return {
        "uuid": stable_uuid(f"animation:{name}"),
        "name": name,
        "loop": "loop",
        "override": False,
        "length": length,
        "snapping": 24,
        "selected": name == "idle",
        "saved": True,
        "path": "",
        "scope": 0,
        "anim_time_update": "",
        "blend_weight": "",
        "start_delay": "",
        "loop_delay": "",
        "animators": animators,
    }


idle_tracks = {
    "ring": {"rotation": [(0, (0, 0, 0)), (8, (0, 360, 0))]},
    "core": {"position": [(0, (0, 0, 0)), (2, (0, 0.18, 0)), (4, (0, 0, 0))]},
    "glow": {"scale": [(0, (0.94, 0.94, 0.94)), (2, (1.04, 1.04, 1.04)), (4, (0.94, 0.94, 0.94))]},
}
active_tracks = {
    "ring": {"rotation": [(0, (0, 0, 0)), (1.6, (0, 360, 0))]},
    "stabilizers": {"rotation": [(0, (0, 0, 0)), (3.2, (0, -360, 0))]},
    "core": {"position": [(0, (0, 0, 0)), (0.8, (0, 0.42, 0)), (1.6, (0, 0, 0))]},
    "glow": {"scale": [(0, (0.9, 0.9, 0.9)), (0.4, (1.12, 1.12, 1.12)), (0.8, (0.9, 0.9, 0.9))]},
}


def create_texture():
    image = Image.new("RGBA", (WIDTH, HEIGHT), (13, 18, 21, 255))
    draw = ImageDraw.Draw(image)
    palettes = {
        "dark": ((18, 25, 29, 255), (30, 40, 44, 255)),
        "metal": ((43, 58, 63, 255), (65, 82, 87, 255)),
        "highlight": ((76, 94, 98, 255), (112, 128, 130, 255)),
        "teal": ((18, 72, 74, 255), (29, 112, 108, 255)),
        "cyan": ((72, 230, 218, 255), (184, 255, 240, 255)),
        "amber": ((214, 118, 41, 255), (255, 213, 92, 255)),
    }
    for name, (u, v) in SWATCHES.items():
        shadow, light = palettes[name]
        draw.rectangle((u, v, u + 5, v + 5), fill=shadow)
        draw.line((u, v, u + 5, v), fill=light)
        draw.line((u, v, u, v + 5), fill=light)
        draw.point((u + 4, v + 4), fill=light)
    # Spare atlas area gets ordered industrial texture, never antialiased.
    for y in range(16, HEIGHT, 4):
        for x in range((y // 4) % 2 * 2, WIDTH, 4):
            draw.point((x, y), fill=(25, 37, 40, 255))
    return image


def create_glowmask():
    image = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    for name, color in (("cyan", (122, 255, 238, 255)), ("amber", (255, 174, 60, 255))):
        u, v = SWATCHES[name]
        draw.rectangle((u, v, u + 5, v + 5), fill=color)
    return image


def create_item_icon():
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.rectangle((2, 11, 13, 14), fill=(22, 30, 34, 255))
    draw.rectangle((3, 9, 12, 11), fill=(58, 76, 81, 255))
    draw.rectangle((5, 6, 10, 10), fill=(22, 91, 91, 255))
    draw.rectangle((6, 4, 9, 8), fill=(93, 239, 224, 255))
    draw.point((7, 4), fill=(211, 255, 244, 255))
    draw.rectangle((2, 8, 4, 9), fill=(222, 132, 43, 255))
    draw.rectangle((11, 8, 13, 9), fill=(222, 132, 43, 255))
    return image


texture_image = create_texture()
buffer = io.BytesIO()
texture_image.save(buffer, format="PNG")
texture_source = "data:image/png;base64," + base64.b64encode(buffer.getvalue()).decode("ascii")

bbmodel = {
    "meta": {"format_version": "5.0", "model_format": "geckolib_model", "box_uv": False},
    "name": "RNA_Training_Anchor",
    "model_identifier": "rna_training_anchor",
    "visible_box": [1.5, 1.5, 0],
    "variable_placeholders": "",
    "multi_file_ruleset": "",
    "variable_placeholder_buttons": [],
    "timeline_setups": [],
    "unhandled_root_fields": {},
    "geckolib_modid": "riftborne",
    "geckolib_filepath_cache": {},
    "resolution": {"width": WIDTH, "height": HEIGHT},
    "elements": model.elements,
    "groups": [group_payload(name) for name in model.bones],
    "outliner": [outliner_group("root")],
    "textures": [{
        "path": str(TEXTURE),
        "name": "rna_training_anchor.png",
        "folder": "block",
        "namespace": "riftborne",
        "id": "0",
        "particle": False,
        "render_mode": "default",
        "visible": True,
        "mode": "bitmap",
        "saved": True,
        "uuid": stable_uuid("texture"),
        "relative_path": "../src/main/resources/assets/riftborne/textures/block/rna_training_anchor.png",
        "source": texture_source,
    }],
    "animations": [
        bb_animation("idle", 8.0, idle_tracks),
        bb_animation("active", 3.2, active_tracks),
    ],
    "geckolib_model_type": "Block",
}

geo_bones = []
for bone in model.bones.values():
    payload = {"name": bone["name"], "pivot": bone["pivot"]}
    if bone["parent"]:
        payload["parent"] = bone["parent"]
    if bone["geo_cubes"]:
        payload["cubes"] = bone["geo_cubes"]
    geo_bones.append(payload)

geo = {
    "format_version": "1.12.0",
    "minecraft:geometry": [{
        "description": {
            "identifier": "geometry.riftborne.rna_training_anchor",
            "texture_width": WIDTH,
            "texture_height": HEIGHT,
            "visible_bounds_width": 2.0,
            "visible_bounds_height": 2.0,
            "visible_bounds_offset": [0, 0.65, 0],
        },
        "bones": geo_bones,
    }],
}

animation = {
    "format_version": "1.8.0",
    "animations": {
        "animation.rna_training_anchor.idle": {
            "loop": True,
            "animation_length": 8.0,
            "bones": {
                "ring": {"rotation": {"0.0": [0, 0, 0], "8.0": [0, 360, 0]}},
                "core": {"position": {"0.0": [0, 0, 0], "2.0": [0, 0.18, 0], "4.0": [0, 0, 0]}},
                "glow": {"scale": {"0.0": [0.94, 0.94, 0.94], "2.0": [1.04, 1.04, 1.04], "4.0": [0.94, 0.94, 0.94]}},
            },
        },
        "animation.rna_training_anchor.active": {
            "loop": True,
            "animation_length": 3.2,
            "bones": {
                "ring": {"rotation": {"0.0": [0, 0, 0], "1.6": [0, 360, 0], "3.2": [0, 720, 0]}},
                "stabilizers": {"rotation": {"0.0": [0, 0, 0], "3.2": [0, -360, 0]}},
                "core": {"position": {"0.0": [0, 0, 0], "0.8": [0, 0.42, 0], "1.6": [0, 0, 0], "2.4": [0, 0.42, 0], "3.2": [0, 0, 0]}},
                "glow": {"scale": {"0.0": [0.9, 0.9, 0.9], "0.4": [1.12, 1.12, 1.12], "0.8": [0.9, 0.9, 0.9]}},
            },
        },
    },
}

for path in (BBMODEL, GEO, ANIMATION, TEXTURE, GLOWMASK, ITEM_TEXTURE):
    path.parent.mkdir(parents=True, exist_ok=True)

BBMODEL.write_text(json.dumps(bbmodel, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
GEO.write_text(json.dumps(geo, ensure_ascii=False, indent=2), encoding="utf-8")
ANIMATION.write_text(json.dumps(animation, ensure_ascii=False, indent=2), encoding="utf-8")
texture_image.save(TEXTURE)
create_glowmask().save(GLOWMASK)
create_item_icon().save(ITEM_TEXTURE)

print(f"Generated RNA training anchor: {len(model.elements)} cubes, {len(model.bones)} bones")
print(BBMODEL)
print(GEO)
print(ANIMATION)
print(TEXTURE)
print(GLOWMASK)
print(ITEM_TEXTURE)
