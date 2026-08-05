"""Generate the Riftborne material-chain icons and stabilizer block textures.

The icons share the Codex/interspace language: near-black blue outlines, cold directional metal,
oxidised copper where it carries current, and restrained cyan resonance.  Every item keeps a
different silhouette and remains readable at the native 16x16 inventory size.
"""

import argparse
import os

from PIL import Image, ImageDraw


ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ITEM_TEX = os.path.join(ROOT, "src/main/resources/assets/riftborne/textures/item")
BLOCK_TEX = os.path.join(ROOT, "src/main/resources/assets/riftborne/textures/block")
PREVIEW = os.path.join(ROOT, "build/asset-previews/rift_materials.png")

TRANSPARENT = (0, 0, 0, 0)
OUTLINE = (5, 14, 19)
DEEP = (10, 25, 30)
SHELL = (17, 31, 38)
STEEL_DARK = (40, 54, 65)
STEEL = (88, 108, 120)
STEEL_LIGHT = (151, 174, 180)
EDGE = (204, 220, 217)
IRON_DARK = (65, 72, 82)
IRON = (141, 151, 159)
IRON_LIGHT = (205, 216, 219)
COPPER_DARK = (91, 48, 31)
COPPER = (184, 101, 57)
COPPER_LIGHT = (237, 151, 88)
BOARD_DARK = (7, 34, 35)
BOARD = (17, 69, 65)
CYAN_DARK = (7, 79, 87)
CYAN = (27, 190, 200)
CYAN_LIGHT = (107, 240, 226)
WHITE = (221, 255, 244)
VIOLET_DARK = (66, 43, 104)
VIOLET = (145, 89, 220)


def new():
    return Image.new("RGBA", (16, 16), TRANSPARENT)


def rift_shard():
    """An unstable shard cluster rather than a recoloured vanilla amethyst."""
    image = new()
    draw = ImageDraw.Draw(image)
    draw.polygon([(7, 1), (11, 4), (10, 10), (7, 15), (4, 10), (5, 4)], fill=OUTLINE)
    draw.polygon([(7, 3), (9, 5), (8, 11), (7, 13), (5, 9), (6, 4)], fill=CYAN_DARK)
    draw.line([(7, 3), (7, 11)], fill=CYAN)
    draw.point((7, 5), fill=CYAN_LIGHT)
    draw.polygon([(2, 7), (5, 8), (5, 12), (3, 13), (1, 10)], fill=OUTLINE)
    draw.polygon([(2, 8), (4, 9), (4, 11), (3, 11)], fill=VIOLET)
    draw.polygon([(11, 7), (14, 6), (15, 9), (13, 12), (10, 11)], fill=OUTLINE)
    draw.polygon([(12, 8), (14, 7), (14, 9), (12, 11)], fill=CYAN)
    return image


def copper_wire():
    """A compact wound coil with exposed leads and top-left copper highlights."""
    image = new()
    draw = ImageDraw.Draw(image)
    path = [(3, 2), (11, 2), (13, 4), (13, 6), (3, 6), (2, 7),
            (2, 9), (12, 9), (13, 10), (13, 12), (5, 12), (2, 15)]
    draw.line(path, fill=OUTLINE, width=3)
    draw.line(path, fill=COPPER, width=1)
    draw.line([(4, 2), (10, 2)], fill=COPPER_LIGHT)
    draw.line([(4, 6), (11, 6)], fill=COPPER_LIGHT)
    draw.line([(3, 9), (10, 9)], fill=COPPER_LIGHT)
    draw.point((2, 14), fill=COPPER_LIGHT)
    return image


def iron_plate():
    """Chamfered flat stock with rivets and an unmistakable sheet-metal face."""
    image = new()
    draw = ImageDraw.Draw(image)
    draw.polygon([(3, 3), (12, 3), (14, 5), (14, 11), (12, 13), (3, 13), (1, 11), (1, 5)], fill=OUTLINE)
    draw.polygon([(3, 4), (12, 4), (13, 5), (13, 11), (12, 12), (3, 12), (2, 11), (2, 5)], fill=IRON)
    draw.line([(3, 4), (11, 4)], fill=IRON_LIGHT)
    draw.line([(13, 6), (13, 10)], fill=IRON_DARK)
    draw.line([(4, 12), (12, 12)], fill=IRON_DARK)
    for point in ((4, 6), (11, 6), (4, 10), (11, 10)):
        draw.point(point, fill=IRON_DARK)
    draw.point((4, 6), fill=EDGE)
    return image


