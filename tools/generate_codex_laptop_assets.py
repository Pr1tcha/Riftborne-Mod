import base64
import io
import json
import math
import random
import uuid
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
BBMODEL = ROOT / "bbmodels" / "Codex_Field_Laptop.bbmodel"
GEO = ROOT / "src/main/resources/assets/riftborne/geo/codex_laptop.geo.json"
ANIMATION = ROOT / "src/main/resources/assets/riftborne/animations/codex_laptop.animation.json"
TEXTURE = ROOT / "src/main/resources/assets/riftborne/textures/block/codex_laptop.png"
GLOWMASK = ROOT / "src/main/resources/assets/riftborne/textures/block/codex_laptop_glowmask.png"

WIDTH = 128
HEIGHT = 128

MESH_REPLACED_CUBES = {
    "lower_chassis",
    "upper_chassis",
    "front_bevel",
    "rear_spine",
    "left_armor_rail",
    "right_armor_rail",
    "left_front_bumper",
    "left_rear_bumper",
    "right_front_bumper",
    "right_rear_bumper",
    "left_hinge",
    "right_hinge",
    "hinge_axle",
    "lid_backplate",
    "lid_top_guard",
    "lid_left_guard",
    "lid_right_guard",
    "lid_bottom_guard",
    "screen_bezel",
}


def stable_uuid(name):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, f"riftborne:codex_laptop:{name}"))


def make_faces(size, uv):
    x, y, z = [max(1, int(round(value))) for value in size]
    u, v = uv
    return {
        "north": {"uv": [u + z, v + z, u + z + x, v + z + y]},
        "east": {"uv": [u, v + z, u + z, v + z + y]},
        "south": {"uv": [u + z + x + z, v + z, u + z + x + z + x, v + z + y]},
        "west": {"uv": [u + z + x, v + z, u + z + x + z, v + z + y]},
        "up": {"uv": [u + z, v + z, u + z + x, v]},
        "down": {"uv": [u + z + x, v, u + z + x + x, v + z]},
    }


def solid_faces(u, v, size=2):
    return {
        direction: {"uv": [u, v, u + size, v + size]}
        for direction in ("north", "east", "south", "west", "up", "down")
    }


KEY_FACES = solid_faces(104, 8)
RUBBER_FACES = solid_faces(48, 8)
METAL_FACES = solid_faces(80, 8)
VENT_FACES = solid_faces(80, 48)
DARK_FACES = solid_faces(112, 48)
CYAN_FACES = solid_faces(80, 80)


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

    def cube(self, name, bone, origin, size, uv=(0, 0), inflate=0.0, rotation=None, pivot=None, face_uv=None):
        if name in MESH_REPLACED_CUBES:
            return

        cube_uuid = stable_uuid(f"cube:{name}")
        x, y, z = origin
        sx, sy, sz = size
        element = {
            "name": name,
            "box_uv": face_uv is None,
            "render_order": "default",
            "locked": False,
            "export": True,
            "scope": 0,
            "allow_mirror_modeling": True,
            "from": [x, y, z],
            "to": [x + sx, y + sy, z + sz],
            "autouv": 0,
            "color": len(self.elements) % 8,
            "origin": pivot or [x + sx / 2, y + sy / 2, z + sz / 2],
            "uv_offset": list(uv),
            "faces": face_uv or make_faces(size, uv),
            "type": "cube",
            "uuid": cube_uuid,
        }
        if rotation:
            element["rotation"] = list(rotation)
        self.elements.append(element)
        self.bones[bone]["children"].append(cube_uuid)

        geo_cube = {
            "origin": list(origin),
            "size": list(size),
            # Bedrock/GeckoLib box UVs are represented directly as [u, v].
            # An object here is interpreted as per-face UV data, leaving every
            # cube face absent when no north/east/etc. entries are present.
            "uv": (
                list(uv)
                if face_uv is None
                else {
                    direction: {
                        "uv": [face["uv"][0], face["uv"][1]],
                        "uv_size": [
                            face["uv"][2] - face["uv"][0],
                            face["uv"][3] - face["uv"][1],
                        ],
                    }
                    for direction, face in face_uv.items()
                }
            ),
        }
        if inflate:
            geo_cube["inflate"] = inflate
        if rotation:
            geo_cube["rotation"] = list(rotation)
            geo_cube["pivot"] = pivot or [x + sx / 2, y + sy / 2, z + sz / 2]
        self.bones[bone]["geo_cubes"].append(geo_cube)


