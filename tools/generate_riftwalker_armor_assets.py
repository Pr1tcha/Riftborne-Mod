from __future__ import annotations

import json
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src" / "main" / "resources" / "assets" / "riftborne"


def cube(origin, size, uv, inflate=0.0, rotation=None, pivot=None):
    result = {
        "origin": origin,
        "size": size,
        "uv": uv,
    }
    if inflate:
        result["inflate"] = inflate
    if rotation:
        result["rotation"] = rotation
        result["pivot"] = pivot
    return result


def face_cube(origin, size, north_uv, side_uv=(0, 0, 2, 2), inflate=0.0):
    def face(region):
        u1, v1, u2, v2 = region
        return {"uv": [u1, v1], "uv_size": [u2 - u1, v2 - v1]}

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
    return result


def bone(name, pivot, cubes, parent=None):
    result = {"name": name, "pivot": pivot, "cubes": cubes}
    if parent:
        result["parent"] = parent
    return result


def build_geometry():
    bones = [
        bone("armorHead", [0, 24, 0], [
            # A complete inner helmet prevents the player's skin from showing
            # through at the rear, temples or under the hood.
            cube([-4.15, 23.75, -4.15], [8.3, 8.35, 8.3], [0, 0], 0.06),
            # Flat black mask. Its front face owns a dedicated atlas area so
            # emissive lines remain painted pixels instead of protruding cubes.
            face_cube([-3.82, 24.05, -4.34], [7.64, 7.55, 0.08], (96, 0, 128, 32)),
            # Block-built hood: broad crown, side cheeks and a projecting rim.
            cube([-5.0, 29.0, -3.8], [10.0, 3.4, 7.6], [26, 0]),
            cube([-5.05, 23.3, -3.45], [1.85, 6.8, 6.9], [0, 18], rotation=[0, 0, -4], pivot=[-4.0, 27, 0]),
            cube([3.2, 23.3, -3.45], [1.85, 6.8, 6.9], [18, 18], rotation=[0, 0, 4], pivot=[4.0, 27, 0]),
            cube([-4.65, 22.9, -4.7], [9.3, 1.05, 1.55], [38, 18]),
            cube([-4.4, 23.0, 2.45], [8.8, 5.4, 1.15], [38, 24]),
        ]),
        bone("armorBody", [0, 24, 0], [
            # Coat torso and raised armored chest.
            cube([-4.82, 11.55, -2.75], [9.64, 12.7, 5.5], [0, 36], 0.12),
            cube([-3.35, 16.8, -3.05], [6.7, 5.6, 1.15], [30, 36]),
            cube([-2.45, 13.5, -3.15], [4.9, 3.0, 1.3], [48, 36]),
            cube([-4.9, 21.0, -2.85], [9.8, 2.0, 5.7], [66, 36]),
            # Broad dark shoulder bridges cover the vanilla shoulder corners
            # while keeping the silhouette sloped rather than boxy.
            cube([-6.25, 20.65, -2.95], [2.7, 2.45, 5.9], [94, 36],
                 rotation=[0, 0, -8], pivot=[-4.1, 21.5, 0]),
            cube([3.55, 20.65, -2.95], [2.7, 2.45, 5.9], [106, 36],
                 rotation=[0, 0, 8], pivot=[4.1, 21.5, 0]),
            cube([-4.7, 11.3, -2.9], [1.25, 10.2, 1.0], [66, 46], rotation=[0, 0, -3], pivot=[-4.1, 17, -2.4]),
            cube([3.45, 11.3, -2.9], [1.25, 10.2, 1.0], [74, 46], rotation=[0, 0, 3], pivot=[4.1, 17, -2.4]),
            # Belt and rear coat spine.
            cube([-4.75, 10.55, -2.7], [9.5, 1.35, 5.4], [84, 36]),
            cube([-1.15, 11.2, 2.45], [2.3, 9.6, 0.9], [84, 44]),
        ]),
        bone("armorRightArm", [-5, 22, 0], [
            cube([-8.65, 11.45, -2.75], [4.8, 11.65, 5.5], [0, 62], 0.14),
            cube([-8.65, 19.0, -2.75], [4.65, 3.6, 5.5], [20, 62]),
            cube([-8.55, 12.0, -2.75], [0.9, 7.0, 5.5], [42, 62]),
        ]),
        bone("armorLeftArm", [5, 22, 0], [
            cube([3.85, 11.45, -2.75], [4.8, 11.65, 5.5], [0, 80], 0.14),
            cube([4.0, 19.0, -2.75], [4.65, 3.6, 5.5], [20, 80]),
            cube([7.65, 12.0, -2.75], [0.9, 7.0, 5.5], [42, 80]),
        ]),
        bone("armorRightLeg", [-1.9, 12, 0], [
            cube([-4.28, -0.1, -2.38], [4.5, 12.4, 4.76], [60, 62], 0.09),
            cube([-4.45, 4.0, -2.85], [4.65, 3.45, 1.0], [82, 62]),
            # Front and rear coat panels follow the leg, keeping the split silhouette.
            cube([-5.05, 2.0, -3.0], [4.7, 9.4, 0.9], [60, 82], rotation=[-2, 0, 2], pivot=[-2, 10.5, -2.4]),
            cube([-4.75, 2.35, 2.05], [4.4, 9.05, 0.9], [82, 82], rotation=[2, 0, 1], pivot=[-2, 10.5, 2.4]),
        ]),
        bone("armorLeftLeg", [1.9, 12, 0], [
            cube([-0.22, -0.1, -2.38], [4.5, 12.4, 4.76], [60, 100], 0.09),
            cube([-0.2, 4.0, -2.85], [4.65, 3.45, 1.0], [82, 100]),
            cube([0.35, 2.0, -3.0], [4.7, 9.4, 0.9], [98, 62], rotation=[-2, 0, -2], pivot=[2, 10.5, -2.4]),
            cube([0.35, 2.35, 2.05], [4.4, 9.05, 0.9], [104, 82], rotation=[2, 0, -1], pivot=[2, 10.5, 2.4]),
        ]),
        bone("armorRightBoot", [-1.9, 12, 0], [
            cube([-4.35, -0.25, -2.55], [4.65, 5.0, 5.1], [0, 98], 0.09),
            cube([-4.45, -0.55, -3.55], [4.85, 2.2, 6.15], [24, 98]),
            cube([-4.5, 3.3, -2.75], [4.95, 1.3, 5.5], [48, 98]),
        ]),
        bone("armorLeftBoot", [1.9, 12, 0], [
            cube([-0.3, -0.25, -2.55], [4.65, 5.0, 5.1], [0, 112], 0.09),
            cube([-0.4, -0.55, -3.55], [4.85, 2.2, 6.15], [24, 112]),
            cube([-0.45, 3.3, -2.75], [4.95, 1.3, 5.5], [48, 112]),
        ]),
    ]
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.riftwalker_armor",
                "texture_width": 128,
                "texture_height": 128,
                "visible_bounds_width": 2.2,
                "visible_bounds_height": 3.2,
                "visible_bounds_offset": [0, 1.4, 0],
            },
            "bones": bones,
        }],
    }