def resonant_steel():
    """A forged ingot split by a narrow, contained resonance seam."""
    image = new()
    draw = ImageDraw.Draw(image)
    draw.polygon([(4, 3), (11, 3), (14, 6), (13, 10), (10, 13), (3, 13), (1, 10), (2, 6)], fill=OUTLINE)
    draw.polygon([(4, 4), (10, 4), (12, 6), (11, 9), (9, 11), (4, 12), (2, 10), (3, 6)], fill=STEEL)
    draw.line([(4, 4), (10, 4), (12, 6)], fill=STEEL_LIGHT)
    draw.line([(3, 10), (9, 11), (11, 9)], fill=STEEL_DARK)
    draw.line([(5, 7), (7, 8), (9, 6), (11, 7)], fill=CYAN_DARK)
    draw.line([(6, 7), (7, 8), (9, 6)], fill=CYAN)
    draw.point((9, 6), fill=CYAN_LIGHT)
    return image


def resonance_circuit():
    """A rugged field board with copper traces converging on a cyan core."""
    image = new()
    draw = ImageDraw.Draw(image)
    draw.polygon([(3, 1), (12, 1), (14, 3), (14, 12), (12, 14), (3, 14), (1, 12), (1, 3)], fill=OUTLINE)
    draw.polygon([(3, 2), (12, 2), (13, 3), (13, 12), (12, 13), (3, 13), (2, 12), (2, 3)], fill=BOARD)
    draw.line([(3, 2), (11, 2)], fill=(45, 103, 94))
    draw.line([(3, 5), (6, 5), (6, 7)], fill=COPPER)
    draw.line([(12, 4), (10, 4), (10, 7)], fill=COPPER_LIGHT)
    draw.line([(3, 11), (6, 11), (6, 9)], fill=COPPER_LIGHT)
    draw.line([(12, 11), (10, 11), (10, 9)], fill=COPPER)
    draw.rectangle((6, 6, 10, 10), fill=STEEL_DARK)
    draw.rectangle((7, 7, 9, 9), fill=CYAN_DARK)
    draw.point((8, 7), fill=CYAN_LIGHT)
    draw.point((8, 8), fill=CYAN)
    for point in ((2, 7), (13, 7), (7, 2), (7, 13)):
        draw.point(point, fill=STEEL_LIGHT)
    return image


def shielded_casing():
    """A deep reinforced shell whose nested seam reads as shielding."""
    image = new()
    draw = ImageDraw.Draw(image)
    draw.polygon([(3, 2), (12, 2), (14, 4), (14, 12), (12, 14), (3, 14), (1, 12), (1, 4)], fill=OUTLINE)
    draw.polygon([(3, 3), (12, 3), (13, 4), (13, 12), (12, 13), (3, 13), (2, 12), (2, 4)], fill=STEEL_DARK)
    draw.line([(3, 3), (11, 3)], fill=STEEL_LIGHT)
    draw.line([(2, 5), (2, 11)], fill=STEEL)
    draw.rectangle((4, 5, 11, 11), fill=IRON_DARK)
    draw.rectangle((5, 6, 10, 10), fill=STEEL)
    draw.line([(5, 6), (10, 6)], fill=IRON_LIGHT)
    draw.line([(5, 9), (10, 9)], fill=STEEL_DARK)
    draw.point((3, 4), fill=CYAN_LIGHT)
    draw.point((12, 12), fill=CYAN)
    return image


def resonance_core():
    """A faceted energy crystal locked inside a four-point steel restraint."""
    image = new()
    draw = ImageDraw.Draw(image)
    draw.polygon([(7, 0), (9, 0), (10, 4), (14, 6), (16, 8), (14, 10),
                  (10, 11), (9, 15), (7, 15), (6, 11), (2, 10), (0, 8),
                  (2, 6), (6, 4)], fill=OUTLINE)
    draw.polygon([(8, 2), (10, 7), (8, 13), (5, 8)], fill=CYAN_DARK)
    draw.polygon([(8, 3), (9, 7), (8, 12), (6, 8)], fill=CYAN)
    draw.line([(8, 4), (8, 11)], fill=CYAN_LIGHT)
    draw.point((8, 6), fill=WHITE)
    draw.rectangle((7, 0, 8, 2), fill=STEEL)
    draw.rectangle((7, 13, 8, 15), fill=STEEL_DARK)
    draw.rectangle((0, 7, 2, 8), fill=STEEL_DARK)
    draw.rectangle((13, 7, 15, 8), fill=STEEL)
    return image