model = Model()
model.bone("root", [0, 0, 0])
model.bone("base", [0, 1.5, 0], "root")
model.bone("keyboard", [0, 2.6, -0.5], "base")
model.bone("hinges", [0, 3.0, 5.25], "base")
model.bone("details", [0, 2.0, 0], "base")
model.bone("lid", [0, 3.0, 5.25], "root")
model.bone("screen_glow", [0, 3.0, 5.25], "lid")

# Armored base shell
model.cube("lower_chassis", "base", [-7.0, 0.0, -5.5], [14.0, 1.25, 11.0], (0, 0))
model.cube("upper_chassis", "base", [-6.55, 1.15, -5.05], [13.1, 1.25, 10.1], (0, 16))
model.cube("front_bevel", "base", [-6.2, 0.65, -5.8], [12.4, 1.25, 0.75], (0, 32))
model.cube("rear_spine", "base", [-6.4, 1.2, 4.75], [12.8, 1.4, 0.8], (0, 32))
model.cube("left_armor_rail", "base", [-7.35, 0.55, -4.5], [0.75, 1.75, 8.9], (32, 0))
model.cube("right_armor_rail", "base", [6.6, 0.55, -4.5], [0.75, 1.75, 8.9], (32, 0))

# Rubberized corner guards
for side, x in (("left", -7.55), ("right", 6.75)):
    for end, z in (("front", -5.75), ("rear", 4.75)):
        model.cube(f"{side}_{end}_bumper", "base", [x, 0.15, z], [0.8, 2.35, 1.0], (32, 16))

# Front latches and handle
model.cube("front_handle_bar", "details", [-2.2, 0.35, -6.05], [4.4, 0.55, 0.45], face_uv=METAL_FACES)
model.cube("front_handle_left", "details", [-2.55, 0.25, -5.95], [0.5, 0.9, 0.55], face_uv=METAL_FACES)
model.cube("front_handle_right", "details", [2.05, 0.25, -5.95], [0.5, 0.9, 0.55], face_uv=METAL_FACES)
for name, x in (("left", -5.0), ("right", 4.15)):
    model.cube(f"{name}_front_latch", "details", [x, 1.1, -5.75], [0.85, 0.8, 0.4], face_uv=METAL_FACES)

# Side ports, vents and antenna blocks
for index in range(4):
    # Push the outer face clear of the impact rail. It used to be coplanar
    # with the rail at x=-7.48, which caused the four squares to shimmer.
    model.cube(
        f"left_vent_{index}",
        "details",
        [-7.58, 1.16, -2.66 + index * 0.72],
        [0.16, 0.22, 0.48],
        face_uv=DARK_FACES,
    )
model.cube("right_port_bank", "details", [7.27, 0.95, -2.6], [0.2, 0.9, 3.2], face_uv=METAL_FACES)
model.cube("right_port_one", "details", [7.42, 1.1, -2.25], [0.12, 0.4, 0.7], face_uv=DARK_FACES)
model.cube("right_port_two", "details", [7.42, 1.1, -0.95], [0.12, 0.4, 0.7], face_uv=DARK_FACES)
model.cube("antenna_base", "details", [5.45, 2.2, 3.8], [0.8, 0.45, 0.8], face_uv=RUBBER_FACES)
model.cube("antenna_stub", "details", [5.68, 2.55, 4.0], [0.34, 1.1, 0.34], face_uv=RUBBER_FACES)