def leather_noise(draw, bounds, base, edge, seed):
    x0, y0, x1, y1 = bounds
    draw.rectangle(bounds, fill=base)
    # Sparse, deliberate pixel clusters: texture, not photographic noise.
    for i in range(0, (x1 - x0) * (y1 - y0), 23):
        x = x0 + ((i * 17 + seed * 11) % max(1, x1 - x0))
        y = y0 + ((i * 7 + seed * 5) % max(1, y1 - y0))
        draw.point((x, y), fill=edge if i % 46 else (17, 18, 23, 255))


def build_texture():
    image = Image.new("RGBA", (128, 128), (12, 13, 17, 255))
    draw = ImageDraw.Draw(image)
    # Large material fields corresponding to the model UV neighborhoods.
    leather_noise(draw, (0, 0, 63, 35), (18, 20, 25, 255), (32, 35, 43, 255), 1)
    leather_noise(draw, (0, 36, 127, 61), (22, 24, 30, 255), (42, 45, 54, 255), 2)
    leather_noise(draw, (0, 62, 127, 97), (19, 21, 27, 255), (36, 39, 48, 255), 3)
    leather_noise(draw, (0, 98, 127, 127), (16, 18, 23, 255), (34, 37, 46, 255), 4)

    # Armor plate language: broad graphite faces with crisp cold edges.
    for box in [(30, 36, 47, 49), (48, 36, 64, 48), (82, 62, 96, 75), (82, 100, 96, 113)]:
        draw.rectangle(box, fill=(27, 30, 37, 255))
        draw.line((box[0], box[1], box[2], box[1]), fill=(67, 72, 84, 255))
        draw.line((box[0], box[1], box[0], box[3]), fill=(45, 49, 59, 255))
        draw.line((box[0], box[3], box[2], box[3]), fill=(10, 11, 15, 255))

    # Restrained violet undertone ties the suit to Riftwalker space.
    for y in (56, 76, 94, 108):
        draw.line((5, y, 118, y), fill=(30, 24, 42, 255))

    # The mask's north-facing region sits in the upper atlas. Two angular,
    # one-pixel slashes remain the only pure-white marks on the suit.
    # Dedicated mask region. Base texture stays absolutely black: when the
    # emissive pass is disabled no eye marks remain visible.
    draw.rectangle((96, 0, 127, 31), fill=(3, 4, 6, 255))
    return image


