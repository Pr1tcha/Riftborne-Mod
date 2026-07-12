import json
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/riftborne"
DATA = ROOT / "src/main/resources/data/riftborne"


def write_json(path, payload):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def write_png(path, pixels):
    height = len(pixels)
    width = len(pixels[0])

    def chunk(name, data):
        return (
            struct.pack(">I", len(data))
            + name
            + data
            + struct.pack(">I", zlib.crc32(name + data) & 0xFFFFFFFF)
        )

    raw = b"".join(b"\x00" + bytes(channel for rgba in row for channel in rgba) for row in pixels)
    png = (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(raw, 9))
        + chunk(b"IEND", b"")
    )
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def mat_texture():
    pixels = []
    for y in range(32):
        row = []
        for x in range(32):
            border = x < 2 or x >= 30 or y < 2 or y >= 30
            seam = y in (15, 16)
            stripe = (x + y) % 9 == 0
            if border:
                color = (16, 68, 76, 255)
            elif seam:
                color = (24, 100, 112, 255)
            elif stripe:
                color = (42, 153, 169, 255)
            else:
                color = (35, 137, 153, 255)
            row.append(color)
        pixels.append(row)
    return pixels


def strap_texture():
    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                color = (9, 18, 22, 255)
            elif (x + y) % 5 == 0:
                color = (26, 45, 52, 255)
            else:
                color = (15, 29, 35, 255)
            row.append(color)
        pixels.append(row)
    return pixels


def face(texture="#mat", uv=None):
    payload = {"texture": texture}
    if uv is not None:
        payload["uv"] = uv
    return payload


def cube(from_, to, texture="#mat", up_uv=None, all_uv=None):
    faces = {
        "north": face(texture, all_uv),
        "east": face(texture, all_uv),
        "south": face(texture, all_uv),
        "west": face(texture, all_uv),
        "down": face(texture, all_uv),
        "up": face(texture, up_uv if up_uv is not None else all_uv),
    }
    return {"from": from_, "to": to, "faces": faces}


def unfolded_model(part):
    top_uv = [0, 0, 16, 8] if part == "foot" else [0, 8, 16, 16]
    return {
        "textures": {
            "mat": "riftborne:block/pushup_mat",
            "particle": "riftborne:block/pushup_mat",
        },
        "elements": [
            cube([1, 0, 0], [15, 1, 16], up_uv=top_uv, all_uv=[0, 15, 16, 16]),
        ],
    }


def rolled_model():
    return {
        "textures": {
            "mat": "riftborne:block/pushup_mat",
            "strap": "riftborne:block/pushup_mat_strap",
            "particle": "riftborne:block/pushup_mat",
        },
        "elements": [
            cube([1, 1, 4], [15, 6, 12], all_uv=[0, 0, 16, 16]),
            cube([2, 0, 5], [14, 1, 11], all_uv=[1, 1, 15, 15]),
            cube([2, 6, 5], [14, 7, 11], all_uv=[1, 1, 15, 15]),
            cube([4, 0, 3.5], [5, 7, 12.5], texture="#strap", all_uv=[0, 0, 16, 16]),
            cube([11, 0, 3.5], [12, 7, 12.5], texture="#strap", all_uv=[0, 0, 16, 16]),
        ],
    }


def pushup_mat_blockstates():
    variants = {}
    rotations = {
        "north": {},
        "east": {"y": 90},
        "south": {"y": 180},
        "west": {"y": 270},
    }
    for facing, rotation in rotations.items():
        for part in ("foot", "head"):
            rolled_key = f"facing={facing},part={part},rolled=true"
            variants[rolled_key] = {"model": "riftborne:block/pushup_mat_rolled", **rotation}

            flat_key = f"facing={facing},part={part},rolled=false"
            variants[flat_key] = {"model": f"riftborne:block/pushup_mat_{part}", **rotation}
    return {"variants": variants}


def main():
    write_png(ASSETS / "textures/block/pushup_mat.png", mat_texture())
    write_png(ASSETS / "textures/block/pushup_mat_strap.png", strap_texture())

    write_json(ASSETS / "blockstates/pushup_mat.json", pushup_mat_blockstates())
    write_json(ASSETS / "models/block/pushup_mat_foot.json", unfolded_model("foot"))
    write_json(ASSETS / "models/block/pushup_mat_head.json", unfolded_model("head"))
    write_json(ASSETS / "models/block/pushup_mat_rolled.json", rolled_model())
    write_json(ASSETS / "models/item/pushup_mat.json", {"parent": "riftborne:block/pushup_mat_rolled"})
    write_json(DATA / "loot_table/blocks/pushup_mat.json", {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "entries": [{
                "type": "minecraft:item",
                "name": "riftborne:pushup_mat",
                "conditions": [{"condition": "minecraft:survives_explosion"}],
            }],
        }],
    })
    write_json(DATA / "recipe/pushup_mat.json", {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": ["WWW", "SCS", "LLL"],
        "key": {
            "W": {"item": "minecraft:cyan_wool"},
            "S": {"item": "minecraft:string"},
            "C": {"item": "minecraft:copper_ingot"},
            "L": {"item": "minecraft:leather"},
        },
        "result": {"id": "riftborne:pushup_mat", "count": 1},
    })


if __name__ == "__main__":
    main()
