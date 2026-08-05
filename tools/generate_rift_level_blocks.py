"""Generate the five cohesive rift-level block families and their data assets.

The level palette walks from the cold cyan slate of the surface shard into the magenta-black
Limit.  Stone uses broad interspace-like plates, crust breaks those plates into smaller fragments,
and veins add a restrained one-pixel resonance network.  The generator is intentionally fully
deterministic so rerunning it never changes shipped textures by accident.
"""

import collections
import io
import json
import os
import random

from PIL import Image, ImageDraw


ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "src/main/resources/assets/riftborne/textures/block")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/riftborne")
DATA = os.path.join(ROOT, "src/main/resources/data/riftborne")
PREVIEW = os.path.join(ROOT, "build/asset-previews/rift_level_blocks.png")

# base rock, crust plate, resonance accent, maximum shard yield, deterministic seed
LEVELS = [
    ("rift_l1", (27, 50, 68), (49, 83, 105), (39, 199, 226), 1, 1101),
    ("rift_l2", (23, 34, 61), (42, 61, 103), (70, 132, 235), 1, 2202),
    ("rift_l3", (31, 25, 54), (62, 44, 92), (139, 91, 235), 2, 3303),
    ("rift_l4", (23, 17, 39), (70, 38, 86), (205, 83, 232), 3, 4404),
    ("rift_l5", (12, 9, 22), (48, 29, 59), (245, 99, 220), 4, 5505),
]


def shade(colour, amount):
    return tuple(max(0, min(255, channel + amount)) for channel in colour)


def mix(first, second, weight):
    return tuple(round(a * (1.0 - weight) + b * weight) for a, b in zip(first, second))


def wrapped_distance(x, y, cx, cy):
    """Squared toroidal distance keeps plate boundaries compatible at tile edges."""
    dx = min(abs(x - cx), 16 - abs(x - cx))
    dy = min(abs(y - cy), 16 - abs(y - cy))
    return dx * dx + dy * dy


def sites_for(fragment_count, rng):
    if fragment_count <= 7:
        anchors = [(2, 2), (9, 1), (14, 5), (4, 8), (11, 9), (2, 14), (9, 14)]
    else:
        anchors = [
            (1, 1), (6, 2), (11, 1), (15, 4),
            (3, 6), (8, 7), (13, 8),
            (1, 11), (6, 12), (11, 12), (15, 14), (4, 15),
        ]
    return [((x + rng.choice((-1, 0, 0, 1))) % 16,
             (y + rng.choice((-1, 0, 0, 1))) % 16)
            for x, y in anchors[:fragment_count]]


def plate_texture(base, plate, rng, fragment_count, contrast):
    """Broad irregular facets with quiet edges instead of outlined circuitry."""
    sites = sites_for(fragment_count, rng)
    tones = [rng.choice((-6, -3, 0, 0, 4, 7)) for _ in sites]
    regions = [[0 for _ in range(16)] for _ in range(16)]
    for y in range(16):
        for x in range(16):
            regions[y][x] = min(range(len(sites)), key=lambda i: wrapped_distance(x, y, *sites[i]))

    image = Image.new("RGBA", (16, 16), base + (255,))
    pixels = image.load()
    for y in range(16):
        for x in range(16):
            region = regions[y][x]
            top_edge = regions[(y - 1) % 16][x] != region
            left_edge = regions[y][(x - 1) % 16] != region
            bottom_edge = regions[(y + 1) % 16][x] != region
            right_edge = regions[y][(x + 1) % 16] != region

            colour = shade(base, tones[region])
            if fragment_count > 7:
                colour = mix(colour, plate, 0.16 + (region % 2) * 0.04)
            edge_phase = (x * 5 + y * 3 + region) % (6 if fragment_count <= 7 else 5)
            if (top_edge or left_edge) and edge_phase <= 1:
                weight = 0.22 if fragment_count <= 7 else 0.36
                colour = mix(shade(base, tones[region] + 5), plate, weight)
            elif (bottom_edge or right_edge) and edge_phase == 2:
                colour = shade(base, tones[region] - contrast)
            # Sparse, ordered facets add texture without falling back to random-noise dithering.
            if not (top_edge or left_edge or bottom_edge or right_edge) and (x * 5 + y * 3 + region) % 19 == 0:
                colour = shade(colour, 6)
            pixels[x, y] = colour + (255,)
    return image


def rock(base, plate, rng):
    return plate_texture(base, plate, rng, fragment_count=7, contrast=8)


def crust(base, plate, rng):
    return plate_texture(base, shade(plate, 5), rng, fragment_count=12, contrast=11)


