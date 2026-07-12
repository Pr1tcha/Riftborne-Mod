from __future__ import annotations

import base64
import io
import json
import math
import uuid
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src" / "main" / "resources" / "assets" / "riftborne"
ICON_REFERENCE = ROOT / "tools" / "reference" / "riftwalker_item_icons_reference.png"


def stable_uuid(scope, name):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, f"riftborne:{scope}:{name}"))


def cube(origin, size, uv, inflate=0.0, rotation=None, pivot=None):
    result = {"origin": origin, "size": size, "uv": uv}
    if inflate:
        result["inflate"] = inflate
    if rotation:
        result["rotation"] = rotation
        result["pivot"] = pivot
    return result


def face(region):
    u0, v0, u1, v1 = region
    return {"uv": [u0, v0], "uv_size": [u1 - u0, v1 - v0]}


def face_cube(origin, size, north_uv, side_uv=None, inflate=0.0, rotation=None, pivot=None):
    if side_uv is None:
        side_uv = north_uv
    result = {
        "origin": origin,
        "size": size,
        "uv": {
            "north": face(north_uv),
            "south": face(side_uv),
            "east": face(side_uv),
            "west": face(side_uv),
            "up": face(side_uv),
            "down": face(side_uv),
        },
    }
    if inflate:
        result["inflate"] = inflate
    if rotation:
        result["rotation"] = rotation
        result["pivot"] = pivot
    return result


def bone(name, pivot, cubes=None, parent=None, mesh=None):
    result = {"name": name, "pivot": pivot}
    if parent:
        result["parent"] = parent
    if cubes:
        result["cubes"] = cubes
    if mesh:
        result["poly_mesh"] = mesh.payload()
    return result


class PolyMesh:
    def __init__(self):
        self.positions = []
        self.normals = []
        self.uvs = []
        self.polys = []

    def add_vertex(self, position, uv):
        position_index = len(self.positions) // 3
        uv_index = len(self.uvs) // 2
        self.positions.extend(round(value, 4) for value in position)
        self.uvs.extend(round(value, 3) for value in uv)
        return position_index, uv_index

    def add_face(self, points, uv_rect):
        u0, v0, u1, v1 = uv_rect
        if len(points) == 3:
            face_uvs = [(u0, v1), (u1, v1), ((u0 + u1) / 2, v0)]
        else:
            face_uvs = [(u0, v1), (u1, v1), (u1, v0), (u0, v0)]
        normal_index = len(self.normals) // 3
        self.normals.extend(face_normal(points))
        polygon = []
        for point, uv in zip(points, face_uvs):
            position_index, uv_index = self.add_vertex(point, uv)
            polygon.append([position_index, normal_index, uv_index])
        self.polys.append(polygon)

    def add_polygon_panel_xy(self, outline, z_front, z_back, uv=(150, 8, 166, 24)):
        front = [(x, y, z_front) for x, y in outline]
        back = [(x, y, z_back) for x, y in outline]
        count = len(outline)
        for index in range(count):
            next_index = (index + 1) % count
            self.add_face([back[index], back[next_index], front[next_index], front[index]], uv)
        self.add_cap(front, reverse=False, uv=uv)
        self.add_cap(back, reverse=True, uv=uv)

    def add_hex_panel_xy(self, x0, x1, y0, y1, z_front, z_back, cut=0.35, uv=(130, 24, 146, 40)):
        outline = [
            (x0 + cut, y0),
            (x1 - cut, y0),
            (x1, y0 + cut),
            (x1, y1 - cut),
            (x1 - cut, y1),
            (x0 + cut, y1),
            (x0, y1 - cut),
            (x0, y0 + cut),
        ]
        self.add_polygon_panel_xy(outline, z_front, z_back, uv)

    def add_polyline_panel_xy(self, points, width, z_front, z_back=None, uv=(240, 0, 248, 8)):
        self.add_tapered_polyline_panel_xy(points, [width] * len(points), z_front, z_back, uv)

    def add_tapered_polyline_panel_xy(self, points, widths, z_front, z_back=None, uv=(240, 0, 248, 8)):
        left = []
        right = []

        for index, (x, y) in enumerate(points):
            half_width = widths[index] / 2
            if index == 0:
                dx = points[1][0] - x
                dy = points[1][1] - y
                length = math.hypot(dx, dy)
                nx, ny = -dy / length, dx / length
                scale = half_width
            elif index == len(points) - 1:
                dx = x - points[index - 1][0]
                dy = y - points[index - 1][1]
                length = math.hypot(dx, dy)
                nx, ny = -dy / length, dx / length
                scale = half_width
            else:
                prev_dx = x - points[index - 1][0]
                prev_dy = y - points[index - 1][1]
                next_dx = points[index + 1][0] - x
                next_dy = points[index + 1][1] - y
                prev_len = math.hypot(prev_dx, prev_dy)
                next_len = math.hypot(next_dx, next_dy)
                prev_nx, prev_ny = -prev_dy / prev_len, prev_dx / prev_len
                next_nx, next_ny = -next_dy / next_len, next_dx / next_len
                nx, ny = prev_nx + next_nx, prev_ny + next_ny
                normal_len = math.hypot(nx, ny)
                if normal_len < 1.0e-6:
                    nx, ny = next_nx, next_ny
                    scale = half_width
                else:
                    nx, ny = nx / normal_len, ny / normal_len
                    dot = max(0.45, abs(nx * next_nx + ny * next_ny))
                    scale = min(half_width / dot, half_width * 1.65)

            left.append((x + nx * scale, y + ny * scale))
            right.append((x - nx * scale, y - ny * scale))

        front = [(x, y, z_front) for x, y in left + list(reversed(right))]
        self.add_cap(front, reverse=False, uv=uv)

    def add_cap(self, points, reverse, uv):
        for index in range(1, len(points) - 1):
            triangle = [points[0], points[index], points[index + 1]]
            if reverse:
                triangle = list(reversed(triangle))
            self.add_face(triangle, uv)

    def payload(self):
        return {
            "normalized_uvs": False,
            "positions": self.positions,
            "normals": self.normals,
            "uvs": self.uvs,
            "polys": self.polys,
        }


def face_normal(points):
    ax, ay, az = [points[1][index] - points[0][index] for index in range(3)]
    bx, by, bz = [points[2][index] - points[0][index] for index in range(3)]
    nx = ay * bz - az * by
    ny = az * bx - ax * bz
    nz = ax * by - ay * bx
    length = math.sqrt(nx * nx + ny * ny + nz * nz)
    if length < 1.0e-8:
        return [0.0, 0.0, -1.0]
    return [nx / length, ny / length, nz / length]