# Recessed keyboard deck. Rows use real modifier-key proportions instead of a
# uniform grid, which keeps the keyboard readable from oblique game angles.
model.cube("keyboard_well", "keyboard", [-5.72, 2.39, -3.18], [11.44, 0.18, 4.35], (32, 32))
key_rows = (
    [1.0] * 12,
    [1.35] + [1.0] * 10 + [1.55],
    [1.55] + [1.0] * 9 + [1.8],
    [1.85] + [1.0] * 8 + [2.35],
    [1.25, 1.25, 1.25, 4.6, 1.25, 1.25, 1.25],
)
key_unit = 0.72
key_gap = 0.10
key_depth = 0.57
for row, widths in enumerate(key_rows):
    row_width = sum(width * key_unit for width in widths) + key_gap * (len(widths) - 1)
    x = -row_width / 2
    z = 0.43 - row * 0.70
    for col, width_units in enumerate(widths):
        width = width_units * key_unit
        model.cube(f"key_{row}_{col}", "keyboard", [x, 2.62, z], [width, 0.16, key_depth], face_uv=KEY_FACES)
        x += width + key_gap

model.cube("trackpad", "keyboard", [-2.35, 2.48, -4.65], [4.7, 0.13, 1.25], face_uv=DARK_FACES)
model.cube("trackpad_left_click", "keyboard", [-2.35, 2.5, -4.95], [2.28, 0.15, 0.28], face_uv=DARK_FACES)
model.cube("trackpad_right_click", "keyboard", [0.07, 2.5, -4.95], [2.28, 0.15, 0.28], face_uv=DARK_FACES)
for index in range(4):
    model.cube(f"status_led_{index}", "keyboard", [1.0 + index * 0.42, 2.86, 3.32], [0.22, 0.08, 0.16], face_uv=CYAN_FACES)

# Upper service deck: speakers, telemetry strip and cable/service access break
# up the formerly empty area between keyboard and hinges.
for side, x0 in (("left", -5.45), ("right", 3.65)):
    for index in range(7):
        model.cube(
            f"{side}_upper_speaker_{index}",
            "keyboard",
            [x0 + index * 0.27, 2.84, 2.0],
            [0.14, 0.10, 1.42],
            face_uv=DARK_FACES,
        )
model.cube("telemetry_display", "keyboard", [-1.75, 2.85, 2.12], [3.5, 0.10, 0.72], face_uv=CYAN_FACES)
model.cube("telemetry_badge", "keyboard", [-1.45, 2.84, 3.05], [1.65, 0.12, 0.28], face_uv=METAL_FACES)
model.cube("service_latch", "keyboard", [-0.55, 2.83, 3.55], [1.1, 0.14, 0.34], face_uv=METAL_FACES)

# Mechanical hinges
model.cube("left_hinge", "hinges", [-5.6, 2.45, 4.7], [2.1, 1.25, 1.1], (64, 0))
model.cube("right_hinge", "hinges", [3.5, 2.45, 4.7], [2.1, 1.25, 1.1], (64, 0))
model.cube("hinge_axle", "hinges", [-3.5, 2.78, 5.0], [7.0, 0.55, 0.55], (64, 16))

