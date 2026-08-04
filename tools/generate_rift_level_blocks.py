"""Textures, models, loot and localisation for the rift level blocks.

Each level gets rock, crust and vein. The palette walks from ordinary grey stone at L1 toward the
violet of the Limit at L5, so a player can tell how deep they are from a single screenshot. Veins
carry the level's accent and are the reason to go deeper: they drop shards, more of them further in.

Placeholder art, generated so the blocks exist and read apart. Replace by hand later.
"""

import io
import json
import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "src/main/resources/assets/riftborne/textures/block")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/riftborne")
DATA = os.path.join(ROOT, "src/main/resources/data/riftborne")

# base rock, crust tint, vein accent — grey stone drifting toward the Limit's violet
LEVELS = [
    ("rift_l1", (122, 124, 128), (150, 152, 156), (110, 178, 196), 1),
    ("rift_l2", (74, 78, 88), (96, 100, 112), (96, 140, 190), 1),
    ("rift_l3", (52, 48, 62), (70, 64, 84), (140, 110, 220), 2),
    ("rift_l4", (34, 30, 46), (48, 42, 64), (168, 96, 224), 3),
    ("rift_l5", (22, 16, 30), (36, 24, 46), (222, 120, 236), 4),
]


def shade(colour, amount):
    return tuple(max(0, min(255, c + amount)) for c in colour)


def rock(base, rng, mottle=16):
    img = Image.new("RGBA", (16, 16), base)
    px = img.load()
    for x in range(16):
        for y in range(16):
            px[x, y] = shade(base, rng.randint(-mottle, mottle)) + (255,)
    return img


def crust(base, tint, rng):
    """Rock with a broken-up upper crust so the surface layer reads as a skin, not a slab."""
    img = rock(base, rng)
    d = ImageDraw.Draw(img)
    for _ in range(26):
        x, y = rng.randrange(16), rng.randrange(16)
        d.point((x, y), fill=shade(tint, rng.randint(-12, 12)))
    for _ in range(5):
        x, y = rng.randrange(15), rng.randrange(15)
        d.line([(x, y), (x + rng.choice((-2, 2)), y + rng.choice((-2, 2)))],
               fill=shade(base, -26))
    return img


def vein(base, accent, rng):
    """Threads of accent running through the rock, brightest at their crossings."""
    img = rock(base, rng, mottle=10)
    d = ImageDraw.Draw(img)
    for _ in range(4):
        x = rng.randrange(16)
        y = 0
        while y < 16:
            d.point((x % 16, y), fill=accent)
            if rng.random() < 0.55:
                d.point(((x + 1) % 16, y), fill=shade(accent, -40))
            x += rng.choice((-1, 0, 0, 1))
            y += 1
    for _ in range(3):
        d.point((rng.randrange(16), rng.randrange(16)), fill=shade(accent, 60))
    return img


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    json.dump(obj, io.open(path, "w", encoding="utf-8"), ensure_ascii=False, indent=2)


en, ru = {}, {}
RU_LEVEL = {
    "rift_l1": "Поверхностного осколка",
    "rift_l2": "Смещённого слоя",
    "rift_l3": "Узлового разлома",
    "rift_l4": "Глубинного разлома",
    "rift_l5": "Предельного среза",
}
EN_LEVEL = {
    "rift_l1": "Surface Shard",
    "rift_l2": "Shifted Layer",
    "rift_l3": "Node Rift",
    "rift_l4": "Deep Rift",
    "rift_l5": "Limit Slice",
}
RU_KIND = {"stone": "Камень", "crust": "Корка", "vein": "Жила"}
EN_KIND = {"stone": "Stone", "crust": "Crust", "vein": "Vein"}

os.makedirs(TEX, exist_ok=True)

for index, (level, base, tint, accent, shard_max) in enumerate(LEVELS):
    rng = random.Random(hash(level) & 0xFFFF)
    for kind in ("stone", "crust", "vein"):
        name = "%s_%s" % (level, kind)
        image = {"stone": lambda: rock(base, rng),
                 "crust": lambda: crust(base, tint, rng),
                 "vein": lambda: vein(base, accent, rng)}[kind]()
        image.save(os.path.join(TEX, name + ".png"))

        write_json(os.path.join(ASSETS, "blockstates", name + ".json"),
                   {"variants": {"": {"model": "riftborne:block/" + name}}})
        write_json(os.path.join(ASSETS, "models/block", name + ".json"),
                   {"parent": "minecraft:block/cube_all",
                    "textures": {"all": "riftborne:block/" + name}})
        write_json(os.path.join(ASSETS, "models/item", name + ".json"),
                   {"parent": "riftborne:block/" + name})

        if kind == "vein":
            # The reason to descend: deeper veins pay out more raw shards.
            entry = {"type": "minecraft:item", "name": "riftborne:rift_shard",
                     "functions": [{"function": "minecraft:set_count",
                                    "count": {"min": 1, "max": shard_max}},
                                   {"function": "minecraft:apply_bonus",
                                    "enchantment": "minecraft:fortune",
                                    "formula": "minecraft:ore_drops"}]}
        else:
            entry = {"type": "minecraft:item", "name": "riftborne:" + name}
        write_json(os.path.join(DATA, "loot_table/blocks", name + ".json"), {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [entry],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}],
            "random_sequence": "riftborne:blocks/" + name,
        })

        en["block.riftborne.%s" % name] = "%s %s" % (EN_LEVEL[level], EN_KIND[kind])
        ru["block.riftborne.%s" % name] = "%s %s" % (RU_KIND[kind], RU_LEVEL[level])

    print("built", level)

for lang_name, table in (("en_us", en), ("ru_ru", ru)):
    path = os.path.join(ASSETS, "lang", lang_name + ".json")
    import collections
    data = json.load(io.open(path, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
    data.update(table)
    json.dump(data, io.open(path, "w", encoding="utf-8"), ensure_ascii=False, indent=2)

print("assets, loot and lang written for %d blocks" % (len(LEVELS) * 3))