def build_geometry():
    white = (240, 0, 248, 8)
    helmet_shell = (176, 0, 192, 16)
    helmet_top = (232, 16, 248, 32)
    helmet_face = (150, 8, 166, 24)
    helmet_seam = (168, 16, 176, 24)
    helmet_panel = (176, 16, 184, 24)
    helmet_trim = (184, 16, 192, 24)
    helmet_back = (192, 48, 208, 64)
    strap = (104, 132, 120, 140)
    buckle = (120, 132, 128, 140)

    chest_mesh = PolyMesh()
    chest_mesh.add_hex_panel_xy(-1.45, 1.45, 16.78, 18.34, -3.22, -3.02, 0.3, (150, 24, 166, 40))
    back_mesh = PolyMesh()
    back_mesh.add_hex_panel_xy(-1.42, 1.42, 16.16, 17.74, 3.02, 3.22, 0.3, (150, 24, 166, 40))

    right_knee_mesh = PolyMesh()
    right_knee_mesh.add_hex_panel_xy(-3.35, -0.85, 4.7, 7.0, -3.08, -2.84, 0.34, (130, 40, 146, 56))
    left_knee_mesh = PolyMesh()
    left_knee_mesh.add_hex_panel_xy(0.85, 3.35, 4.7, 7.0, -3.08, -2.84, 0.34, (130, 40, 146, 56))
    left_eye_mesh = PolyMesh()
    left_eye_mesh.add_tapered_polyline_panel_xy(
        [(-2.66, 30.04), (-2.58, 29.36), (-2.42, 28.66)],
        [0.13, 0.15, 0.14],
        -4.28,
        None,
        white,
    )
    left_eye_mesh.add_tapered_polyline_panel_xy(
        [(-2.42, 28.66), (-2.34, 28.22)],
        [0.14, 0.17],
        -4.28,
        None,
        white,
    )
    left_eye_mesh.add_tapered_polyline_panel_xy(
        [(-2.34, 28.22), (-2.08, 27.42), (-1.58, 26.82), (-1.1, 26.32)],
        [0.17, 0.18, 0.18, 0.15],
        -4.28,
        None,
        white,
    )
    left_eye_mesh.add_tapered_polyline_panel_xy(
        [(-1.1, 26.32), (-0.92, 25.38)],
        [0.15, 0.15],
        -4.28,
        None,
        white,
    )
    left_eye_mesh.add_tapered_polyline_panel_xy(
        [(-0.92, 25.38), (-0.78, 24.72)],
        [0.15, 0.13],
        -4.28,
        None,
        white,
    )
    right_eye_mesh = PolyMesh()
    right_eye_mesh.add_tapered_polyline_panel_xy(
        [(2.66, 30.04), (2.58, 29.36), (2.42, 28.66)],
        [0.13, 0.15, 0.14],
        -4.28,
        None,
        white,
    )
    right_eye_mesh.add_tapered_polyline_panel_xy(
        [(2.42, 28.66), (2.34, 28.22)],
        [0.14, 0.17],
        -4.28,
        None,
        white,
    )
    right_eye_mesh.add_tapered_polyline_panel_xy(
        [(2.34, 28.22), (2.08, 27.42), (1.58, 26.82), (1.1, 26.32)],
        [0.17, 0.18, 0.18, 0.15],
        -4.28,
        None,
        white,
    )
    right_eye_mesh.add_tapered_polyline_panel_xy(
        [(1.1, 26.32), (0.92, 25.38)],
        [0.15, 0.15],
        -4.28,
        None,
        white,
    )
    right_eye_mesh.add_tapered_polyline_panel_xy(
        [(0.92, 25.38), (0.78, 24.72)],
        [0.15, 0.13],
        -4.28,
        None,
        white,
    )

    bones = [
        bone("armorHead", [0, 24, 0]),
        bone("helmet_base", [0, 24, 0], [
            face_cube([-4.02, 23.76, -4.08], [8.04, 8.08, 8.16], helmet_shell),
            face_cube([-3.52, 23.68, -4.34], [7.04, 7.66, 0.3], helmet_face),
            face_cube([-4.22, 24.52, -3.72], [0.42, 6.7, 7.44], helmet_shell),
            face_cube([3.8, 24.52, -3.72], [0.42, 6.7, 7.44], helmet_shell),
            face_cube([-3.56, 23.54, -3.62], [7.12, 0.58, 7.24], helmet_top),
            face_cube([-3.62, 31.62, -3.9], [7.24, 0.56, 7.8], helmet_face),
            face_cube([-3.54, 24.0, 3.96], [7.08, 7.52, 0.3], helmet_back),
        ], parent="armorHead"),
        bone("helmet_left_panel", [0, 24, 0], [
            face_cube([-4.06, 24.75, -3.24], [0.42, 6.25, 6.08], helmet_shell),
            face_cube([-4.22, 25.15, -3.18], [0.18, 5.45, 0.34], helmet_trim),
            face_cube([-4.22, 25.15, 2.36], [0.18, 5.45, 0.34], helmet_trim),
            face_cube([-4.28, 29.4, -2.72], [0.18, 0.56, 1.48], helmet_panel),
            face_cube([-4.28, 27.35, -1.46], [0.18, 0.56, 1.72], helmet_panel),
            face_cube([-4.28, 25.45, 0.02], [0.18, 0.56, 1.48], helmet_panel),
            face_cube([-4.32, 28.55, -2.92], [0.14, 0.92, 0.36], helmet_trim),
            face_cube([-4.32, 26.45, 2.28], [0.14, 0.92, 0.36], helmet_trim),
        ], parent="armorHead"),
        bone("helmet_right_panel", [0, 24, 0], [
            face_cube([3.64, 24.75, -3.24], [0.42, 6.25, 6.08], helmet_shell),
            face_cube([4.04, 25.15, -3.18], [0.18, 5.45, 0.34], helmet_trim),
            face_cube([4.04, 25.15, 2.36], [0.18, 5.45, 0.34], helmet_trim),
            face_cube([4.1, 29.4, -2.72], [0.18, 0.56, 1.48], helmet_panel),
            face_cube([4.1, 27.35, -1.46], [0.18, 0.56, 1.72], helmet_panel),
            face_cube([4.1, 25.45, 0.02], [0.18, 0.56, 1.48], helmet_panel),
            face_cube([4.18, 28.55, -2.92], [0.14, 0.92, 0.36], helmet_trim),
            face_cube([4.18, 26.45, 2.28], [0.14, 0.92, 0.36], helmet_trim),
        ], parent="armorHead"),
        bone("helmet_jaw", [0, 24, 0], [
            face_cube([-2.38, 23.5, -4.2], [4.76, 0.34, 0.1], helmet_trim),
            face_cube([-0.62, 23.1, -4.21], [1.24, 0.48, 0.11], helmet_trim),
            face_cube([-2.94, 23.86, -4.2], [0.5, 0.48, 0.1], helmet_panel),
            face_cube([2.44, 23.86, -4.2], [0.5, 0.48, 0.1], helmet_panel),
        ], parent="armorHead"),
        bone("helmet_face_seams", [0, 24, 0], [
            face_cube([-1.88, 29.18, -4.15], [0.13, 1.28, 0.05], helmet_seam, rotation=[0, 0, -6], pivot=[-1.82, 29.84, -4.12]),
            face_cube([1.75, 29.18, -4.15], [0.13, 1.28, 0.05], helmet_seam, rotation=[0, 0, 6], pivot=[1.82, 29.84, -4.12]),
            face_cube([-1.48, 28.24, -4.16], [0.13, 1.22, 0.05], helmet_seam, rotation=[0, 0, -28], pivot=[-1.42, 28.84, -4.12]),
            face_cube([1.35, 28.24, -4.16], [0.13, 1.22, 0.05], helmet_seam, rotation=[0, 0, 28], pivot=[1.42, 28.84, -4.12]),
            face_cube([-0.72, 27.72, -4.16], [1.44, 0.12, 0.05], helmet_seam),
            face_cube([-2.48, 25.22, -4.16], [0.1, 2.08, 0.05], helmet_seam, rotation=[0, 0, -22], pivot=[-2.42, 26.22, -4.12]),
            face_cube([2.38, 25.22, -4.16], [0.1, 2.08, 0.05], helmet_seam, rotation=[0, 0, 22], pivot=[2.42, 26.22, -4.12]),
            face_cube([-1.72, 24.16, -4.16], [0.1, 1.4, 0.05], helmet_seam, rotation=[0, 0, -12], pivot=[-1.66, 24.84, -4.12]),
            face_cube([1.62, 24.16, -4.16], [0.1, 1.4, 0.05], helmet_seam, rotation=[0, 0, 12], pivot=[1.66, 24.84, -4.12]),
            face_cube([-0.42, 23.7, -4.17], [0.84, 0.1, 0.06], helmet_panel),
        ], parent="armorHead"),
        bone("eye_indicator_left", [0, 24, 0], parent="armorHead", mesh=left_eye_mesh),
        bone("eye_indicator_right", [0, 24, 0], parent="armorHead", mesh=right_eye_mesh),
        bone("armorBody", [0, 24, 0], [
            cube([-4.04, 12.25, -2.38], [8.08, 10.78, 4.76], [0, 82], 0.03),
        ]),
        bone("chest_armor", [0, 24, 0], [
            cube([-3.58, 20.72, -2.86], [7.16, 0.48, 0.34], [84, 82]),
            cube([-3.88, 18.78, -2.9], [0.5, 2.7, 0.34], [68, 82], rotation=[0, 0, -9], pivot=[-3.58, 20.12, -2.68]),
            cube([3.38, 18.78, -2.9], [0.5, 2.7, 0.34], [68, 82], rotation=[0, 0, 9], pivot=[3.58, 20.12, -2.68]),
            cube([-3.6, 13.32, -2.78], [0.54, 5.65, 0.3], [176, 16], rotation=[0, 0, -5], pivot=[-3.36, 16.05, -2.62]),
            cube([3.06, 13.32, -2.78], [0.54, 5.65, 0.3], [176, 16], rotation=[0, 0, 5], pivot=[3.36, 16.05, -2.62]),
            cube([-2.8, 18.12, -3.1], [1.42, 0.4, 0.32], strap, rotation=[0, 0, -22], pivot=[-2.1, 18.55, -2.88]),
            cube([1.38, 18.12, -3.1], [1.42, 0.4, 0.32], strap, rotation=[0, 0, 22], pivot=[2.1, 18.55, -2.88]),
            cube([-1.34, 17.98, -3.28], [2.68, 0.46, 0.28], [120, 132]),
            cube([-1.02, 17.28, -3.34], [2.04, 0.38, 0.26], [120, 132]),
            cube([-3.18, 12.78, -2.72], [6.36, 0.4, 0.3], [84, 82]),
        ], parent="armorBody", mesh=chest_mesh),
        bone("back_armor", [0, 24, 0], [
            cube([-3.58, 20.54, 2.52], [7.16, 0.48, 0.34], [84, 82]),
            cube([-3.54, 13.32, 2.56], [0.54, 5.62, 0.3], [176, 16], rotation=[0, 0, 6], pivot=[-3.32, 16.05, 2.72]),
            cube([3.0, 13.32, 2.56], [0.54, 5.62, 0.3], [176, 16], rotation=[0, 0, -6], pivot=[3.32, 16.05, 2.72]),
            cube([-2.98, 17.78, 2.62], [0.5, 2.82, 0.3], strap, rotation=[0, 0, 14], pivot=[-2.62, 19.12, 2.82]),
            cube([2.48, 17.78, 2.62], [0.5, 2.82, 0.3], strap, rotation=[0, 0, -14], pivot=[2.62, 19.12, 2.82]),
            cube([-1.36, 17.78, 3.08], [2.72, 0.44, 0.3], [120, 132]),
            cube([-3.18, 12.78, 2.58], [6.36, 0.4, 0.3], [84, 82]),
        ], parent="armorBody", mesh=back_mesh),
        bone("belt", [0, 24, 0], [
            cube([-4.06, 11.92, -2.58], [8.12, 0.42, 5.16], [84, 82]),
            cube([-0.68, 11.84, -2.9], [1.36, 0.56, 0.36], [120, 132]),
            cube([-3.25, 11.84, -2.82], [0.7, 0.54, 0.3], [104, 132]),
            cube([2.55, 11.84, -2.82], [0.7, 0.54, 0.3], [104, 132]),
        ], parent="armorBody"),

        bone("armorRightArm", [-5, 22, 0], [
            cube([-7.42, 10.95, -1.82], [2.92, 12.02, 3.64], [0, 132], 0.0),
        ]),
        bone("shoulder_right", [-5, 22, 0], [
            cube([-7.9, 21.08, -2.18], [3.62, 1.88, 4.36], [0, 132], 0.015),
            cube([-8.02, 21.62, -2.36], [3.88, 0.34, 4.72], [24, 132]),
        ], parent="armorRightArm"),
        bone("upper_arm_right", [-5, 22, 0], [
            cube([-7.58, 15.9, -1.98], [3.18, 4.86, 3.96], [36, 132], 0.015),
            cube([-7.76, 18.42, -2.32], [3.52, 0.3, 0.34], strap),
            cube([-7.76, 16.58, -2.32], [3.52, 0.3, 0.34], strap),
            cube([-7.9, 18.25, -0.38], [0.3, 0.62, 0.76], buckle),
        ], parent="armorRightArm"),
        bone("forearm_right", [-5, 22, 0], [
            cube([-7.7, 11.12, -2.04], [3.28, 4.92, 4.08], [60, 132], 0.015),
            cube([-7.9, 14.2, -2.4], [3.68, 0.32, 0.36], strap),
            cube([-7.9, 12.42, -2.4], [3.68, 0.32, 0.36], strap),
            cube([-7.98, 13.98, -0.4], [0.3, 0.62, 0.8], buckle),
        ], parent="armorRightArm"),
        bone("glove_right", [-5, 22, 0], [
            cube([-7.66, 10.18, -2.0], [3.24, 1.08, 4.0], [84, 132], 0.015),
        ], parent="armorRightArm"),

        bone("armorLeftArm", [5, 22, 0], [
            cube([4.5, 10.95, -1.82], [2.92, 12.02, 3.64], [0, 154], 0.0),
        ]),
        bone("shoulder_left", [5, 22, 0], [
            cube([4.28, 21.08, -2.18], [3.62, 1.88, 4.36], [0, 154], 0.015),
            cube([4.14, 21.62, -2.36], [3.88, 0.34, 4.72], [24, 154]),
        ], parent="armorLeftArm"),
        bone("upper_arm_left", [5, 22, 0], [
            cube([4.4, 15.9, -1.98], [3.18, 4.86, 3.96], [36, 154], 0.015),
            cube([4.24, 18.42, -2.32], [3.52, 0.3, 0.34], strap),
            cube([4.24, 16.58, -2.32], [3.52, 0.3, 0.34], strap),
            cube([7.6, 18.25, -0.38], [0.3, 0.62, 0.76], buckle),
        ], parent="armorLeftArm"),
        bone("forearm_left", [5, 22, 0], [
            cube([4.42, 11.12, -2.04], [3.28, 4.92, 4.08], [60, 154], 0.015),
            cube([4.22, 14.2, -2.4], [3.68, 0.32, 0.36], strap),
            cube([4.22, 12.42, -2.4], [3.68, 0.32, 0.36], strap),
            cube([7.68, 13.98, -0.4], [0.3, 0.62, 0.8], buckle),
        ], parent="armorLeftArm"),
        bone("glove_left", [5, 22, 0], [
            cube([4.42, 10.18, -2.0], [3.24, 1.08, 4.0], [84, 154], 0.015),
        ], parent="armorLeftArm"),

        bone("armorRightLeg", [-1.9, 12, 0]),
        bone("thigh_right", [-1.9, 12, 0], [
            cube([-3.9, 6.85, -2.08], [3.62, 5.45, 4.16], [0, 206], 0.015),
            cube([-4.08, 8.8, -2.48], [3.94, 0.38, 0.46], strap),
            cube([-4.08, 7.05, 1.96], [3.94, 0.36, 0.42], strap),
        ], parent="armorRightLeg"),
        bone("knee_right", [-1.9, 12, 0], [
            cube([-3.36, 5.24, -2.56], [2.54, 1.25, 0.16], [104, 206]),
        ], parent="armorRightLeg", mesh=right_knee_mesh),
        bone("shin_right", [-1.9, 12, 0], [
            cube([-3.86, 0.55, -2.1], [3.56, 5.05, 4.2], [36, 206], 0.015),
            cube([-4.04, 3.3, -2.52], [3.9, 0.36, 0.46], strap),
            cube([-4.04, 1.62, -2.52], [3.9, 0.36, 0.46], strap),
        ], parent="armorRightLeg"),

        bone("armorLeftLeg", [1.9, 12, 0]),
        bone("thigh_left", [1.9, 12, 0], [
            cube([0.28, 6.85, -2.08], [3.62, 5.45, 4.16], [0, 228], 0.015),
            cube([0.14, 8.8, -2.48], [3.94, 0.38, 0.46], strap),
            cube([0.14, 7.05, 1.96], [3.94, 0.36, 0.42], strap),
        ], parent="armorLeftLeg"),
        bone("knee_left", [1.9, 12, 0], [
            cube([0.82, 5.24, -2.56], [2.54, 1.25, 0.16], [104, 228]),
        ], parent="armorLeftLeg", mesh=left_knee_mesh),
        bone("shin_left", [1.9, 12, 0], [
            cube([0.3, 0.55, -2.1], [3.56, 5.05, 4.2], [36, 228], 0.015),
            cube([0.14, 3.3, -2.52], [3.9, 0.36, 0.46], strap),
            cube([0.14, 1.62, -2.52], [3.9, 0.36, 0.46], strap),
        ], parent="armorLeftLeg"),

        bone("armorRightBoot", [-1.9, 12, 0]),
        bone("boot_right", [-1.9, 12, 0], [
            cube([-4.08, -0.2, -2.28], [4.18, 4.45, 4.56], [0, 242], 0.02),
            cube([-4.28, -0.48, -3.18], [4.58, 1.7, 5.52], [32, 242]),
            cube([-3.62, 1.35, -3.32], [3.24, 0.3, 0.38], strap),
            cube([-3.62, 2.2, -3.32], [3.24, 0.3, 0.38], strap),
        ], parent="armorRightBoot"),
        bone("sole_right", [-1.9, 12, 0], [
            cube([-4.34, -0.88, -3.28], [4.72, 0.72, 5.82], [72, 242]),
        ], parent="armorRightBoot"),
        bone("armorLeftBoot", [1.9, 12, 0]),
        bone("boot_left", [1.9, 12, 0], [
            cube([-0.1, -0.2, -2.28], [4.18, 4.45, 4.56], [0, 242], 0.02),
            cube([-0.3, -0.48, -3.18], [4.58, 1.7, 5.52], [32, 242]),
            cube([0.38, 1.35, -3.32], [3.24, 0.3, 0.38], strap),
            cube([0.38, 2.2, -3.32], [3.24, 0.3, 0.38], strap),
        ], parent="armorLeftBoot"),
        bone("sole_left", [1.9, 12, 0], [
            cube([-0.38, -0.88, -3.28], [4.72, 0.72, 5.82], [72, 242]),
        ], parent="armorLeftBoot"),
    ]

    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.riftwalker_armor",
                "texture_width": 256,
                "texture_height": 256,
                "visible_bounds_width": 2.25,
                "visible_bounds_height": 3.4,
                "visible_bounds_offset": [0, 1.35, 0],
            },
            "bones": bones,
        }],
    }


