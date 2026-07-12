"""Export Blockbench mesh elements from a .bbmodel into GeckoLib poly_mesh bones.

The stock GeckoLib Blockbench codec currently exports cube geometry only. This bridge keeps
the normal GeckoLib geo file and injects explicit Bedrock poly_mesh arrays for every bone that
contains Blockbench Mesh elements.
"""

from __future__ import annotations

import argparse
import base64
import json
import math
from pathlib import Path


def vector_sub(a, b):
    return [a[0] - b[0], a[1] - b[1], a[2] - b[2]]


def vector_cross(a, b):
    return [
        a[1] * b[2] - a[2] * b[1],
        a[2] * b[0] - a[0] * b[2],
        a[0] * b[1] - a[1] * b[0],
    ]


def normalize(vector):
    length = math.sqrt(sum(component * component for component in vector))
    if length < 1.0e-8:
        return [0.0, 1.0, 0.0]
    return [component / length for component in vector]


def collect_element_bones(project):
    mapping = {}

    groups_by_uuid = {
        group["uuid"]: group
        for group in project.get("groups", [])
    }

    def visit(nodes, parent_bone=None):
        for node in nodes:
            if isinstance(node, str):
                if parent_bone is not None:
                    mapping[node] = parent_bone
                continue

            group = groups_by_uuid.get(node.get("uuid"))
            bone_name = group["name"] if group is not None else parent_bone
            visit(node.get("children", []), bone_name)

    # Current Blockbench versions keep the authoritative hierarchy in outliner.
    # Older generated projects may still store element UUIDs directly on groups.
    visit(project.get("outliner", []))
    for group in project.get("groups", []):
        bone_name = group["name"]
        for child in group.get("children", []):
            if isinstance(child, str):
                mapping.setdefault(child, bone_name)
    return mapping


def export_texture(project, texture_path: Path):
    textures = project.get("textures", [])
    if not textures:
        raise ValueError("Blockbench project has no embedded texture")

    source = textures[0].get("source", "")
    marker = ";base64,"
    if marker not in source:
        raise ValueError("Blockbench texture is not embedded as base64 data")

    texture_path.parent.mkdir(parents=True, exist_ok=True)
    texture_path.write_bytes(base64.b64decode(source.split(marker, 1)[1]))
    print(f"Exported embedded texture to {texture_path}")


def export_display(project, item_model_path: Path, compensate_geckolib_half_turn: bool = False):
    supported_contexts = {
        "thirdperson_righthand",
        "thirdperson_lefthand",
        "firstperson_righthand",
        "firstperson_lefthand",
        "ground",
        "gui",
        "head",
        "fixed",
    }
    display = {}
    for context, source_transform in project.get("display", {}).items():
        if context not in supported_contexts:
            continue

        transform = {
            key: list(value) if isinstance(value, list) else value
            for key, value in source_transform.items()
        }
        if compensate_geckolib_half_turn and context in {
            "thirdperson_righthand",
            "thirdperson_lefthand",
            "firstperson_righthand",
            "firstperson_lefthand",
            "ground",
        }:
            rotation = transform.get("rotation")
            if rotation and abs(rotation[0]) == 180:
                rotation[0] = 0
        display[context] = transform
    item_model = {
        "parent": "minecraft:builtin/entity",
        "display": display,
    }
    item_model_path.parent.mkdir(parents=True, exist_ok=True)
    item_model_path.write_text(
        json.dumps(item_model, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"Exported {len(display)} display context(s) to {item_model_path}")


def triangulate(vertex_keys):
    if len(vertex_keys) <= 4:
        return [vertex_keys]
    return [
        [vertex_keys[0], vertex_keys[index], vertex_keys[index + 1]]
        for index in range(1, len(vertex_keys) - 1)
    ]


def build_poly_mesh(meshes):
    positions = []
    normals = []
    uvs = []
    polygons = []

    for mesh in meshes:
        mesh_vertices = mesh.get("vertices", {})
        for face in mesh.get("faces", {}).values():
            face_keys = face.get("vertices", [])
            face_uv = face.get("uv", {})
            for polygon_keys in triangulate(face_keys):
                points = [mesh_vertices[key] for key in polygon_keys]
                normal = normalize(vector_cross(
                    vector_sub(points[1], points[0]),
                    vector_sub(points[2], points[0]),
                ))
                normal_index = len(normals) // 3
                normals.extend(normal)

                polygon = []
                for key in polygon_keys:
                    position_index = len(positions) // 3
                    uv_index = len(uvs) // 2
                    positions.extend(mesh_vertices[key])
                    uvs.extend(face_uv.get(key, [0, 0]))
                    polygon.append([position_index, normal_index, uv_index])
                polygons.append(polygon)

    return {
        "normalized_uvs": False,
        "positions": positions,
        "normals": normals,
        "uvs": uvs,
        "polys": polygons,
    }


def export(
    bbmodel_path: Path,
    geo_path: Path,
    texture_path: Path | None = None,
    item_model_path: Path | None = None,
    compensate_geckolib_half_turn: bool = False,
):
    project = json.loads(bbmodel_path.read_text(encoding="utf-8"))
    geo = json.loads(geo_path.read_text(encoding="utf-8"))
    element_bones = collect_element_bones(project)
    meshes_by_bone = {}

    for element in project.get("elements", []):
        if element.get("type") != "mesh":
            continue
        bone_name = element_bones.get(element["uuid"])
        if bone_name is None:
            raise ValueError(f"Mesh {element.get('name', element['uuid'])} is not assigned to a bone")
        meshes_by_bone.setdefault(bone_name, []).append(element)

    runtime_bones = {
        bone["name"]: bone
        for bone in geo["minecraft:geometry"][0]["bones"]
    }
    for bone in runtime_bones.values():
        bone.pop("poly_mesh", None)

    for bone_name, meshes in meshes_by_bone.items():
        if bone_name not in runtime_bones:
            raise ValueError(f"Runtime geo has no bone named {bone_name!r}")
        runtime_bones[bone_name]["poly_mesh"] = build_poly_mesh(meshes)

    geo_path.write_text(json.dumps(geo, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Exported {sum(map(len, meshes_by_bone.values()))} mesh element(s) into {len(meshes_by_bone)} bone(s)")

    if texture_path is not None:
        export_texture(project, texture_path)
    if item_model_path is not None:
        export_display(project, item_model_path, compensate_geckolib_half_turn)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("bbmodel", type=Path)
    parser.add_argument("geo", type=Path)
    parser.add_argument("--texture", type=Path)
    parser.add_argument("--item-model", type=Path)
    parser.add_argument(
        "--compensate-geckolib-half-turn",
        action="store_true",
        help="Remove Blockbench's redundant X half-turn from held-item display contexts",
    )
    arguments = parser.parse_args()
    export(
        arguments.bbmodel,
        arguments.geo,
        texture_path=arguments.texture,
        item_model_path=arguments.item_model,
        compensate_geckolib_half_turn=arguments.compensate_geckolib_half_turn,
    )


if __name__ == "__main__":
    main()
