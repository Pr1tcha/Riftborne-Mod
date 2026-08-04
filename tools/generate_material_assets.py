"""Generate placeholder art for the material chain.

Deliberately simple, readable shapes rather than finished art: the point is that the items are
distinguishable on the hotbar while the chain is being balanced. Replace with hand-drawn textures
later — nothing in code depends on these pixels.
"""

import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ITEM_TEX = os.path.join(ROOT, "src/main/resources/assets/riftborne/textures/item")
BLOCK_TEX = os.path.join(ROOT, "src/main/resources/assets/riftborne/textures/block")

CYAN = (79, 208, 220)
CYAN_DARK = (24, 92, 104)
VIOLET = (155, 135, 245)
VIOLET_DARK = (72, 58, 128)
STEEL = (108, 122, 140)
STEEL_DARK = (48, 56, 68)
AMBER = (232, 212, 138)
SHELL = (26, 32, 42)


def new():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def resonance_core():
    """A contained, faceted core — raw shard matter brought under control."""
    img = new()
    d = ImageDraw.Draw(img)
    d.polygon([(8, 1), (14, 8), (8, 15), (2, 8)], fill=CYAN_DARK, outline=STEEL_DARK)
    d.polygon([(8, 4), (11, 8), (8, 12), (5, 8)], fill=CYAN)
    d.line([(8, 4), (8, 12)], fill=(210, 250, 255))
    d.point((7, 7), fill=(255, 255, 255))
    return img


def rna_conductor():
    """A drawn filament: conductive, not crystalline."""
    img = new()
    d = ImageDraw.Draw(img)
    d.rectangle([3, 6, 12, 9], fill=STEEL_DARK)
    d.rectangle([4, 7, 11, 8], fill=CYAN)
    d.rectangle([1, 5, 3, 10], fill=STEEL)
    d.rectangle([12, 5, 14, 10], fill=STEEL)
    d.point((2, 7), fill=CYAN)
    d.point((13, 8), fill=CYAN)
    return img


def damaged_codex_laptop():
    """A closed, cracked terminal — clearly the same family as the working laptop, clearly broken."""
    img = new()
    d = ImageDraw.Draw(img)
    d.rectangle([1, 4, 14, 12], fill=SHELL, outline=STEEL_DARK)
    d.rectangle([3, 6, 12, 10], fill=(18, 24, 32))
    d.line([(4, 6), (8, 10)], fill=STEEL)
    d.line([(8, 7), (11, 9)], fill=STEEL)
    d.point((11, 11), fill=AMBER)
    d.rectangle([1, 12, 14, 13], fill=STEEL_DARK)
    return img


def stabilizer_face(active_ring):
    """Block face: a housing with a containment ring around the working chamber."""
    img = Image.new("RGBA", (16, 16), SHELL)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=STEEL_DARK)
    d.rectangle([2, 2, 13, 13], fill=(20, 26, 34), outline=STEEL)
    d.ellipse([4, 4, 11, 11], outline=active_ring)
    d.point((7, 7), fill=active_ring)
    d.point((8, 8), fill=active_ring)
    return img


def stabilizer_top():
    img = Image.new("RGBA", (16, 16), (22, 28, 36))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=STEEL_DARK)
    d.ellipse([3, 3, 12, 12], outline=CYAN, fill=(16, 22, 30))
    d.ellipse([6, 6, 9, 9], fill=CYAN)
    return img


def save(img, folder, name):
    os.makedirs(folder, exist_ok=True)
    path = os.path.join(folder, name + ".png")
    img.save(path)
    print("wrote", os.path.relpath(path, ROOT))


save(resonance_core(), ITEM_TEX, "resonance_core")
save(rna_conductor(), ITEM_TEX, "rna_conductor")
save(damaged_codex_laptop(), ITEM_TEX, "damaged_codex_laptop")
save(stabilizer_face(VIOLET_DARK), BLOCK_TEX, "resonance_stabilizer_side")
save(stabilizer_face(CYAN), BLOCK_TEX, "resonance_stabilizer_side_working")
save(stabilizer_top(), BLOCK_TEX, "resonance_stabilizer_top")


# --- intermediate parts ------------------------------------------------------------------------
COPPER = (196, 124, 84)
COPPER_DARK = (112, 66, 42)
IRON = (176, 182, 190)
IRON_DARK = (96, 102, 112)
STEEL_RES = (138, 156, 176)
BOARD = (36, 72, 68)


def copper_wire():
    """Coiled filament — reads as flexible, not as an ingot."""
    img = new()
    d = ImageDraw.Draw(img)
    for i, y in enumerate(range(3, 13, 3)):
        d.line([(3, y), (12, y)], fill=COPPER)
        d.point((3, y + 1), fill=COPPER_DARK)
        d.point((12, y + 1), fill=COPPER_DARK)
        if y + 3 < 13:
            side = 12 if i % 2 == 0 else 3
            d.line([(side, y), (side, y + 3)], fill=COPPER_DARK)
    return img


def iron_plate():
    """Flat stock with rivets, clearly not an ingot."""
    img = new()
    d = ImageDraw.Draw(img)
    d.rectangle([2, 4, 13, 11], fill=IRON, outline=IRON_DARK)
    for x in (4, 11):
        for y in (6, 9):
            d.point((x, y), fill=IRON_DARK)
    d.line([(3, 5), (12, 5)], fill=(206, 212, 220))
    return img


def resonant_steel():
    """An ingot, but veined with the cyan of stabilized matter."""
    img = new()
    d = ImageDraw.Draw(img)
    d.polygon([(3, 6), (12, 6), (13, 11), (2, 11)], fill=STEEL_RES, outline=IRON_DARK)
    d.line([(4, 8), (11, 8)], fill=CYAN)
    d.point((6, 9), fill=CYAN)
    d.point((9, 7), fill=CYAN)
    return img


def resonance_circuit():
    """A board: traces and a seated core."""
    img = new()
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, 13, 13], fill=BOARD, outline=(18, 38, 36))
    d.line([(4, 5), (11, 5)], fill=COPPER)
    d.line([(4, 10), (11, 10)], fill=COPPER)
    d.line([(4, 5), (4, 10)], fill=COPPER)
    d.rectangle([6, 6, 9, 9], fill=CYAN_DARK, outline=STEEL)
    d.point((7, 7), fill=CYAN)
    d.point((8, 8), fill=CYAN)
    return img


def shielded_casing():
    """A closed box with a shielded seam."""
    img = new()
    d = ImageDraw.Draw(img)
    d.rectangle([2, 3, 13, 12], fill=IRON_DARK, outline=(60, 66, 76))
    d.rectangle([4, 5, 11, 10], fill=STEEL_RES)
    d.line([(4, 7), (11, 7)], fill=IRON_DARK)
    d.point((3, 4), fill=CYAN)
    d.point((12, 11), fill=CYAN)
    return img


save(copper_wire(), ITEM_TEX, "copper_wire")
save(iron_plate(), ITEM_TEX, "iron_plate")
save(resonant_steel(), ITEM_TEX, "resonant_steel")
save(resonance_circuit(), ITEM_TEX, "resonance_circuit")
save(shielded_casing(), ITEM_TEX, "shielded_casing")
