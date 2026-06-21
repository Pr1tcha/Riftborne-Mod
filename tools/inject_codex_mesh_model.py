"""Build the production mesh shell for the Codex field laptop.

Large silhouette-defining parts use real chamfered meshes. Small controls remain
ordinary GeckoLib cubes because they are cheaper and look better at Minecraft scale.
"""

from __future__ import annotations

import json
import math
import uuid
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PROJECT = ROOT / "bbmodels" / "Codex_Field_Laptop.bbmodel"
PREFIX = "mesh_"
REPLACED_CUBES = {
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

# Mesh faces sample compact, low-variance material swatches. Mapping every
# bevel across a whole 27px material tile produced visible triangular gradients
# and moire when the model was minified.
UV_CASE = (8, 8, 11, 11)
UV_RUBBER = (40, 8, 43, 11)
UV_METAL = (72, 8, 75, 11)
UV_DARK = (8, 40, 11, 43)


def stable_uuid(name):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, f"riftborne:codex_laptop:mesh:{name}"))


def find_outliner_group(node, group_uuid):
    if isinstance(node, dict):
        if node.get("uuid") == group_uuid:
            return node
        for child in node.get("children", []):
            found = find_outliner_group(child, group_uuid)
            if found:
                return found
    elif isinstance(node, list):
        for child in node:
            found = find_outliner_group(child, group_uuid)
            if found:
                return found
    return None


class Mesh:
    def __init__(self, name, bone, color=0):
        self.name = PREFIX + name
        self.bone = bone
        self.color = color
        self.vertices = {}
        self.faces = {}

    def vertex(self, position):
        key = stable_uuid(f"{self.name}:vertex:{len(self.vertices)}")
        self.vertices[key] = [round(value, 4) for value in position]
        return key

    def face(self, keys, uv_rect):
        u0, v0, u1, v1 = uv_rect
        if len(keys) == 3:
            coords = ((u0, v1), (u1, v1), (u0, v0))
        elif len(keys) == 4:
            coords = ((u0, v1), (u1, v1), (u1, v0), (u0, v0))
        else:
            coords = []
            for index in range(len(keys)):
                angle = math.tau * index / len(keys) - math.pi / 2
                coords.append((
                    (u0 + u1) / 2 + math.cos(angle) * (u1 - u0) / 2,
                    (v0 + v1) / 2 + math.sin(angle) * (v1 - v0) / 2,
                ))
        face_key = stable_uuid(f"{self.name}:face:{len(self.faces)}")
        self.faces[face_key] = {
            "vertices": keys,
            "uv": {key: [round(uv[0], 3), round(uv[1], 3)] for key, uv in zip(keys, coords)},
            "texture": 0,
        }

    def payload(self):
        center = [sum(values[index] for values in self.vertices.values()) / len(self.vertices) for index in range(3)]
        return {
            "name": self.name,
            "color": self.color,
            "origin": [round(value, 4) for value in center],
            "rotation": [0, 0, 0],
            "vertices": self.vertices,
            "faces": self.faces,
            "type": "mesh",
            "uuid": stable_uuid(self.name),
        }


def chamfered_prism_xz(name, bone, x0, x1, y0, y1, z0, z1, chamfer, uv=UV_CASE, color=0):
    mesh = Mesh(name, bone, color)
    footprint = [
        (x0 + chamfer, z0), (x1 - chamfer, z0),
        (x1, z0 + chamfer), (x1, z1 - chamfer),
        (x1 - chamfer, z1), (x0 + chamfer, z1),
        (x0, z1 - chamfer), (x0, z0 + chamfer),
    ]
    bottom = [mesh.vertex((x, y0, z)) for x, z in footprint]
    top = [mesh.vertex((x, y1, z)) for x, z in footprint]
    for index in range(8):
        next_index = (index + 1) % 8
        mesh.face([bottom[index], bottom[next_index], top[next_index], top[index]], uv)
    # Three quads cover each cap without long fan triangles crossing the whole
    # panel. This keeps normals and texture derivatives stable.
    mesh.face([bottom[5], bottom[4], bottom[1], bottom[0]], uv)
    mesh.face([bottom[6], bottom[5], bottom[0], bottom[7]], uv)
    mesh.face([bottom[4], bottom[3], bottom[2], bottom[1]], uv)
    mesh.face([top[0], top[1], top[4], top[5]], uv)
    mesh.face([top[7], top[0], top[5], top[6]], uv)
    mesh.face([top[1], top[2], top[3], top[4]], uv)
    return mesh


