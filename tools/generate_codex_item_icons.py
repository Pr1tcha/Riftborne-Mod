"""Generate the shared 32x32 inventory icon set for all Codex hardware."""

import argparse
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
ITEM_TEXTURES = ROOT / "src/main/resources/assets/riftborne/textures/item"
PREVIEW_PATH = ROOT / "build/asset-previews/codex_technical_icons.png"

TRANSPARENT = (0, 0, 0, 0)
OUTLINE = (6, 15, 18, 255)
DEEP = (11, 27, 31, 255)
CASE_DARK = (23, 43, 45, 255)
CASE = (43, 67, 66, 255)
CASE_LIGHT = (79, 105, 98, 255)
EDGE = (119, 143, 132, 255)
SCREEN = (8, 46, 52, 255)
CYAN_DARK = (10, 100, 108, 255)
CYAN = (21, 193, 198, 255)
CYAN_LIGHT = (112, 245, 229, 255)
WHITE = (206, 238, 221, 255)
AMBER = (229, 173, 65, 255)


def canvas() -> tuple[Image.Image, ImageDraw.ImageDraw]:
    image = Image.new("RGBA", (32, 32), TRANSPARENT)
    return image, ImageDraw.Draw(image)


def draw_laptop() -> Image.Image:
    image, draw = canvas()

    # Straight-on rugged lid, copied from the front in-game silhouette.
    draw.polygon([(6, 2), (25, 2), (27, 4), (27, 16), (25, 18), (6, 18), (4, 16), (4, 4)], fill=OUTLINE)
    draw.rectangle((6, 4, 25, 16), fill=CASE)
    draw.line([(7, 4), (24, 4)], fill=EDGE, width=1)
    draw.rectangle((8, 6, 23, 14), fill=SCREEN)
    draw.line([(9, 7), (22, 7)], fill=CYAN, width=1)
    draw.line([(9, 9), (17, 9)], fill=CYAN_DARK, width=1)
    draw.line([(9, 11), (19, 11)], fill=CYAN_DARK, width=1)
    draw.line([(18, 9), (21, 12)], fill=CYAN_LIGHT, width=1)
    draw.point((6, 15), fill=CYAN)
    draw.point((25, 15), fill=CYAN)

    # Symmetric deck with twin vents, keyboard, touchpad and front equipment drawer.
    draw.polygon([(4, 16), (27, 16), (30, 22), (29, 28), (2, 28), (1, 22)], fill=OUTLINE)
    draw.polygon([(5, 18), (26, 18), (28, 22), (27, 25), (4, 25), (3, 22)], fill=CASE)
    draw.line([(5, 18), (26, 18)], fill=CASE_LIGHT, width=1)
    draw.rectangle((5, 19, 9, 21), fill=DEEP)
    draw.rectangle((22, 19, 26, 21), fill=DEEP)
    draw.line([(6, 20), (8, 20)], fill=EDGE, width=1)
    draw.line([(23, 20), (25, 20)], fill=EDGE, width=1)
    draw.rectangle((10, 19, 21, 23), fill=DEEP)
    draw.line([(11, 20), (20, 20)], fill=CASE_LIGHT, width=1)
    draw.line([(11, 22), (20, 22)], fill=CASE_LIGHT, width=1)
    draw.rectangle((14, 23, 17, 24), fill=CASE_DARK)
    draw.line([(13, 18), (18, 18)], fill=CYAN, width=1)
    draw.rectangle((4, 25, 27, 27), fill=CASE_DARK)
    draw.line([(6, 25), (25, 25)], fill=EDGE, width=1)
    draw.line([(12, 27), (19, 27)], fill=CASE_LIGHT, width=1)
    return image