def vein(base, plate, accent, rng):
    """A thin branching crack network laid over broad stone plates."""
    image = plate_texture(base, plate, rng, fragment_count=7, contrast=9)
    pixels = image.load()
    bright = set()

    y = rng.randrange(4, 12)
    main = []
    for x in range(16):
        main.append((x, y))
        y = max(1, min(14, y + rng.choice((-1, 0, 0, 1))))
    bright.update(main)

    for branch_x, direction in ((4, -1), (10, 1), (13, -1)):
        x, y = main[branch_x]
        while 0 <= y < 16:
            bright.add((x % 16, y))
            x += rng.choice((-1, 0, 1))
            y += direction

    halo = shade(accent, -85)
    for x, y in bright:
        neighbour = ((x + (1 if (x + y) % 2 else -1)) % 16, y)
        if neighbour not in bright:
            pixels[neighbour[0], neighbour[1]] = halo + (255,)
    for x, y in bright:
        pixels[x, y] = accent + (255,)

    # Only junctions reach the near-white resonance highlight.
    for x, y in bright:
        neighbours = sum(((x + dx) % 16, (y + dy) % 16) in bright
                         for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if neighbours >= 3:
            pixels[x, y] = shade(accent, 48) + (255,)
    return image


def validate_texture(name, image):
    if image.mode != "RGBA" or image.size != (16, 16):
        raise ValueError(f"{name}: expected a 16x16 RGBA texture")
    alpha = {image.getpixel((x, y))[3] for y in range(16) for x in range(16)}
    if alpha != {255}:
        raise ValueError(f"{name}: block textures must be fully opaque")
    colours = {image.getpixel((x, y)) for y in range(16) for x in range(16)}
    if len(colours) > 24:
        raise ValueError(f"{name}: palette grew beyond 24 colours ({len(colours)})")


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with io.open(path, "w", encoding="utf-8") as handle:
        json.dump(obj, handle, ensure_ascii=False, indent=2)


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
generated = []
for level, base, plate, accent, shard_max, seed in LEVELS:
    for kind_index, kind in enumerate(("stone", "crust", "vein")):
        name = f"{level}_{kind}"
        rng = random.Random(seed + kind_index * 97)
        image = {
            "stone": lambda: rock(base, plate, rng),
            "crust": lambda: crust(base, plate, rng),
            "vein": lambda: vein(base, plate, accent, rng),
        }[kind]()
        validate_texture(name, image)
        image.save(os.path.join(TEX, name + ".png"), optimize=True)
        generated.append((name, image))

        write_json(os.path.join(ASSETS, "blockstates", name + ".json"),
                   {"variants": {"": {"model": "riftborne:block/" + name}}})
        write_json(os.path.join(ASSETS, "models/block", name + ".json"),
                   {"parent": "minecraft:block/cube_all", "textures": {"all": "riftborne:block/" + name}})
        write_json(os.path.join(ASSETS, "models/item", name + ".json"),
                   {"parent": "riftborne:block/" + name})

        if kind == "vein":
            entry = {
                "type": "minecraft:item",
                "name": "riftborne:rift_shard",
                "functions": [
                    {"function": "minecraft:set_count", "count": {"min": 1, "max": shard_max}},
                    {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune",
                     "formula": "minecraft:ore_drops"},
                ],
            }
        else:
            entry = {"type": "minecraft:item", "name": "riftborne:" + name}
        write_json(os.path.join(DATA, "loot_table/blocks", name + ".json"), {
            "type": "minecraft:block",
            "pools": [{
                "rolls": 1,
                "entries": [entry],
                "conditions": [{"condition": "minecraft:survives_explosion"}],
            }],
            "random_sequence": "riftborne:blocks/" + name,
        })

        en[f"block.riftborne.{name}"] = f"{EN_LEVEL[level]} {EN_KIND[kind]}"
        ru[f"block.riftborne.{name}"] = f"{RU_KIND[kind]} {RU_LEVEL[level]}"
    print("built", level)

for lang_name, table in (("en_us", en), ("ru_ru", ru)):
    path = os.path.join(ASSETS, "lang", lang_name + ".json")
    with io.open(path, encoding="utf-8") as handle:
        data = json.load(handle, object_pairs_hook=collections.OrderedDict)
    data.update(table)
    with io.open(path, "w", encoding="utf-8") as handle:
        json.dump(data, handle, ensure_ascii=False, indent=2)

scale, gap = 10, 8
preview = Image.new("RGBA", (3 * 16 * scale + 4 * gap, 5 * 16 * scale + 6 * gap), (15, 18, 25, 255))
for index, (_, image) in enumerate(generated):
    x = gap + (index % 3) * (16 * scale + gap)
    y = gap + (index // 3) * (16 * scale + gap)
    preview.alpha_composite(image.resize((16 * scale, 16 * scale), Image.Resampling.NEAREST), (x, y))
os.makedirs(os.path.dirname(PREVIEW), exist_ok=True)
preview.save(PREVIEW, optimize=True)

print(f"validated and wrote {len(generated)} rift block textures")