def material_noise(draw, bounds, base, edge, seed):
    draw.rectangle(bounds, fill=base)


def bevel_box(draw, box, fill, top, side, shadow):
    x0, y0, x1, y1 = box
    draw.rectangle(box, fill=fill)
    draw.line((x0, y0, x1, y0), fill=top)
    draw.line((x0, y0, x0, y1), fill=side)
    draw.line((x0, y1, x1, y1), fill=shadow)
    draw.line((x1, y0, x1, y1), fill=shadow)


def build_texture():
    image = Image.new("RGBA", (256, 256), (8, 8, 10, 255))
    draw = ImageDraw.Draw(image)

    black = (5, 6, 8, 255)
    cloth = (13, 14, 17, 255)
    coat = (18, 19, 23, 255)
    plate = (30, 33, 38, 255)
    edge = (52, 56, 64, 255)
    worn = (70, 73, 80, 255)

    material_noise(draw, (0, 0, 126, 76), coat, (31, 33, 39, 255), 1)
    material_noise(draw, (0, 82, 124, 124), cloth, (30, 32, 38, 255), 2)
    material_noise(draw, (126, 82, 178, 124), coat, (34, 36, 42, 255), 3)
    material_noise(draw, (0, 132, 128, 176), cloth, (31, 33, 39, 255), 4)
    material_noise(draw, (0, 184, 178, 238), coat, (35, 37, 43, 255), 5)
    material_noise(draw, (0, 242, 118, 255), (14, 15, 18, 255), (36, 39, 46, 255), 6)

    for box in [
        (34, 0, 64, 18), (150, 8, 166, 24),
        (36, 82, 68, 104), (68, 82, 84, 104),
        (130, 24, 146, 40), (150, 24, 166, 40),
        (130, 40, 146, 56), (104, 206, 124, 222), (104, 228, 124, 244),
        (32, 242, 70, 255), (72, 242, 110, 255),
    ]:
        bevel_box(draw, box, plate, worn, edge, black)

    # Clean chest/back armor swatches. These UV islands sit on large visible
    # plates, so noisy fabric lines look like z-fighting in-game.
    for box, fill in [
        ((0, 82, 34, 124), (16, 18, 22, 255)),
        ((36, 82, 68, 104), (27, 31, 37, 255)),
        ((68, 82, 84, 104), (18, 21, 26, 255)),
        ((84, 82, 124, 96), (21, 24, 29, 255)),
        ((130, 24, 146, 40), (31, 35, 42, 255)),
        ((150, 24, 166, 40), (18, 20, 25, 255)),
        ((130, 40, 146, 56), (24, 27, 33, 255)),
    ]:
        bevel_box(draw, box, fill, (58, 64, 74, 255), (37, 42, 50, 255), (5, 6, 8, 255))

    # Clean arm armor and strap islands too; these were the main source of the
    # visible speckled bands on sleeves and buckles.
    draw.rectangle((0, 132, 128, 176), fill=(8, 9, 12, 255))
    for box, fill in [
        ((0, 132, 24, 148), (10, 11, 14, 255)),
        ((24, 132, 34, 148), (16, 18, 22, 255)),
        ((36, 132, 56, 148), (21, 24, 30, 255)),
        ((60, 132, 80, 148), (20, 23, 28, 255)),
        ((84, 132, 100, 148), (11, 12, 15, 255)),
        ((104, 132, 120, 140), (9, 10, 12, 255)),
        ((120, 132, 128, 140), (32, 36, 43, 255)),
        ((0, 154, 24, 170), (10, 11, 14, 255)),
        ((24, 154, 34, 170), (16, 18, 22, 255)),
        ((36, 154, 56, 170), (21, 24, 30, 255)),
        ((60, 154, 80, 170), (20, 23, 28, 255)),
        ((84, 154, 100, 170), (11, 12, 15, 255)),
    ]:
        bevel_box(draw, box, fill, (45, 50, 59, 255), (28, 32, 38, 255), (2, 3, 5, 255))

    def helmet_swatch(box, base, top, side, low):
        x0, y0, x1, y1 = box
        draw.rectangle(box, fill=base)
        draw.line((x0, y0, x1, y0), fill=top)
        draw.line((x0, y0 + 1, x1, y0 + 1), fill=side)
        draw.line((x0, y1, x1, y1), fill=low)
        draw.line((x1, y0, x1, y1), fill=low)

    # Riftwalker graphite palette: still black armor, but not an unreadable
    # absolute-black block in daylight. The face remains the darkest continuous
    # plate; top, sides and trim carry the values for Minecraft readability.
    helmet_swatch((176, 0, 192, 16), (10, 12, 15, 255), (24, 29, 34, 255), (15, 18, 22, 255), (3, 4, 6, 255))
    helmet_swatch((232, 16, 248, 32), (18, 22, 26, 255), (49, 56, 64, 255), (28, 33, 39, 255), (5, 6, 8, 255))
    helmet_swatch((150, 8, 166, 24), (9, 11, 14, 255), (27, 32, 38, 255), (15, 18, 22, 255), (2, 3, 5, 255))
    helmet_swatch((168, 16, 176, 24), (16, 19, 23, 255), (31, 36, 42, 255), (21, 25, 30, 255), (4, 5, 7, 255))
    helmet_swatch((176, 16, 184, 24), (20, 24, 29, 255), (42, 48, 55, 255), (27, 32, 38, 255), (5, 6, 8, 255))
    helmet_swatch((184, 16, 192, 24), (25, 30, 36, 255), (56, 63, 72, 255), (34, 39, 46, 255), (7, 8, 11, 255))
    helmet_swatch((192, 48, 208, 64), (37, 42, 48, 255), (73, 80, 90, 255), (50, 57, 65, 255), (12, 14, 18, 255))

    for box in [(104, 132, 120, 140), (120, 132, 128, 140), (84, 82, 124, 96)]:
        bevel_box(draw, box, (21, 22, 26, 255), edge, (34, 36, 42, 255), black)

    # Keep the old coat seam pass disabled while the armor fit is being tuned:
    # broad texture lines on moving limbs read as z-fighting in-game.

    # Pure white swatch used by the physical eye cubes and the glow layer.
    draw.rectangle((240, 0, 248, 8), fill=(250, 253, 255, 255))

    # A tiny atlas preview of the mask helps item icons and Blockbench viewport sampling.
    draw.rectangle((192, 0, 232, 42), fill=(2, 3, 4, 255))
    draw.polygon([(198, 5), (226, 5), (224, 31), (216, 38), (208, 38), (200, 31)], fill=(8, 9, 12, 255))
    draw.line((203, 11, 209, 18, 211, 28), fill=(250, 253, 255, 255), width=2)
    draw.line((221, 11, 215, 18, 213, 28), fill=(250, 253, 255, 255), width=2)
    draw.rectangle((192, 16, 200, 24), fill=(13, 15, 20, 255))
    draw.line((193, 16, 199, 16), fill=(64, 68, 76, 255))
    draw.line((192, 17, 192, 23), fill=(34, 37, 44, 255))

    # The preview above touches its right edge inclusively in Pillow, so stamp
    # the helmet roof swatch again last with the same readable graphite values.
    helmet_swatch((232, 16, 248, 32), (18, 22, 26, 255), (49, 56, 64, 255), (28, 33, 39, 255), (5, 6, 8, 255))

    return image