# Lid: the entire assembly uses one pivot along the hinge line.
model.cube("lid_backplate", "lid", [-6.75, 3.0, 4.78], [13.5, 10.4, 0.72], (0, 48))
model.cube("lid_top_guard", "lid", [-6.95, 12.75, 4.55], [13.9, 0.85, 1.1], (32, 0))
model.cube("lid_left_guard", "lid", [-7.05, 3.0, 4.55], [0.85, 10.2, 1.1], (32, 0))
model.cube("lid_right_guard", "lid", [6.2, 3.0, 4.55], [0.85, 10.2, 1.1], (32, 0))
model.cube("lid_bottom_guard", "lid", [-6.45, 3.0, 4.55], [12.9, 1.0, 1.1], (32, 0))
screen_faces = {
    "north": {"uv": [3, 67, 60, 124]},
    "south": {"uv": [3, 67, 60, 124]},
    "east": {"uv": [32, 34, 33, 35]},
    "west": {"uv": [32, 34, 33, 35]},
    "up": {"uv": [32, 34, 33, 35]},
    "down": {"uv": [32, 34, 33, 35]},
}
model.cube(
    "screen_panel",
    "screen_glow",
    [-5.32, 4.52, 4.43],
    [10.64, 7.04, 0.12],
    face_uv=screen_faces,
)
model.cube("camera", "screen_glow", [-0.28, 12.25, 4.30], [0.56, 0.34, 0.18], face_uv=CYAN_FACES)
model.cube("screen_status_left", "screen_glow", [-5.75, 3.55, 4.32], [0.28, 0.28, 0.16], face_uv=CYAN_FACES)
model.cube("screen_status_right", "screen_glow", [5.47, 3.55, 4.32], [0.28, 0.28, 0.16], face_uv=CYAN_FACES)
model.cube("codex_badge", "lid", [-1.65, 3.35, 4.32], [3.3, 0.42, 0.16], face_uv=METAL_FACES)

# Exterior ribs improve the closed silhouette.
rib_centers = (-5.25, -2.625, 0.0, 2.625, 5.25)
for index, center_x in enumerate(rib_centers):
    model.cube(
        f"lid_back_rib_{index}",
        "lid",
        [center_x - 0.24, 3.35, 5.70],
        [0.48, 9.72, 0.22],
        face_uv=RUBBER_FACES,
    )