def rna_conductor():
    """A sealed filament cartridge with keyed couplers, not a generic bar."""
    image = new()
    draw = ImageDraw.Draw(image)
    draw.polygon([(1, 6), (4, 6), (5, 4), (11, 4), (12, 6), (15, 6),
                  (15, 10), (12, 10), (11, 12), (5, 12), (4, 10), (1, 10)], fill=OUTLINE)
    draw.rectangle((2, 7, 4, 9), fill=STEEL)
    draw.rectangle((12, 7, 14, 9), fill=STEEL_DARK)
    draw.polygon([(5, 5), (10, 5), (12, 7), (12, 9), (10, 11), (5, 11), (4, 9), (4, 7)], fill=STEEL_DARK)
    draw.rectangle((5, 7, 11, 9), fill=CYAN_DARK)
    draw.line([(6, 7), (10, 7)], fill=CYAN)
    draw.line([(6, 8), (10, 8)], fill=CYAN_LIGHT)
    draw.point((2, 7), fill=EDGE)
    draw.point((13, 8), fill=CYAN)
    return image


def damaged_codex_laptop():
    image = new()
    draw = ImageDraw.Draw(image)
    draw.rectangle((1, 4, 14, 12), fill=SHELL, outline=STEEL_DARK)
    draw.rectangle((3, 6, 12, 10), fill=DEEP)
    draw.line([(4, 6), (8, 10), (9, 7), (12, 9)], fill=STEEL)
    draw.point((11, 11), fill=COPPER_LIGHT)
    draw.rectangle((1, 12, 14, 13), fill=STEEL_DARK)
    return image


def stabilizer_face(active_ring):
    image = Image.new("RGBA", (16, 16), SHELL + (255,))
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, 15, 15), outline=STEEL_DARK)
    draw.rectangle((2, 2, 13, 13), fill=DEEP, outline=STEEL)
    draw.ellipse((4, 4, 11, 11), outline=active_ring)
    draw.point((7, 7), fill=active_ring)
    draw.point((8, 8), fill=active_ring)
    return image


def stabilizer_top():
    image = Image.new("RGBA", (16, 16), SHELL + (255,))
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, 15, 15), outline=STEEL_DARK)
    draw.ellipse((3, 3, 12, 12), outline=CYAN, fill=DEEP)
    draw.ellipse((6, 6, 9, 9), fill=CYAN)
    return image


def validate_item(name, image):
    if image.mode != "RGBA" or image.size != (16, 16):
        raise ValueError(f"{name}: expected a 16x16 RGBA item texture")
    alpha = {image.getpixel((x, y))[3] for y in range(16) for x in range(16)}
    if not alpha.issubset({0, 255}):
        raise ValueError(f"{name}: soft alpha would blur the pixel-art silhouette")
    colours = {image.getpixel((x, y)) for y in range(16) for x in range(16) if image.getpixel((x, y))[3]}
    if len(colours) > 12:
        raise ValueError(f"{name}: palette grew beyond 12 opaque colours ({len(colours)})")
    if any(image.getpixel(point)[3] for point in ((0, 0), (15, 0), (0, 15), (15, 15))):
        raise ValueError(f"{name}: icon touches a canvas corner")


def save(image, folder, name, validate=False):
    if validate:
        validate_item(name, image)
    os.makedirs(folder, exist_ok=True)
    path = os.path.join(folder, name + ".png")
    image.save(path, optimize=True)
    print("wrote", os.path.relpath(path, ROOT))


parser = argparse.ArgumentParser(description="Generate Riftborne material and support textures.")
parser.add_argument("--materials-only", action="store_true",
                    help="Do not rewrite the existing damaged Codex or stabilizer support textures.")
args = parser.parse_args()

items = [
    ("rift_shard", rift_shard()),
    ("copper_wire", copper_wire()),
    ("iron_plate", iron_plate()),
    ("resonant_steel", resonant_steel()),
    ("resonance_circuit", resonance_circuit()),
    ("shielded_casing", shielded_casing()),
    ("resonance_core", resonance_core()),
    ("rna_conductor", rna_conductor()),
]
for item_name, item_image in items:
    save(item_image, ITEM_TEX, item_name, validate=True)

if not args.materials_only:
    save(damaged_codex_laptop(), ITEM_TEX, "damaged_codex_laptop", validate=True)
    save(stabilizer_face(VIOLET_DARK), BLOCK_TEX, "resonance_stabilizer_side")
    save(stabilizer_face(CYAN), BLOCK_TEX, "resonance_stabilizer_side_working")
    save(stabilizer_top(), BLOCK_TEX, "resonance_stabilizer_top")

scale, gap = 12, 10
preview = Image.new("RGBA", (4 * 16 * scale + 5 * gap, 2 * 16 * scale + 3 * gap), (16, 20, 27, 255))
for index, (_, item_image) in enumerate(items):
    x = gap + (index % 4) * (16 * scale + gap)
    y = gap + (index // 4) * (16 * scale + gap)
    preview.alpha_composite(item_image.resize((16 * scale, 16 * scale), Image.Resampling.NEAREST), (x, y))
os.makedirs(os.path.dirname(PREVIEW), exist_ok=True)
preview.save(PREVIEW, optimize=True)

print(f"validated and wrote {len(items)} material icons")
