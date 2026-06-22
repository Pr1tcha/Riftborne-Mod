import base64
import io
import json
import math
import uuid
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/riftborne"

WIDTH = 128
HEIGHT = 128

CASE = (8, 8, 14, 14)
RUBBER = (24, 8, 30, 14)
METAL = (40, 8, 46, 14)
DARK = (56, 8, 62, 14)
CYAN = (72, 8, 78, 14)
SCREEN = (4, 68, 60, 124)


def stable_uuid(scope, name):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, f"riftborne:{scope}:{name}"))


class Mesh:
    def __init__(self):
        self.positions = []
        self.normals = []
        self.uvs = []
        self.polys = []

    def quad(self, points, uv_rect):
        u0, v0, u1, v1 = uv_rect
        uvs = ((u0, v1), (u1, v1), (u1, v0), (u0, v0))
        edge_a = [points[1][i] - points[0][i] for i in range(3)]
        edge_b = [points[2][i] - points[0][i] for i in range(3)]
        normal = [
            edge_a[1] * edge_b[2] - edge_a[2] * edge_b[1],
            edge_a[2] * edge_b[0] - edge_a[0] * edge_b[2],
            edge_a[0] * edge_b[1] - edge_a[1] * edge_b[0],
        ]
        length = math.sqrt(sum(value * value for value in normal)) or 1.0
        normal = [value / length for value in normal]
        normal_index = len(self.normals) // 3
        self.normals.extend(normal)
        polygon = []
        for point, uv in zip(points, uvs):
            position_index = len(self.positions) // 3
            uv_index = len(self.uvs) // 2
            self.positions.extend(round(value, 4) for value in point)
            self.uvs.extend(uv)
            polygon.append([position_index, normal_index, uv_index])
        self.polys.append(polygon)

    def chamfered_box(self, x0, x1, y0, y1, z0, z1, chamfer, uv=CASE):
        footprint = [
            (x0 + chamfer, z0), (x1 - chamfer, z0),
            (x1, z0 + chamfer), (x1, z1 - chamfer),
            (x1 - chamfer, z1), (x0 + chamfer, z1),
            (x0, z1 - chamfer), (x0, z0 + chamfer),
        ]
        bottom = [(x, y0, z) for x, z in footprint]
        top = [(x, y1, z) for x, z in footprint]
        for index in range(8):
            next_index = (index + 1) % 8
            self.quad([bottom[index], bottom[next_index], top[next_index], top[index]], uv)
        self.quad([bottom[5], bottom[4], bottom[1], bottom[0]], uv)
        self.quad([bottom[6], bottom[5], bottom[0], bottom[7]], uv)
        self.quad([bottom[4], bottom[3], bottom[2], bottom[1]], uv)
        self.quad([top[0], top[1], top[4], top[5]], uv)
        self.quad([top[7], top[0], top[5], top[6]], uv)
        self.quad([top[1], top[2], top[3], top[4]], uv)

    def payload(self):
        return {
            "normalized_uvs": False,
            "positions": self.positions,
            "normals": self.normals,
            "uvs": self.uvs,
            "polys": self.polys,
        }


def cube(origin, size, uv):
    return {"origin": origin, "size": size, "uv": list(uv[:2])}


