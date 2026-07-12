import base64
import io
import json
import uuid
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/riftborne"
DATA = ROOT / "src/main/resources/data/riftborne"
BBMODELS = ROOT / "bbmodels"

PALETTE = {
    "outline": (12, 18, 24, 255),
    "shadow": (25, 34, 42, 255),
    "metal": (48, 62, 70, 255),
    "light": (91, 111, 116, 255),
    "cyan": (48, 226, 214, 255),
    "blue": (52, 139, 226, 255),
    "orange": (232, 126, 45, 255),
    "violet": (164, 91, 224, 255),
    "green": (74, 205, 126, 255),
}

STATIONS = {
    "endurance_station": {
        "title": "Endurance Station",
        "accent": "cyan",
        "elements": [
            ("base", [0, 0, 0], [16, 3, 16]),
            ("belt", [3, 3, 1], [13, 4, 15]),
            ("console_post", [11, 4, 12], [14, 11, 15]),
            ("console", [9, 10, 10], [15, 14, 15]),
        ],
    },
    "strength_station": {
        "title": "Strength Station",
        "accent": "orange",
        "elements": [
            ("mat", [1, 0, 1], [15, 2, 15]),
            ("bench", [4, 2, 3], [12, 5, 13]),
            ("rack_left", [2, 2, 11], [4, 13, 14]),
            ("rack_right", [12, 2, 11], [14, 13, 14]),
            ("bar", [1, 11, 11.5], [15, 12, 13.5]),
        ],
    },
    "motorics_station": {
        "title": "Motorics Station",
        "accent": "violet",
        "elements": [
            ("pad", [1, 0, 1], [15, 2, 15]),
            ("center", [5, 2, 5], [11, 3, 11]),
            ("north", [6, 2, 1], [10, 5, 4]),
            ("south", [6, 2, 12], [10, 5, 15]),
            ("west", [1, 2, 6], [4, 5, 10]),
            ("east", [12, 2, 6], [15, 5, 10]),
        ],
    },
    "stability_station": {
        "title": "Stability Station",
        "accent": "green",
        "elements": [
            ("base", [1, 0, 1], [15, 2, 15]),
            ("lower_platform", [3, 2, 3], [13, 3, 13]),
            ("balance_plate", [4, 3, 4], [12, 5, 12]),
            ("focus_pillar", [7, 5, 7], [9, 11, 9]),
            ("focus_core", [6, 10, 6], [10, 14, 10]),
        ],
    },
}


def stable_uuid(name):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, f"riftborne:physical_training:{name}"))


def write_json(path, payload):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def station_texture(accent_name):
    image = Image.new("RGBA", (32, 32), PALETTE["shadow"])
    draw = ImageDraw.Draw(image)
    accent = PALETTE[accent_name]
    draw.rectangle((0, 0, 31, 31), outline=PALETTE["outline"], width=2)
    for y in range(3, 30, 4):
        draw.line((2, y, 29, y), fill=PALETTE["metal"])
    for x in range(4, 29, 6):
        draw.line((x, 2, x, 29), fill=PALETTE["light"])
    draw.rectangle((4, 11, 27, 20), fill=PALETTE["outline"])
    draw.rectangle((6, 13, 25, 18), fill=accent)
    draw.line((7, 14, 24, 14), fill=tuple(min(255, c + 30) for c in accent[:3]) + (255,))
    return image


def weight_texture():
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.rectangle((1, 5, 4, 10), fill=PALETTE["outline"])
    draw.rectangle((2, 4, 5, 11), fill=PALETTE["metal"])
    draw.rectangle((5, 7, 10, 8), fill=PALETTE["light"])
    draw.rectangle((10, 4, 13, 11), fill=PALETTE["metal"])
    draw.rectangle((12, 5, 15, 10), fill=PALETTE["outline"])
    draw.point((3, 5), fill=PALETTE["orange"])
    draw.point((12, 5), fill=PALETTE["orange"])
    return image


def save_png(path, image):
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, format="PNG", optimize=False)


def png_data_url(image):
    output = io.BytesIO()
    image.save(output, format="PNG", optimize=False)
    return "data:image/png;base64," + base64.b64encode(output.getvalue()).decode("ascii")


def vanilla_element(start, end):
    return {
        "from": start,
        "to": end,
        "faces": {
            face: {"texture": "#all", "uv": [0, 0, 16, 16]}
            for face in ("north", "east", "south", "west", "up", "down")
        },
    }


