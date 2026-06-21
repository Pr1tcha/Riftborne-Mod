"""Export Blockbench mesh elements from a .bbmodel into GeckoLib poly_mesh bones.

The stock GeckoLib Blockbench codec currently exports cube geometry only. This bridge keeps
the normal GeckoLib geo file and injects explicit Bedrock poly_mesh arrays for every bone that
contains Blockbench Mesh elements.
"""

from __future__ import annotations

import argparse
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
    for group in project.get("groups", []):
        bone_name = group["name"]
        for child in group.get("children", []):
            if isinstance(child, str):
                mapping[child] = bone_name
    return mapping


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


def export(bbmodel_path: Path, geo_path: Path):
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


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("bbmodel", type=Path)
    parser.add_argument("geo", type=Path)
    arguments = parser.parse_args()
    export(arguments.bbmodel, arguments.geo)


if __name__ == "__main__":
    main()