def draw_dock() -> Image.Image:
    image, draw = canvas()

    # Orthographic empty cradle: rear stop, bed, contacts and two guide rails.
    draw.polygon([(8, 6), (23, 6), (25, 8), (25, 18), (23, 20), (8, 20), (6, 18), (6, 8)], fill=OUTLINE)
    draw.rectangle((8, 8, 23, 18), fill=CASE_DARK)
    draw.line([(9, 8), (22, 8)], fill=CASE_LIGHT, width=1)
    draw.rectangle((9, 12, 22, 18), fill=DEEP)
    draw.point((12, 15), fill=EDGE)
    draw.point((15, 15), fill=EDGE)
    draw.point((19, 15), fill=EDGE)
    draw.polygon([(4, 17), (27, 17), (29, 20), (29, 27), (2, 27), (2, 20)], fill=OUTLINE)
    draw.rectangle((4, 19, 27, 25), fill=CASE)
    draw.rectangle((7, 19, 24, 23), fill=DEEP)
    draw.rectangle((5, 17, 7, 24), fill=EDGE)
    draw.rectangle((24, 17, 26, 24), fill=EDGE)
    draw.line([(8, 19), (23, 19)], fill=CASE_LIGHT, width=1)
    draw.rectangle((4, 25, 27, 27), fill=CASE_DARK)
    draw.line([(11, 26), (20, 26)], fill=CYAN, width=1)
    draw.point((20, 26), fill=CYAN_LIGHT)
    return image


def draw_capsule() -> Image.Image:
    image, draw = canvas()

    # Keep the approved chamber design, but put every vertical on the pixel grid.
    draw.polygon([(7, 2), (24, 2), (27, 5), (27, 27), (24, 30), (7, 30), (4, 27), (4, 5)], fill=OUTLINE)
    draw.rectangle((7, 4, 24, 8), fill=CASE)
    draw.line([(8, 4), (23, 4)], fill=EDGE, width=1)
    draw.rectangle((7, 8, 24, 25), fill=CYAN_DARK)
    draw.rectangle((9, 9, 22, 24), fill=SCREEN)
    draw.rectangle((11, 11, 20, 22), fill=DEEP)
    draw.line([(10, 10), (10, 23)], fill=CYAN, width=1)
    draw.line([(21, 10), (21, 23)], fill=CYAN_LIGHT, width=1)
    draw.line([(15, 11), (15, 22)], fill=CYAN_DARK, width=1)
    draw.line([(9, 17), (22, 17)], fill=CYAN, width=1)
    draw.point((12, 12), fill=CYAN_LIGHT)
    draw.rectangle((5, 8, 7, 26), fill=CASE_LIGHT)
    draw.rectangle((24, 8, 26, 26), fill=CASE_DARK)
    draw.point((25, 11), fill=AMBER)
    draw.point((25, 13), fill=CYAN_LIGHT)
    draw.polygon([(6, 24), (25, 24), (28, 27), (25, 30), (6, 30), (3, 27)], fill=OUTLINE)
    draw.rectangle((7, 25, 24, 28), fill=CASE)
    draw.line([(8, 25), (23, 25)], fill=EDGE, width=1)
    draw.line([(11, 28), (20, 28)], fill=CYAN, width=1)
    return image


def draw_flash_drive() -> Image.Image:
    image, draw = canvas()

    # Clean side elevation of the real armoured USB stick.
    draw.polygon([(3, 11), (20, 11), (23, 14), (29, 14), (30, 16), (30, 20), (29, 22), (23, 22), (20, 25), (3, 25), (1, 23), (1, 13)], fill=OUTLINE)
    draw.rectangle((3, 13, 20, 23), fill=CASE_DARK)
    draw.line([(4, 13), (19, 13)], fill=CASE_LIGHT, width=1)
    draw.rectangle((5, 15, 17, 17), fill=CYAN_DARK)
    draw.rectangle((6, 15, 15, 16), fill=CYAN)
    draw.point((16, 16), fill=CYAN_LIGHT)
    draw.rectangle((5, 20, 18, 22), fill=DEEP)
    draw.rectangle((20, 15, 28, 21), fill=EDGE)
    draw.rectangle((22, 16, 29, 20), fill=CASE_LIGHT)
    draw.rectangle((24, 17, 28, 18), fill=DEEP)
    draw.point((4, 23), fill=CASE)
    return image