def chamfered_prism_xy(name, bone, x0, x1, y0, y1, z0, z1, chamfer, uv=UV_CASE, color=0):
    mesh = Mesh(name, bone, color)
    footprint = [
        (x0 + chamfer, y0), (x1 - chamfer, y0),
        (x1, y0 + chamfer), (x1, y1 - chamfer),
        (x1 - chamfer, y1), (x0 + chamfer, y1),
        (x0, y1 - chamfer), (x0, y0 + chamfer),
    ]
    back = [mesh.vertex((x, y, z1)) for x, y in footprint]
    front = [mesh.vertex((x, y, z0)) for x, y in footprint]
    for index in range(8):
        next_index = (index + 1) % 8
        mesh.face([back[index], back[next_index], front[next_index], front[index]], uv)
    mesh.face([back[5], back[4], back[1], back[0]], uv)
    mesh.face([back[6], back[5], back[0], back[7]], uv)
    mesh.face([back[4], back[3], back[2], back[1]], uv)
    mesh.face([front[0], front[1], front[4], front[5]], uv)
    mesh.face([front[7], front[0], front[5], front[6]], uv)
    mesh.face([front[1], front[2], front[3], front[4]], uv)
    return mesh


def chamfered_frame_xy(
        name, bone,
        outer_x0, outer_x1, outer_y0, outer_y1,
        inner_x0, inner_x1, inner_y0, inner_y1,
        z0, z1, outer_chamfer, inner_chamfer,
        uv=UV_DARK, color=0):
    mesh = Mesh(name, bone, color)

    def loop(x0, x1, y0, y1, chamfer, z):
        points = [
            (x0 + chamfer, y0), (x1 - chamfer, y0),
            (x1, y0 + chamfer), (x1, y1 - chamfer),
            (x1 - chamfer, y1), (x0 + chamfer, y1),
            (x0, y1 - chamfer), (x0, y0 + chamfer),
        ]
        return [mesh.vertex((x, y, z)) for x, y in points]

    outer_front = loop(outer_x0, outer_x1, outer_y0, outer_y1, outer_chamfer, z0)
    outer_back = loop(outer_x0, outer_x1, outer_y0, outer_y1, outer_chamfer, z1)
    inner_front = loop(inner_x0, inner_x1, inner_y0, inner_y1, inner_chamfer, z0)
    inner_back = loop(inner_x0, inner_x1, inner_y0, inner_y1, inner_chamfer, z1)

    for index in range(8):
        next_index = (index + 1) % 8
        mesh.face([outer_front[index], outer_front[next_index], outer_back[next_index], outer_back[index]], uv)
        mesh.face([inner_front[next_index], inner_front[index], inner_back[index], inner_back[next_index]], uv)
        mesh.face([outer_front[index], inner_front[index], inner_front[next_index], outer_front[next_index]], uv)
        mesh.face([outer_back[next_index], inner_back[next_index], inner_back[index], outer_back[index]], uv)
    return mesh


def cylinder_x(name, bone, x0, x1, center_y, center_z, radius, segments=10, uv=UV_METAL, color=0):
    mesh = Mesh(name, bone, color)
    left = []
    right = []
    for index in range(segments):
        angle = math.tau * index / segments
        y = center_y + math.sin(angle) * radius
        z = center_z + math.cos(angle) * radius
        left.append(mesh.vertex((x0, y, z)))
        right.append(mesh.vertex((x1, y, z)))
    for index in range(segments):
        next_index = (index + 1) % segments
        mesh.face([left[index], left[next_index], right[next_index], right[index]], uv)
    # End caps are hidden inside the hinge mounts; omitting them avoids
    # unnecessary high-valence polygons and their minification artefacts.
    return mesh