def bbmodel(name, title, elements, image):
    bb_elements = []
    outliner = []
    for element_name, start, end in elements:
        element_uuid = stable_uuid(f"{name}:{element_name}")
        bb_elements.append({
            "name": element_name,
            "box_uv": False,
            "render_order": "default",
            "locked": False,
            "export": True,
            "from": start,
            "to": end,
            "autouv": 0,
            "color": len(bb_elements) % 8,
            "origin": [(start[0] + end[0]) / 2, (start[1] + end[1]) / 2, (start[2] + end[2]) / 2],
            "faces": {
                face: {"uv": [0, 0, 16, 16], "texture": 0}
                for face in ("north", "east", "south", "west", "up", "down")
            },
            "type": "cube",
            "uuid": element_uuid,
        })
        outliner.append(element_uuid)
    return {
        "meta": {
            "format_version": "4.10",
            "model_format": "java_block",
            "box_uv": False,
        },
        "name": title,
        "model_identifier": name,
        "visible_box": [1, 1, 0],
        "variable_placeholders": "",
        "variable_placeholder_buttons": [],
        "timeline_setups": [],
        "unhandled_root_fields": {},
        "resolution": {"width": 32, "height": 32},
        "elements": bb_elements,
        "outliner": outliner,
        "textures": [{
            "path": f"src/main/resources/assets/riftborne/textures/block/{name}.png",
            "name": f"{name}.png",
            "folder": "block",
            "namespace": "riftborne",
            "id": "0",
            "particle": True,
            "render_mode": "default",
            "visible": True,
            "mode": "bitmap",
            "saved": True,
            "uuid": stable_uuid(f"texture:{name}"),
            "source": png_data_url(image),
        }],
    }


def recipe(name, accent_item):
    return {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": ["IRI", "SCS", "III"],
        "key": {
            "I": {"item": "minecraft:iron_ingot"},
            "R": {"item": accent_item},
            "S": {"item": "minecraft:smooth_stone"},
            "C": {"item": "minecraft:copper_ingot"},
        },
        "result": {"id": f"riftborne:{name}", "count": 1},
    }


def main():
    recipe_accents = {
        "endurance_station": "minecraft:redstone",
        "strength_station": "minecraft:anvil",
        "motorics_station": "minecraft:amethyst_shard",
        "stability_station": "minecraft:slime_ball",
    }
    for name, spec in STATIONS.items():
        image = station_texture(spec["accent"])
        save_png(ASSETS / "textures/block" / f"{name}.png", image)
        write_json(BBMODELS / f"Physical_{spec['title'].replace(' ', '_')}.bbmodel",
                   bbmodel(name, spec["title"], spec["elements"], image))
        write_json(ASSETS / "blockstates" / f"{name}.json", {
            "variants": {"": {"model": f"riftborne:block/{name}"}}
        })
        write_json(ASSETS / "models/block" / f"{name}.json", {
            "textures": {"all": f"riftborne:block/{name}", "particle": f"riftborne:block/{name}"},
            "elements": [vanilla_element(start, end) for _, start, end in spec["elements"]],
        })
        write_json(ASSETS / "models/item" / f"{name}.json", {
            "parent": f"riftborne:block/{name}"
        })
        write_json(DATA / "loot_table/blocks" / f"{name}.json", {
            "type": "minecraft:block",
            "pools": [{
                "rolls": 1,
                "entries": [{
                    "type": "minecraft:item",
                    "name": f"riftborne:{name}",
                    "conditions": [{"condition": "minecraft:survives_explosion"}],
                }],
            }],
        })
        write_json(DATA / "recipe" / f"{name}.json", recipe(name, recipe_accents[name]))

    weight = weight_texture()
    save_png(ASSETS / "textures/item/training_weight.png", weight)
    write_json(ASSETS / "models/item/training_weight.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "riftborne:item/training_weight"},
    })
    write_json(DATA / "recipe/training_weight.json", {
        "type": "minecraft:crafting_shaped",
        "category": "equipment",
        "pattern": ["I I", "ISI", "I I"],
        "key": {
            "I": {"item": "minecraft:iron_ingot"},
            "S": {"item": "minecraft:stick"},
        },
        "result": {"id": "riftborne:training_weight", "count": 1},
    })


if __name__ == "__main__":
    main()