def make_texture(name, device):
    texture_path = ASSETS / f"textures/block/{name}.png"
    glow_path = ASSETS / f"textures/block/{name}_glowmask.png"
    icon_path = ASSETS / f"textures/item/{name}.png"
    texture_path.parent.mkdir(parents=True, exist_ok=True)
    icon_path.parent.mkdir(parents=True, exist_ok=True)

    image = Image.new("RGBA", (WIDTH, HEIGHT), (29, 36, 34, 255))
    draw = ImageDraw.Draw(image)
    for rect, color in (
        (CASE, (47, 59, 55, 255)),
        (RUBBER, (16, 22, 22, 255)),
        (METAL, (111, 124, 116, 255)),
        (DARK, (5, 11, 12, 255)),
        (CYAN, (79, 225, 204, 255)),
    ):
        draw.rectangle(rect, fill=color)
    # Runtime code draws the active Pocket Codex UI into this area.
    draw.rectangle(SCREEN, fill=(2, 9, 11, 255))
    image.save(texture_path)

    glow = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    # The dynamic screen is rendered by the item renderer. Keep only physical
    # status LEDs in the static emissive mask.
    glow_draw.rectangle(CYAN, fill=(80, 255, 225, 255))
    glow.save(glow_path)

    icon = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    icon_draw = ImageDraw.Draw(icon)
    if device:
        icon_draw.rounded_rectangle((8, 2, 24, 30), radius=3, fill=(34, 44, 41, 255), outline=(93, 113, 105, 255), width=2)
        icon_draw.rectangle((10, 6, 22, 20), fill=(3, 23, 25, 255), outline=(72, 222, 201, 255))
        icon_draw.rectangle((11, 7, 21, 9), fill=(44, 164, 153, 255))
        for x in (11, 15, 19):
            icon_draw.rectangle((x, 23, x + 2, 25), fill=(75, 213, 194, 255))
        icon_draw.rectangle((13, 27, 19, 28), fill=(99, 113, 105, 255))
    else:
        icon_draw.rounded_rectangle((3, 15, 29, 27), radius=3, fill=(34, 44, 41, 255), outline=(93, 113, 105, 255), width=2)
        icon_draw.polygon(((7, 15), (10, 6), (22, 6), (25, 15)), fill=(25, 33, 31, 255), outline=(80, 99, 92, 255))
        icon_draw.rectangle((12, 9, 20, 14), fill=(3, 23, 25, 255), outline=(72, 222, 201, 255))
        icon_draw.rectangle((6, 21, 26, 23), fill=(10, 17, 17, 255))
    icon.save(icon_path)
    return texture_path, glow_path, icon_path, image