def build_glowmask():
    image = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    # Wider spacing and a tapered broken-chevron silhouette. A dim cyan fringe
    # gives bloom something to catch while the white core stays razor sharp.
    cyan = (105, 184, 255, 155)
    white = (235, 247, 255, 255)
    left = [(101, 8), (104, 11), (106, 15), (107, 21)]
    right = [(122, 8), (119, 11), (117, 15), (116, 21)]
    draw.line(left, fill=cyan, width=3)
    draw.line(right, fill=cyan, width=3)
    draw.line(left, fill=white, width=1)
    draw.line(right, fill=white, width=1)
    return image


def icon(piece):
    image = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    outline = (8, 9, 12, 255)
    dark = (18, 20, 25, 255)
    mid = (37, 40, 49, 255)
    edge = (67, 72, 84, 255)
    white = (229, 241, 255, 255)
    if piece == "hood":
        draw.polygon([(7, 9), (11, 4), (21, 4), (25, 9), (24, 25), (8, 25)], fill=outline)
        draw.polygon([(9, 10), (12, 6), (20, 6), (23, 10), (21, 23), (11, 23)], fill=dark)
        draw.polygon([(12, 11), (20, 11), (19, 20), (13, 20)], fill=(7, 8, 11, 255))
    elif piece == "coat":
        draw.polygon([(9, 4), (23, 4), (27, 10), (24, 28), (8, 28), (5, 10)], fill=outline)
        draw.polygon([(10, 6), (22, 6), (24, 11), (22, 26), (10, 26), (8, 11)], fill=dark)
        draw.polygon([(12, 8), (20, 8), (22, 15), (19, 19), (13, 19), (10, 15)], fill=mid)
        draw.line((12, 8, 20, 8), fill=edge)
        draw.rectangle((12, 21, 20, 23), fill=mid)
    elif piece == "leggings":
        draw.polygon([(9, 4), (23, 4), (22, 15), (20, 28), (14, 28), (13, 15), (12, 28), (6, 28), (9, 15)], fill=outline)
        draw.polygon([(11, 6), (15, 6), (14, 25), (9, 25)], fill=dark)
        draw.polygon([(17, 6), (21, 6), (23, 25), (18, 25)], fill=dark)
        draw.rectangle((9, 14, 14, 17), fill=mid)
        draw.rectangle((18, 14, 23, 17), fill=mid)
    else:
        draw.polygon([(5, 8), (13, 8), (14, 21), (11, 27), (3, 27), (3, 20)], fill=outline)
        draw.polygon([(19, 8), (27, 8), (29, 20), (29, 27), (21, 27), (18, 21)], fill=outline)
        draw.polygon([(7, 10), (11, 10), (12, 21), (10, 24), (5, 24)], fill=dark)
        draw.polygon([(21, 10), (25, 10), (27, 24), (22, 24), (20, 21)], fill=dark)
        draw.line((6, 11, 12, 11), fill=edge)
        draw.line((20, 11, 26, 11), fill=edge)
    return image


def first_person_texture():
    image = Image.new("RGBA", (64, 64), (12, 13, 17, 255))
    draw = ImageDraw.Draw(image)
    # Player arm and sleeve UV regions. Keeping the whole sheet dark also makes
    # this robust for both classic and slim player models.
    draw.rectangle((0, 0, 63, 63), fill=(13, 15, 20, 255))
    draw.rectangle((32, 16, 55, 31), fill=(19, 21, 27, 255))
    draw.rectangle((40, 16, 55, 19), fill=(43, 47, 57, 255))
    draw.rectangle((48, 48, 63, 63), fill=(18, 20, 26, 255))
    draw.line((32, 29, 55, 29), fill=(31, 25, 44, 255))
    draw.line((48, 60, 63, 60), fill=(31, 25, 44, 255))
    return image


def main():
    geo_dir = RES / "geo" / "armor"
    tex_dir = RES / "textures" / "armor"
    item_tex_dir = RES / "textures" / "item"
    anim_dir = RES / "animations" / "armor"
    model_dir = RES / "models" / "item"
    for directory in (geo_dir, tex_dir, item_tex_dir, anim_dir, model_dir):
        directory.mkdir(parents=True, exist_ok=True)

    (geo_dir / "riftwalker_armor.geo.json").write_text(
        json.dumps(build_geometry(), ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    (anim_dir / "riftwalker_armor.animation.json").write_text(
        json.dumps({"format_version": "1.8.0", "animations": {}}, indent=2) + "\n", encoding="utf-8"
    )
    build_texture().save(tex_dir / "riftwalker_armor.png")
    build_glowmask().save(tex_dir / "riftwalker_armor_glowmask.png")
    first_person_texture().save(tex_dir / "riftwalker_first_person.png")

    pieces = {
        "riftwalker_hood": "hood",
        "riftwalker_coat": "coat",
        "riftwalker_leggings": "leggings",
        "riftwalker_boots": "boots",
    }
    for name, piece in pieces.items():
        icon(piece).save(item_tex_dir / f"{name}.png")
        (model_dir / f"{name}.json").write_text(json.dumps({
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"riftborne:item/{name}"},
        }, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