def draw_pocket_codex() -> Image.Image:
    image, draw = canvas()

    # Asymmetric one-handed field recorder: raised sensor head, right grip,
    # square diagnostic display and physical controls.
    draw.polygon([(7, 3), (22, 3), (26, 7), (26, 27), (23, 30), (7, 30), (4, 27), (4, 7)], fill=OUTLINE)
    draw.polygon([(8, 5), (21, 5), (24, 7), (24, 26), (22, 28), (8, 28), (6, 26), (6, 7)], fill=CASE)
    draw.rectangle((4, 9, 6, 24), fill=CASE_LIGHT)
    draw.rectangle((23, 10, 27, 23), fill=CASE_DARK)
    draw.rectangle((7, 9, 22, 21), fill=DEEP)
    draw.rectangle((8, 10, 21, 20), fill=SCREEN)
    draw.line([(9, 11), (20, 11)], fill=CYAN, width=1)
    draw.rectangle((10, 13, 18, 17), outline=CYAN)
    draw.line([(14, 12), (14, 19)], fill=CYAN_DARK, width=1)
    draw.point((11, 14), fill=CYAN_LIGHT)
    draw.polygon([(5, 3), (14, 3), (16, 5), (16, 8), (5, 8), (3, 6)], fill=OUTLINE)
    draw.rectangle((6, 4, 14, 7), fill=CASE_LIGHT)
    draw.rectangle((7, 5, 11, 6), fill=CYAN)
    draw.rectangle((3, 1, 5, 7), fill=EDGE)
    draw.point((4, 1), fill=CYAN_LIGHT)
    draw.rectangle((7, 22, 22, 27), fill=DEEP)
    draw.rectangle((8, 23, 11, 25), fill=CASE_DARK)
    draw.rectangle((13, 23, 16, 25), fill=CASE_DARK)
    draw.rectangle((18, 23, 21, 26), fill=CYAN_DARK)
    draw.line([(19, 23), (20, 23)], fill=CYAN_LIGHT, width=1)
    draw.point((23, 8), fill=CYAN_LIGHT)
    return image


def save_icon(name: str, image: Image.Image) -> None:
    ITEM_TEXTURES.mkdir(parents=True, exist_ok=True)
    image.save(ITEM_TEXTURES / f"{name}.png", optimize=True)


def validate_icon(name: str, image: Image.Image) -> None:
    if image.mode != "RGBA" or image.size != (32, 32):
        raise ValueError(f"{name}: expected a 32x32 RGBA icon")
    pixels = [image.getpixel((x, y)) for y in range(32) for x in range(32)]
    alpha_values = {pixel[3] for pixel in pixels}
    if not alpha_values.issubset({0, 255}):
        raise ValueError(f"{name}: soft alpha would blur the pixel-art edge")
    if any(image.getpixel(point)[3] for point in ((0, 0), (31, 0), (0, 31), (31, 31))):
        raise ValueError(f"{name}: icon touches a canvas corner")
    colors = {pixel for pixel in pixels if pixel[3]}
    if len(colors) > 16:
        raise ValueError(f"{name}: palette grew beyond 16 opaque colors")


def save_preview(icons: list[tuple[str, Image.Image]]) -> None:
    scale = 8
    gap = 16
    width = len(icons) * 32 * scale + (len(icons) + 1) * gap
    height = 32 * scale + gap * 2
    preview = Image.new("RGBA", (width, height), (18, 22, 24, 255))
    x = gap
    for _, icon in icons:
        enlarged = icon.resize((32 * scale, 32 * scale), Image.Resampling.NEAREST)
        preview.alpha_composite(enlarged, (x, gap))
        x += 32 * scale + gap
    PREVIEW_PATH.parent.mkdir(parents=True, exist_ok=True)
    preview.save(PREVIEW_PATH, optimize=True)


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate the shared Codex inventory icon set.")
    parser.add_argument("--only", choices=(
        "codex_laptop", "codex_dock", "codex_diagnostic_capsule", "pocket_codex", "codex_flash_drive"
    ))
    args = parser.parse_args()
    icons = [
        ("codex_laptop", draw_laptop()),
        ("codex_dock", draw_dock()),
        ("codex_diagnostic_capsule", draw_capsule()),
        ("pocket_codex", draw_pocket_codex()),
        ("codex_flash_drive", draw_flash_drive()),
    ]
    if args.only:
        icons = [entry for entry in icons if entry[0] == args.only]
    for name, icon in icons:
        validate_icon(name, icon)
        save_icon(name, icon)
        pixels = [icon.getpixel((x, y)) for y in range(32) for x in range(32)]
        print(f"validated {name}: 32x32 RGBA, hard alpha, {len(set(pixels)) - 1} colors")
    save_preview(icons)


if __name__ == "__main__":
    main()