def build_meshes():
    meshes = [
        # Base: layered magnesium shell and replaceable rubber impact rails.
        chamfered_prism_xz("base_lower_shell", "base", -7.0, 7.0, 0.0, 1.22, -5.5, 5.5, 0.72, UV_CASE, 0),
        chamfered_prism_xz("base_upper_deck", "base", -6.55, 6.55, 1.1, 2.38, -5.05, 5.05, 0.5, UV_DARK, 1),
        chamfered_prism_xz("base_front_lip", "base", -6.35, 6.35, 0.55, 1.72, -5.86, -5.05, 0.28, UV_METAL, 2),
        chamfered_prism_xz("base_rear_spine", "base", -6.45, 6.45, 1.15, 2.58, 4.72, 5.58, 0.25, UV_METAL, 2),
        chamfered_prism_xz("left_impact_rail", "base", -7.48, -6.58, 0.38, 2.5, -4.62, 4.62, 0.34, UV_RUBBER, 3),
        chamfered_prism_xz("right_impact_rail", "base", 6.58, 7.48, 0.38, 2.5, -4.62, 4.62, 0.34, UV_RUBBER, 3),
        # Raised equipment deck fills the dead zone above the keyboard and
        # creates a stepped silhouette leading into the hinge assembly.
        chamfered_prism_xz("rear_service_deck", "base", -5.9, 5.9, 2.34, 2.68, 1.28, 4.46, 0.28, UV_DARK, 1),
        chamfered_prism_xz("left_speaker_housing", "base", -5.55, -3.28, 2.64, 2.82, 1.62, 3.78, 0.18, UV_RUBBER, 3),
        chamfered_prism_xz("right_speaker_housing", "base", 3.28, 5.55, 2.64, 2.82, 1.62, 3.78, 0.18, UV_RUBBER, 3),
        chamfered_prism_xz("center_telemetry_housing", "base", -2.65, 2.65, 2.64, 2.84, 1.72, 3.82, 0.20, UV_CASE, 0),
    ]
    for side, x0, x1 in (("left", -7.68, -6.45), ("right", 6.45, 7.68)):
        for end, z0, z1 in (("front", -5.92, -4.55), ("rear", 4.55, 5.92)):
            meshes.append(chamfered_prism_xz(
                f"{side}_{end}_corner_guard", "base",
                x0, x1, 0.08, 2.72, z0, z1, 0.42, UV_RUBBER, 3
            ))

    # Lid: bevelled structural panel with a thicker shock frame.
    meshes.extend([
        chamfered_prism_xy("lid_structural_panel", "lid", -6.78, 6.78, 3.0, 13.42, 4.75, 5.52, 0.68, UV_CASE, 0),
        chamfered_prism_xy("lid_top_impact_bar", "lid", -7.08, 7.08, 12.55, 13.72, 4.48, 5.66, 0.4, UV_RUBBER, 3),
        chamfered_prism_xy("lid_left_impact_bar", "lid", -7.18, -6.12, 3.18, 13.25, 4.48, 5.66, 0.36, UV_RUBBER, 3),
        chamfered_prism_xy("lid_right_impact_bar", "lid", 6.12, 7.18, 3.18, 13.25, 4.48, 5.66, 0.36, UV_RUBBER, 3),
        chamfered_prism_xy("lid_bottom_impact_bar", "lid", -6.55, 6.55, 2.88, 4.12, 4.48, 5.66, 0.38, UV_RUBBER, 3),
        chamfered_frame_xy(
            "screen_recess_bezel", "lid",
            -5.92, 5.92, 3.92, 12.22,
            -5.48, 5.48, 4.36, 11.78,
            4.30, 4.70, 0.38, 0.22, UV_DARK, 1
        ),
    ])

    # Proper round hinge barrels instead of rectangular blocks.
    meshes.extend([
        cylinder_x("left_hinge_barrel", "hinges", -5.7, -3.45, 3.0, 5.16, 0.55, 10, UV_METAL, 2),
        cylinder_x("right_hinge_barrel", "hinges", 3.45, 5.7, 3.0, 5.16, 0.55, 10, UV_METAL, 2),
        cylinder_x("hinge_center_axle", "hinges", -3.45, 3.45, 3.0, 5.16, 0.3, 10, UV_RUBBER, 3),
    ])
    return meshes


project = json.loads(PROJECT.read_text(encoding="utf-8"))
previous_mesh_uuids = {
    element["uuid"]
    for element in project.get("elements", [])
    if element.get("type") == "mesh" and element.get("name", "").startswith(PREFIX)
}
replaced_cube_uuids = {
    element["uuid"]
    for element in project.get("elements", [])
    if element.get("type") == "cube" and element.get("name") in REPLACED_CUBES
}
removed_uuids = previous_mesh_uuids | replaced_cube_uuids
project["elements"] = [
    element for element in project["elements"]
    if not (
        (element.get("type") == "mesh" and element.get("name", "").startswith(PREFIX))
        or (element.get("type") == "cube" and element.get("name") in REPLACED_CUBES)
    )
]

groups = {group["name"]: group for group in project["groups"]}
for group in groups.values():
    group["children"] = [
        child for child in group.get("children", [])
        if not isinstance(child, str) or child not in removed_uuids
    ]

# Remove all previous mesh UUIDs from the outliner before inserting the new set.
for group in groups.values():
    outliner_group = find_outliner_group(project["outliner"], group["uuid"])
    if outliner_group:
        outliner_group["children"] = [
            child for child in outliner_group.get("children", [])
            if not isinstance(child, str) or child not in removed_uuids
        ]

meshes = build_meshes()
for mesh in meshes:
    payload = mesh.payload()
    project["elements"].append(payload)
    groups[mesh.bone]["children"].append(payload["uuid"])
    outliner_group = find_outliner_group(project["outliner"], groups[mesh.bone]["uuid"])
    if outliner_group is None:
        raise RuntimeError(f"Could not locate {mesh.bone!r} in Blockbench outliner")
    outliner_group["children"].append(payload["uuid"])

PROJECT.write_text(json.dumps(project, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
print(f"Injected {len(meshes)} production mesh elements")