def write_bbmodel(name, model_type, bones, texture_path, texture_image):
    elements = []
    groups_by_name = {}
    for bone in bones:
        group_uuid = stable_uuid(name, f"bone:{bone['name']}")
        groups_by_name[bone["name"]] = {
            "name": bone["name"],
            "origin": bone.get("pivot", [0, 0, 0]),
            "uuid": group_uuid,
            "children": [],
            "parent": bone.get("parent"),
        }

        poly_mesh = bone.get("poly_mesh")
        if poly_mesh:
            mesh_uuid = stable_uuid(name, f"mesh:{bone['name']}")
            vertices = {}
            vertex_keys = []
            positions = poly_mesh["positions"]
            for position_index in range(len(positions) // 3):
                key = stable_uuid(name, f"mesh:{bone['name']}:vertex:{position_index}")
                offset = position_index * 3
                vertices[key] = positions[offset:offset + 3]
                vertex_keys.append(key)
            faces = {}
            uvs = poly_mesh["uvs"]
            for face_index, polygon in enumerate(poly_mesh["polys"]):
                face_key = stable_uuid(name, f"mesh:{bone['name']}:face:{face_index}")
                keys = [vertex_keys[indices[0]] for indices in polygon]
                faces[face_key] = {
                    "vertices": keys,
                    "uv": {
                        key: uvs[indices[2] * 2:indices[2] * 2 + 2]
                        for key, indices in zip(keys, polygon)
                    },
                    "texture": 0,
                }
            elements.append({
                "name": f"mesh_{bone['name']}",
                "color": len(elements) % 8,
                "origin": bone.get("pivot", [0, 0, 0]),
                "rotation": [0, 0, 0],
                "vertices": vertices,
                "faces": faces,
                "type": "mesh",
                "uuid": mesh_uuid,
            })
            groups_by_name[bone["name"]]["children"].append(mesh_uuid)

        for cube_index, geo_cube in enumerate(bone.get("cubes", [])):
            cube_uuid = stable_uuid(name, f"cube:{bone['name']}:{cube_index}")
            origin = geo_cube["origin"]
            size = geo_cube["size"]
            u, v = geo_cube.get("uv", [0, 0])
            elements.append({
                "name": f"{bone['name']}_{cube_index}",
                "box_uv": False,
                "render_order": "default",
                "from": origin,
                "to": [origin[i] + size[i] for i in range(3)],
                "origin": [origin[i] + size[i] / 2 for i in range(3)],
                "faces": {
                    direction: {"uv": [u, v, u + 3, v + 3], "texture": 0}
                    for direction in ("north", "east", "south", "west", "up", "down")
                },
                "type": "cube",
                "uuid": cube_uuid,
            })
            groups_by_name[bone["name"]]["children"].append(cube_uuid)

    def outliner_group(bone_name):
        group = groups_by_name[bone_name]
        children = list(group["children"])
        children.extend(
            outliner_group(child["name"])
            for child in bones
            if child.get("parent") == bone_name
        )
        return {
            "name": group["name"],
            "origin": group["origin"],
            "uuid": group["uuid"],
            "children": children,
        }

    groups = [
        {
            "name": group["name"],
            "origin": group["origin"],
            "uuid": group["uuid"],
            "children": list(group["children"]),
        }
        for group in groups_by_name.values()
    ]
    outliner = [
        outliner_group(bone["name"])
        for bone in bones
        if not bone.get("parent")
    ]
    buffer = io.BytesIO()
    texture_image.save(buffer, format="PNG")
    payload = {
        "meta": {"format_version": "5.0", "model_format": "geckolib_model", "box_uv": True},
        "name": name,
        "model_identifier": name,
        "resolution": {"width": WIDTH, "height": HEIGHT},
        "elements": elements,
        "groups": groups,
        "outliner": outliner,
        "textures": [{
            "path": str(texture_path),
            "name": texture_path.name,
            "folder": "block",
            "namespace": "riftborne",
            "id": "0",
            "visible": True,
            "mode": "bitmap",
            "saved": True,
            "uuid": stable_uuid(name, "texture"),
            "source": "data:image/png;base64," + base64.b64encode(buffer.getvalue()).decode("ascii"),
        }],
        "animations": [],
        "geckolib_model_type": model_type,
    }
    path = ROOT / f"bbmodels/{name}.bbmodel"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")


def build_pocket():
    shell = Mesh()
    shell.chamfered_box(-4.2, 4.2, 0.0, 0.9, -6.4, 6.4, 0.65, CASE)
    shell.chamfered_box(-4.55, -3.8, 0.25, 1.25, -5.1, 5.0, 0.2, RUBBER)
    shell.chamfered_box(3.8, 4.55, 0.25, 1.25, -5.1, 5.0, 0.2, RUBBER)
    shell.chamfered_box(-3.7, 3.7, 0.82, 1.22, -5.65, -4.9, 0.18, METAL)
    shell.chamfered_box(-3.45, 3.45, 0.88, 1.12, -4.75, 2.65, 0.32, DARK)

    screen = Mesh()
    # Keep the display artwork on one planar quad. Mapping the full screen
    # texture onto every chamfer and cap segment produced repeated cyan bands
    # and torn-looking edges around the display.
    screen.quad(
        [
            (-3.12, 1.14, -4.46),
            (-3.12, 1.14, 2.34),
            (3.12, 1.14, 2.34),
            (3.12, 1.14, -4.46),
        ],
        SCREEN,
    )

    bones = [
        {"name": "root", "pivot": [0, 0, 0]},
        {"name": "shell", "pivot": [0, 0, 0], "parent": "root", "poly_mesh": shell.payload()},
        {
            "name": "controls",
            "pivot": [0, 0, 0],
            "parent": "root",
            "cubes": [
                cube([-2.9, 0.92, 3.2], [1.4, 0.35, 1.0], DARK),
                cube([-0.7, 0.92, 3.2], [1.4, 0.35, 1.0], CYAN),
                cube([1.5, 0.92, 3.2], [1.4, 0.35, 1.0], DARK),
                cube([-0.45, 0.92, 4.75], [0.9, 0.35, 0.9], METAL),
                cube([3.25, 0.8, 4.4], [0.28, 2.5, 0.28], RUBBER),
            ],
        },
        {"name": "screen_glow", "pivot": [0, 0, 0], "parent": "root", "poly_mesh": screen.payload()},
    ]
    texture, _, _, image = make_texture("pocket_codex", True)
    write_geo("pocket_codex", bones, 1.3, 1.3)
    write_animation("pocket_codex", "screen_glow")
    write_bbmodel("Pocket_Codex", "Item", bones, texture, image)


def build_dock():
    base = Mesh()
    base.chamfered_box(-5.2, 5.2, 0.0, 1.35, -4.1, 4.1, 0.7, CASE)
    base.chamfered_box(-4.5, 4.5, 1.15, 1.7, -2.8, 2.8, 0.45, RUBBER)
    base.chamfered_box(-4.8, -3.8, 1.2, 2.8, -2.7, 2.4, 0.25, METAL)
    base.chamfered_box(3.8, 4.8, 1.2, 2.8, -2.7, 2.4, 0.25, METAL)
    base.chamfered_box(-4.1, 4.1, 1.45, 3.5, 2.15, 3.3, 0.35, CASE)

    status = Mesh()
    status.chamfered_box(-1.65, 1.65, 1.4, 1.64, -3.65, -3.05, 0.15, CYAN)

    device = Mesh()
    device.chamfered_box(-2.7, 2.7, 1.7, 2.35, -1.9, 2.8, 0.45, DARK)

    bones = [
        {"name": "root", "pivot": [0, 0, 0]},
        {"name": "dock", "pivot": [0, 0, 0], "parent": "root", "poly_mesh": base.payload()},
        {
            "name": "contacts",
            "pivot": [0, 0, 0],
            "parent": "root",
            "cubes": [
                cube([-1.8, 1.68, 1.7], [0.7, 0.2, 0.8], METAL),
                cube([-0.35, 1.68, 1.7], [0.7, 0.2, 0.8], METAL),
                cube([1.1, 1.68, 1.7], [0.7, 0.2, 0.8], METAL),
            ],
        },
        {"name": "inserted_device", "pivot": [0, 0, 0], "parent": "root", "poly_mesh": device.payload()},
        {"name": "screen_glow", "pivot": [0, 0, 0], "parent": "root", "poly_mesh": status.payload()},
    ]
    texture, _, _, image = make_texture("codex_dock", False)
    write_geo("codex_dock", bones, 1.2, 0.8)
    write_animation("codex_dock", "screen_glow")
    write_bbmodel("Codex_Dock", "Block", bones, texture, image)


def write_geo(name, bones, bounds_width, bounds_height):
    path = ASSETS / f"geo/{name}.geo.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": f"geometry.riftborne.{name}",
                "texture_width": WIDTH,
                "texture_height": HEIGHT,
                "visible_bounds_width": bounds_width,
                "visible_bounds_height": bounds_height,
                "visible_bounds_offset": [0, bounds_height / 2, 0],
            },
            "bones": bones,
        }],
    }
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def write_animation(name, glow_bone):
    path = ASSETS / f"animations/{name}.animation.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "format_version": "1.8.0",
        "animations": {
            f"animation.{name}.idle": {
                "loop": True,
                "animation_length": 2.0,
                "bones": {
                    glow_bone: {
                        "scale": {
                            "0.0": [1, 1, 1],
                            "1.0": [1.01, 1.01, 1],
                            "2.0": [1, 1, 1],
                        }
                    }
                },
            }
        },
    }
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    build_pocket()
    build_dock()
    print("Generated Pocket Codex and Codex Dock mesh assets.")