def build_glowmask():
    image = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.rectangle((240, 0, 248, 8), fill=(255, 255, 255, 255))
    draw.line((203, 11, 209, 18, 211, 28), fill=(255, 255, 255, 230), width=3)
    draw.line((221, 11, 215, 18, 213, 28), fill=(255, 255, 255, 230), width=3)
    return image


def icon(piece):
    image = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    outline = (0, 1, 2, 255)
    black = (4, 5, 7, 255)
    dark = (10, 11, 14, 255)
    mid = (21, 23, 27, 255)
    plate = (35, 37, 42, 255)
    edge = (62, 66, 74, 255)
    metal = (118, 122, 128, 255)
    white = (250, 253, 255, 255)

    def box(x0, y0, x1, y1, fill=dark, hi=True):
        draw.rectangle((x0, y0, x1, y1), fill=outline)
        if x1 - x0 > 2 and y1 - y0 > 2:
            draw.rectangle((x0 + 1, y0 + 1, x1 - 1, y1 - 1), fill=fill)
        if hi and x1 - x0 > 3:
            draw.line((x0 + 1, y0 + 1, x1 - 2, y0 + 1), fill=plate)
            draw.line((x0 + 1, y1 - 1, x1 - 1, y1 - 1), fill=(1, 2, 3, 255))

    def strap_line(x0, y0, x1, y1):
        draw.line((x0, y0, x1, y1), fill=edge)
        draw.line((x0, y0 + 1, x1, y1 + 1), fill=outline)

    if piece == "hood":
        draw.polygon(
            [(6, 13), (8, 9), (11, 6), (21, 6), (24, 9), (26, 13), (27, 22), (23, 27), (9, 27), (5, 22)],
            fill=outline,
        )
        draw.polygon(
            [(8, 14), (10, 10), (12, 8), (20, 8), (22, 10), (24, 14), (25, 21), (22, 25), (10, 25), (7, 21)],
            fill=dark,
        )
        draw.rectangle((11, 7, 21, 10), fill=plate)
        draw.rectangle((9, 11, 12, 20), fill=mid)
        draw.rectangle((20, 11, 23, 20), fill=mid)
        draw.polygon([(12, 12), (20, 12), (20, 20), (17, 24), (15, 24), (12, 20)], fill=(2, 3, 4, 255))
        draw.rectangle((7, 15, 10, 23), fill=black)
        draw.rectangle((22, 15, 25, 23), fill=black)
        draw.rectangle((10, 23, 22, 27), fill=outline)
        draw.rectangle((12, 24, 20, 25), fill=mid)
        draw.line((13, 13, 16, 18), fill=white, width=2)
        draw.line((19, 13, 16, 18), fill=white, width=2)
        draw.point((13, 13), fill=(210, 216, 224, 255))
        draw.point((19, 13), fill=(210, 216, 224, 255))
    elif piece == "coat":
        draw.rectangle((12, 5, 20, 8), fill=outline)
        draw.rectangle((13, 4, 19, 7), fill=black)
        draw.rectangle((8, 8, 24, 29), fill=outline)
        draw.rectangle((10, 9, 22, 28), fill=dark)
        draw.rectangle((5, 10, 10, 22), fill=outline)
        draw.rectangle((22, 10, 27, 22), fill=outline)
        draw.rectangle((6, 11, 10, 17), fill=dark)
        draw.rectangle((22, 11, 26, 17), fill=dark)
        draw.rectangle((6, 19, 11, 24), fill=black)
        draw.rectangle((21, 19, 26, 24), fill=black)
        draw.rectangle((9, 25, 13, 30), fill=black)
        draw.rectangle((19, 25, 23, 30), fill=black)
        draw.polygon([(11, 9), (15, 9), (15, 26), (12, 29), (9, 29), (9, 14)], fill=mid)
        draw.polygon([(17, 9), (21, 9), (23, 14), (23, 29), (20, 29), (17, 26)], fill=mid)
        draw.rectangle((13, 10, 19, 14), fill=plate)
        draw.rectangle((13, 15, 19, 18), fill=(18, 20, 24, 255))
        draw.rectangle((13, 19, 19, 22), fill=(16, 18, 22, 255))
        draw.rectangle((12, 21, 20, 24), fill=outline)
        draw.rectangle((14, 22, 18, 23), fill=metal)
        draw.rectangle((15, 22, 17, 23), fill=(20, 22, 26, 255))
        for y in (13, 16, 19):
            strap_line(6, y, 10, y)
            strap_line(22, y, 26, y)
        draw.rectangle((14, 24, 15, 26), fill=edge)
        draw.rectangle((17, 24, 18, 26), fill=edge)
    elif piece == "leggings":
        draw.rectangle((8, 5, 24, 8), fill=outline)
        draw.rectangle((10, 6, 14, 7), fill=plate)
        draw.rectangle((18, 6, 22, 7), fill=plate)
        draw.rectangle((14, 4, 18, 8), fill=metal)
        draw.rectangle((15, 5, 17, 7), fill=outline)
        draw.rectangle((8, 8, 15, 29), fill=outline)
        draw.rectangle((17, 8, 24, 29), fill=outline)
        draw.rectangle((10, 9, 14, 27), fill=dark)
        draw.rectangle((18, 9, 22, 27), fill=dark)
        draw.rectangle((7, 13, 10, 18), fill=outline)
        draw.rectangle((22, 13, 25, 18), fill=outline)
        draw.rectangle((9, 17, 15, 21), fill=plate)
        draw.rectangle((17, 17, 23, 21), fill=plate)
        draw.rectangle((10, 18, 14, 20), fill=edge)
        draw.rectangle((18, 18, 22, 20), fill=edge)
        for y in (10, 12, 23, 26):
            strap_line(9, y, 14, y)
            strap_line(18, y, 23, y)
        draw.rectangle((9, 28, 15, 30), fill=black)
        draw.rectangle((17, 28, 23, 30), fill=black)
    else:
        draw.rectangle((6, 9, 14, 26), fill=outline)
        draw.rectangle((18, 9, 26, 26), fill=outline)
        draw.rectangle((8, 10, 12, 24), fill=dark)
        draw.rectangle((20, 10, 24, 24), fill=dark)
        draw.rectangle((7, 23, 15, 29), fill=outline)
        draw.rectangle((17, 23, 25, 29), fill=outline)
        draw.rectangle((8, 24, 14, 27), fill=plate)
        draw.rectangle((18, 24, 24, 27), fill=plate)
        draw.rectangle((5, 27, 15, 30), fill=black)
        draw.rectangle((17, 27, 27, 30), fill=black)
        draw.rectangle((8, 10, 12, 13), fill=plate)
        draw.rectangle((20, 10, 24, 13), fill=plate)
        draw.rectangle((9, 11, 11, 12), fill=metal)
        draw.rectangle((21, 11, 23, 12), fill=metal)
        for y in (15, 18, 21):
            strap_line(8, y, 12, y)
            strap_line(20, y, 24, y)
    return image