def group_payload(name):
    bone = model.bones[name]
    return {
        "name": name,
        "uuid": bone["uuid"],
        "export": True,
        "locked": False,
        "scope": 0,
        "selected": False,
        "_static": {"properties": {}, "temp_data": {}},
        "origin": bone["pivot"],
        "rotation": [90, 0, 0] if name == "lid" else [0, 0, 0],
        "color": 0,
        "children": list(bone["children"]),
        "reset": False,
        "shade": True,
        "mirror_uv": False,
        "visibility": True,
        "autouv": 0,
        "isOpen": True,
        "primary_selected": False,
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


animations_bb = []
for animation_name, start, end, length in (
    ("open", 0, -100, 0.72),
    ("close", -100, 0, 0.62),
):
    animations_bb.append({
        "uuid": stable_uuid(f"animation:{animation_name}"),
        "name": animation_name,
        "loop": "hold",
        "override": False,
        "length": length,
        "snapping": 24,
        "selected": animation_name == "open",
        "saved": True,
        "path": "",
        "scope": 0,
        "anim_time_update": "",
        "blend_weight": "",
        "start_delay": "",
        "loop_delay": "",
        "animators": {
            model.bones["lid"]["uuid"]: {
                "name": "lid",
                "type": "bone",
                "rotation_global": False,
                "quaternion_interpolation": False,
                "keyframes": [
                    {
                        "channel": "rotation",
                        "data_points": [{"x": str(start), "y": "0", "z": "0"}],
                        "uuid": stable_uuid(f"{animation_name}:start"),
                        "time": 0,
                        "color": -1,
                        "interpolation": "linear",
                    },
                    {
                        "channel": "rotation",
                        "data_points": [{"x": str(end), "y": "0", "z": "0"}],
                        "uuid": stable_uuid(f"{animation_name}:end"),
                        "time": length,
                        "color": -1,
                        "interpolation": "linear",
                    },
                ],
            }
        },
    })

animations_bb.append({
    "uuid": stable_uuid("animation:opened"),
    "name": "opened",
    "loop": "hold",
    "override": False,
    "length": 0.05,
    "snapping": 24,
    "selected": False,
    "saved": True,
    "path": "",
    "scope": 0,
    "anim_time_update": "",
    "blend_weight": "",
    "start_delay": "",
    "loop_delay": "",
    "animators": {
        model.bones["lid"]["uuid"]: {
            "name": "lid",
            "type": "bone",
            "rotation_global": False,
            "quaternion_interpolation": False,
            "keyframes": [{
                "channel": "rotation",
                "data_points": [{"x": "-100", "y": "0", "z": "0"}],
                "uuid": stable_uuid("opened:0"),
                "time": 0,
                "color": -1,
                "interpolation": "linear",
            }],
        }
    },
})

animations_bb.append({
    "uuid": stable_uuid("animation:closed"),
    "name": "closed",
    "loop": "hold",
    "override": False,
    "length": 0.05,
    "snapping": 24,
    "selected": False,
    "saved": True,
    "path": "",
    "scope": 0,
    "anim_time_update": "",
    "blend_weight": "",
    "start_delay": "",
    "loop_delay": "",
    "animators": {
        model.bones["lid"]["uuid"]: {
            "name": "lid",
            "type": "bone",
            "rotation_global": False,
            "quaternion_interpolation": False,
            "keyframes": [{
                "channel": "rotation",
                "data_points": [{"x": "0", "y": "0", "z": "0"}],
                "uuid": stable_uuid("closed:0"),
                "time": 0,
                "color": -1,
                "interpolation": "linear",
            }],
        }
    },
})

animations_bb.append({
    "uuid": stable_uuid("animation:working"),
    "name": "working",
    "loop": "loop",
    "override": False,
    "length": 2.0,
    "snapping": 24,
    "selected": False,
    "saved": True,
    "path": "",
    "scope": 0,
    "anim_time_update": "",
    "blend_weight": "",
    "start_delay": "",
    "loop_delay": "",
    "animators": {
        model.bones["screen_glow"]["uuid"]: {
            "name": "screen_glow",
            "type": "bone",
            "rotation_global": False,
            "quaternion_interpolation": False,
            "keyframes": [
                {
                    "channel": "scale",
                    "data_points": [{"x": "1", "y": "1", "z": "1"}],
                    "uuid": stable_uuid("working:0"),
                    "time": 0,
                    "color": -1,
                    "interpolation": "linear",
                },
                {
                    "channel": "scale",
                    "data_points": [{"x": "1.003", "y": "1.003", "z": "1"}],
                    "uuid": stable_uuid("working:1"),
                    "time": 1,
                    "color": -1,
                    "interpolation": "linear",
                },
                {
                    "channel": "scale",
                    "data_points": [{"x": "1", "y": "1", "z": "1"}],
                    "uuid": stable_uuid("working:2"),
                    "time": 2,
                    "color": -1,
                    "interpolation": "linear",
                },
            ],
        }
    },
})


def create_texture():
    random.seed(8241)
    image = Image.new("RGBA", (WIDTH, HEIGHT), (24, 29, 31, 255))
    draw = ImageDraw.Draw(image)

    regions = [
        ((0, 0, 31, 31), (65, 72, 70, 255)),       # painted case
        ((32, 0, 63, 31), (24, 28, 28, 255)),      # rubber
        ((64, 0, 95, 31), (103, 108, 101, 255)),   # exposed metal
        ((96, 0, 127, 31), (23, 29, 30, 255)),     # keys
        ((0, 32, 31, 63), (45, 54, 54, 255)),      # dark case
        ((32, 32, 63, 63), (19, 29, 32, 255)),     # recesses
        ((64, 32, 95, 63), (73, 82, 79, 255)),     # vents
        ((96, 32, 127, 63), (8, 12, 13, 255)),     # input details
        ((0, 64, 63, 127), (8, 30, 34, 255)),      # screen
        ((64, 64, 95, 95), (65, 225, 211, 255)),   # emissive cyan
        ((96, 64, 127, 95), (78, 88, 83, 255)),    # labels
        ((64, 96, 127, 127), (25, 31, 31, 255)),
    ]
    for rect, color in regions:
        draw.rectangle(rect, fill=color)

    # Large, low-frequency material gradients remain stable under mipmapping.
    # Fine random scratches previously became fingerprint-like moire on meshes.
    for y in range(2, 30):
        shade = 63 + int(8 * (1.0 - y / 31.0))
        draw.line((2, y, 29, y), fill=(shade, shade + 6, shade + 5, 255))
    for y in range(2, 30):
        shade = 23 + int(4 * y / 31.0)
        draw.line((34, y, 61, y), fill=(shade, shade + 3, shade + 3, 255))
    for y in range(2, 30):
        shade = 98 + int(10 * (1.0 - y / 31.0))
        draw.line((66, y, 93, y), fill=(shade, shade + 4, shade + 1, 255))
    for y in range(34, 62):
        shade = 42 + int(6 * (1.0 - (y - 34) / 28.0))
        draw.line((2, y, 29, y), fill=(shade, shade + 7, shade + 7, 255))
    draw.rectangle((2, 2, 29, 29), outline=(82, 90, 86, 255))
    draw.rectangle((34, 2, 61, 29), outline=(36, 41, 40, 255))
    draw.rectangle((66, 2, 93, 29), outline=(132, 135, 124, 255))
    draw.rectangle((2, 34, 29, 61), outline=(59, 69, 68, 255))

    # Padded solid swatches for tiny and thin geometry. Their UVs point at the
    # central 2x2 area while the surrounding pixels prevent mipmap bleeding.
    swatches = (
        ((104, 8), (25, 31, 32, 255)),   # keycaps
        ((48, 8), (27, 32, 31, 255)),    # rubber ribs and antenna
        ((80, 8), (112, 116, 107, 255)), # exposed metal
        ((80, 48), (55, 64, 62, 255)),   # vents
        ((112, 48), (10, 15, 16, 255)),  # recesses and ports
        ((80, 80), (65, 225, 211, 255)), # emissive details
    )
    for (u, v), color in swatches:
        draw.rectangle((u - 2, v - 2, u + 3, v + 3), fill=color)

    # Screen graphics: restrained RIFT/OS telemetry.
    draw.rectangle((3, 67, 60, 124), fill=(4, 18, 22, 255))
    draw.rectangle((5, 69, 58, 72), fill=(39, 151, 148, 255))
    draw.rectangle((5, 75, 35, 77), fill=(76, 231, 217, 255))
    for row in range(5):
        length = (37, 48, 29, 44, 33)[row]
        draw.rectangle((6, 82 + row * 7, 6 + length, 83 + row * 7), fill=(24, 102 + row * 7, 105, 255))
    draw.rectangle((43, 82, 56, 106), outline=(52, 203, 193, 255), width=1)
    draw.line((44, 101, 47, 95, 50, 98, 54, 87, 56, 91), fill=(76, 231, 217, 255), width=1)
    draw.rectangle((6, 119, 57, 121), fill=(18, 72, 76, 255))

    # Keys stay intentionally clean. Single-pixel legends shimmer badly when
    # dozens of tiny keycaps are minified at normal gameplay distance.
    for x, y, length in ((69, 7, 7), (78, 14, 9), (84, 23, 5), (72, 27, 4)):
        draw.line((x, y, x + length, y), fill=(112, 117, 108, 255))

    image.save(TEXTURE)

    glow = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    glow_draw.rectangle((3, 67, 60, 124), fill=(24, 209, 196, 220))
    glow_draw.rectangle((78, 78, 83, 83), fill=(89, 255, 235, 255))
    glow.save(GLOWMASK)
    return image


texture_image = create_texture()
buffer = io.BytesIO()
texture_image.save(buffer, format="PNG")
texture_source = "data:image/png;base64," + base64.b64encode(buffer.getvalue()).decode("ascii")

bbmodel = {
    "meta": {"format_version": "5.0", "model_format": "geckolib_model", "box_uv": True},
    "name": "Codex_Field_Laptop",
    "model_identifier": "codex_laptop",
    "visible_box": [1.1, 1.0, 0],
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
        "name": "codex_laptop.png",
        "folder": "block",
        "namespace": "riftborne",
        "id": "0",
        "particle": False,
        "render_mode": "default",
        "visible": True,
        "mode": "bitmap",
        "saved": True,
        "uuid": stable_uuid("texture"),
        "relative_path": "../src/main/resources/assets/riftborne/textures/block/codex_laptop.png",
        "source": texture_source,
    }],
    "animations": animations_bb,
    "geckolib_model_type": "Block",
}