def reference_icons():
    if not ICON_REFERENCE.exists():
        return {}

    source = Image.open(ICON_REFERENCE).convert("RGBA")
    cells = {
        "hood": (14, 14, 620, 620),
        "coat": (634, 14, 1240, 620),
        "leggings": (14, 634, 620, 1240),
        "boots": (634, 634, 1240, 1240),
    }
    icons = {}
    for piece, box in cells.items():
        crop = source.crop(box)
        pixels = crop.load()
        alpha = Image.new("L", crop.size, 0)
        alpha_pixels = alpha.load()
        for y in range(crop.height):
            for x in range(crop.width):
                r, g, b, a = pixels[x, y]
                if a and max(r, g, b) < 190:
                    alpha_pixels[x, y] = 255
        bounds = alpha.getbbox()
        if not bounds:
            continue
        x0, y0, x1, y1 = bounds
        pad = 12
        bounds = (
            max(0, x0 - pad),
            max(0, y0 - pad),
            min(crop.width, x1 + pad),
            min(crop.height, y1 + pad),
        )
        crop = crop.crop(bounds)
        transparent = Image.new("RGBA", crop.size, (0, 0, 0, 0))
        crop_pixels = crop.load()
        out_pixels = transparent.load()
        for y in range(crop.height):
            for x in range(crop.width):
                r, g, b, a = crop_pixels[x, y]
                light_neutral = min(r, g, b) > 160 and max(r, g, b) - min(r, g, b) < 18
                if a and max(r, g, b) < 205 and not light_neutral:
                    out_pixels[x, y] = (r, g, b, 255)

        transparent.thumbnail((30, 30), Image.Resampling.NEAREST)
        icon_image = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
        icon_image.alpha_composite(transparent, ((32 - transparent.width) // 2, (32 - transparent.height) // 2))
        if piece == "hood":
            icon_draw = ImageDraw.Draw(icon_image)
            white = (250, 253, 255, 255)
            icon_draw.line((11, 11, 12, 14, 14, 17), fill=white, width=1)
            icon_draw.line((21, 11, 20, 14, 18, 17), fill=white, width=1)
            icon_draw.point((12, 12), fill=white)
            icon_draw.point((20, 12), fill=white)
            icon_draw.point((14, 16), fill=white)
            icon_draw.point((18, 16), fill=white)
        else:
            icon_pixels = icon_image.load()
            for y in range(icon_image.height):
                for x in range(icon_image.width):
                    r, g, b, a = icon_pixels[x, y]
                    if a and min(r, g, b) > 185:
                        icon_pixels[x, y] = (0, 0, 0, 0)
        icons[piece] = icon_image
    return icons


def first_person_texture():
    image = Image.new("RGBA", (64, 64), (10, 11, 14, 255))
    draw = ImageDraw.Draw(image)
    draw.rectangle((32, 16, 55, 31), fill=(18, 19, 23, 255))
    draw.rectangle((40, 16, 55, 19), fill=(54, 58, 66, 255))
    draw.rectangle((48, 48, 63, 63), fill=(16, 17, 20, 255))
    draw.line((32, 29, 55, 29), fill=(42, 44, 50, 255))
    draw.line((48, 60, 63, 60), fill=(42, 44, 50, 255))
    draw.line((34, 18, 51, 18), fill=(68, 73, 84, 255))
    return image


def write_preview(geo):
    preview = Image.new("RGBA", (720, 420), (13, 14, 17, 255))
    draw = ImageDraw.Draw(preview)
    bones = geo["minecraft:geometry"][0]["bones"]
    colors = {
        "hood": (38, 40, 46, 255),
        "coat": (30, 32, 38, 255),
        "armor": (23, 25, 31, 255),
        "eye": (245, 250, 255, 255),
        "boot": (20, 21, 25, 255),
    }

    def pick(name):
        if "eye" in name:
            return colors["eye"]
        if "hood" in name or "helmet" in name or "faceplate" in name:
            return colors["hood"]
        if "coat" in name:
            return colors["coat"]
        if "boot" in name or "sole" in name:
            return colors["boot"]
        return colors["armor"]

    def draw_cube(c, offset_x, offset_y, scale, side="front"):
        x, y, z = c["origin"]
        sx, sy, sz = c["size"]
        if side == "back":
            x = -x - sx
        x0 = offset_x + (x + 7) * scale
        y0 = offset_y + (31 - y - sy) * scale
        x1 = offset_x + (x + sx + 7) * scale
        y1 = offset_y + (31 - y) * scale
        return (x0, y0, x1, y1)

    for side, ox, title in [("front", 70, "FRONT"), ("back", 410, "BACK")]:
        draw.text((ox + 95, 24), title, fill=(180, 184, 190, 255))
        for b in bones:
            name = b["name"]
            color = pick(name)
            for c in b.get("cubes", []):
                box = draw_cube(c, ox, 48, 10, side)
                draw.rectangle(box, fill=color, outline=(6, 7, 9, 255))
        draw.line((ox + 70, 383, ox + 220, 383), fill=(70, 74, 82, 255))

    out = ROOT / "tmp" / "riftwalker_armor_tz_preview.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    preview.save(out)


def write_bbmodel(geo, texture_path, texture_image):
    name = "Riftwalker_Armor"
    bones = geo["minecraft:geometry"][0]["bones"]
    elements = []
    groups_by_name = {}

    def cube_face_payload(geo_cube, direction):
        uv = geo_cube.get("uv", [0, 0])
        if isinstance(uv, dict):
            face_data = uv.get(direction) or uv.get("north") or next(iter(uv.values()))
            u, v = face_data["uv"]
            width, height = face_data.get("uv_size", [1, 1])
            return {"uv": [u, v, u + width, v + height], "texture": 0}
        if len(uv) == 4:
            return {"uv": uv, "texture": 0}
        u, v = uv
        return {"uv": [u, v, u + 3, v + 3], "texture": 0}

    for bone_data in bones:
        bone_name = bone_data["name"]
        group_uuid = stable_uuid(name, f"bone:{bone_name}")
        groups_by_name[bone_name] = {
            "name": bone_name,
            "origin": bone_data.get("pivot", [0, 0, 0]),
            "uuid": group_uuid,
            "children": [],
            "parent": bone_data.get("parent"),
        }

        poly_mesh = bone_data.get("poly_mesh")
        if poly_mesh:
            mesh_uuid = stable_uuid(name, f"mesh:{bone_name}")
            vertices = {}
            vertex_keys = []
            positions = poly_mesh["positions"]
            for position_index in range(len(positions) // 3):
                key = stable_uuid(name, f"mesh:{bone_name}:vertex:{position_index}")
                offset = position_index * 3
                vertices[key] = positions[offset:offset + 3]
                vertex_keys.append(key)

            faces = {}
            uvs = poly_mesh["uvs"]
            for face_index, polygon in enumerate(poly_mesh["polys"]):
                face_key = stable_uuid(name, f"mesh:{bone_name}:face:{face_index}")
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
                "name": f"mesh_{bone_name}",
                "color": len(elements) % 8,
                "origin": bone_data.get("pivot", [0, 0, 0]),
                "rotation": [0, 0, 0],
                "vertices": vertices,
                "faces": faces,
                "type": "mesh",
                "uuid": mesh_uuid,
            })
            groups_by_name[bone_name]["children"].append(mesh_uuid)

        for cube_index, geo_cube in enumerate(bone_data.get("cubes", [])):
            cube_uuid = stable_uuid(name, f"cube:{bone_name}:{cube_index}")
            origin = geo_cube["origin"]
            size = geo_cube["size"]
            element = {
                "name": f"{bone_name}_{cube_index}",
                "box_uv": False,
                "render_order": "default",
                "from": origin,
                "to": [origin[index] + size[index] for index in range(3)],
                "origin": geo_cube.get("pivot", [origin[index] + size[index] / 2 for index in range(3)]),
                "faces": {
                    direction: cube_face_payload(geo_cube, direction)
                    for direction in ("north", "east", "south", "west", "up", "down")
                },
                "type": "cube",
                "uuid": cube_uuid,
            }
            if "rotation" in geo_cube:
                element["rotation"] = geo_cube["rotation"]
            elements.append(element)
            groups_by_name[bone_name]["children"].append(cube_uuid)

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
        outliner_group(bone_data["name"])
        for bone_data in bones
        if not bone_data.get("parent")
    ]

    buffer = io.BytesIO()
    texture_image.save(buffer, format="PNG")
    bb_dir = ROOT / "bbmodels"
    bb_dir.mkdir(parents=True, exist_ok=True)
    editable_texture = bb_dir / "Riftwalker_Armor_Texture.png"
    texture_image.save(editable_texture)

    payload = {
        "meta": {"format_version": "5.0", "model_format": "geckolib_model", "box_uv": True},
        "name": name,
        "model_identifier": "riftwalker_armor",
        "geckolib_modid": "riftborne",
        "resolution": {"width": 256, "height": 256},
        "elements": elements,
        "groups": groups,
        "outliner": outliner,
        "textures": [{
            "path": str(texture_path),
            "name": texture_path.name,
            "folder": "armor",
            "namespace": "riftborne",
            "id": "0",
            "particle": False,
            "visible": True,
            "mode": "bitmap",
            "saved": True,
            "uuid": stable_uuid(name, "texture"),
            "relative_path": "../src/main/resources/assets/riftborne/textures/armor/riftwalker_armor.png",
            "source": "data:image/png;base64," + base64.b64encode(buffer.getvalue()).decode("ascii"),
        }],
        "animations": [],
        "geckolib_model_type": "Entity",
    }
    (bb_dir / "Riftwalker_Armor.bbmodel").write_text(
        json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )


def main():
    geo_dir = RES / "geo" / "armor"
    tex_dir = RES / "textures" / "armor"
    item_tex_dir = RES / "textures" / "item"
    anim_dir = RES / "animations" / "armor"
    model_dir = RES / "models" / "item"
    for directory in (geo_dir, tex_dir, item_tex_dir, anim_dir, model_dir):
        directory.mkdir(parents=True, exist_ok=True)

    geo = build_geometry()
    (geo_dir / "riftwalker_armor.geo.json").write_text(
        json.dumps(geo, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    (anim_dir / "riftwalker_armor.animation.json").write_text(
        json.dumps({"format_version": "1.8.0", "animations": {}}, indent=2) + "\n", encoding="utf-8"
    )
    texture = build_texture()
    glowmask = build_glowmask()
    texture.save(tex_dir / "riftwalker_armor.png")
    glowmask.save(tex_dir / "riftwalker_armor_glowmask.png")
    glowmask.save(tex_dir / "riftwalker_armor_emissive.png")
    first_person_texture().save(tex_dir / "riftwalker_first_person.png")

    pieces = {
        "riftwalker_hood": "hood",
        "riftwalker_coat": "coat",
        "riftwalker_leggings": "leggings",
        "riftwalker_boots": "boots",
    }
    extracted_icons = reference_icons()
    for name, piece in pieces.items():
        icon_path = item_tex_dir / f"{name}.png"
        if name != "riftwalker_hood" or not icon_path.exists():
            extracted_icons.get(piece, icon(piece)).save(icon_path)
        (model_dir / f"{name}.json").write_text(json.dumps({
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"riftborne:item/{name}"},
        }, indent=2) + "\n", encoding="utf-8")

    write_preview(geo)
    write_bbmodel(geo, tex_dir / "riftwalker_armor.png", texture)


if __name__ == "__main__":
    main()