geo_bones = []
for bone in model.bones.values():
    payload = {"name": bone["name"], "pivot": bone["pivot"]}
    if bone["name"] == "lid":
        payload["rotation"] = [90, 0, 0]
    if bone["parent"]:
        payload["parent"] = bone["parent"]
    if bone["geo_cubes"]:
        payload["cubes"] = bone["geo_cubes"]
    geo_bones.append(payload)

geo = {
    "format_version": "1.12.0",
    "minecraft:geometry": [{
        "description": {
            "identifier": "geometry.riftborne.codex_laptop",
            "texture_width": WIDTH,
            "texture_height": HEIGHT,
            "visible_bounds_width": 2.0,
            "visible_bounds_height": 1.5,
            "visible_bounds_offset": [0, 0.5, 0],
        },
        "bones": geo_bones,
    }],
}

animation = {
    "format_version": "1.8.0",
    "animations": {
        "animation.codex_laptop.open": {
            "loop": "hold_on_last_frame",
            "animation_length": 0.72,
            "bones": {
                "lid": {
                    "rotation": {
                        "0.0": [0, 0, 0],
                        "0.32": [-42, 0, 0],
                        "0.72": [-100, 0, 0],
                    }
                }
            },
        },
        "animation.codex_laptop.close": {
            "loop": "hold_on_last_frame",
            "animation_length": 0.62,
            "bones": {
                "lid": {
                    "rotation": {
                        "0.0": [-100, 0, 0],
                        "0.28": [-48, 0, 0],
                        "0.62": [0, 0, 0],
                    }
                }
            },
        },
        "animation.codex_laptop.opened": {
            "loop": "hold_on_last_frame",
            "animation_length": 0.05,
            "bones": {
                "lid": {
                    "rotation": {
                        "0.0": [-100, 0, 0],
                    }
                }
            },
        },
        "animation.codex_laptop.closed": {
            "loop": "hold_on_last_frame",
            "animation_length": 0.05,
            "bones": {
                "lid": {
                    "rotation": {
                        "0.0": [0, 0, 0],
                    }
                }
            },
        },
        "animation.codex_laptop.working": {
            "loop": True,
            "animation_length": 2.0,
            "bones": {
                "screen_glow": {
                    "scale": {
                        "0.0": [1, 1, 1],
                        "1.0": [1.003, 1.003, 1],
                        "2.0": [1, 1, 1],
                    }
                }
            },
        },
    },
}

for path in (BBMODEL, GEO, ANIMATION, TEXTURE, GLOWMASK):
    path.parent.mkdir(parents=True, exist_ok=True)

BBMODEL.write_text(json.dumps(bbmodel, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
GEO.write_text(json.dumps(geo, ensure_ascii=False, indent=2), encoding="utf-8")
ANIMATION.write_text(json.dumps(animation, ensure_ascii=False, indent=2), encoding="utf-8")

print(f"Generated {len(model.elements)} cubes")
print(BBMODEL)
print(GEO)
print(ANIMATION)
print(TEXTURE)
print(GLOWMASK)
