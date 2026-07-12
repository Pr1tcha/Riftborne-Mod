from __future__ import annotations

import colorsys
import json
import shutil
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
GEO = ROOT / "src/main/resources/assets/riftborne/geo/entity/rift_splinter.geo.json"
TEXTURE = ROOT / "src/main/resources/assets/riftborne/textures/entity/rift_splinter.png"
BACKUP = ROOT / "tmp/rift_splinter_texture_before_rna_blue.png"
PREVIEW = ROOT / "tmp/rift_splinter_texture_rna_blue_preview.png"


def cube_pixels(cube: dict) -> set[tuple[int, int]]:
    pixels: set[tuple[int, int]] = set()
    for face in cube["uv"].values():
        u, v = face["uv"]
        width, height = face["uv_size"]
        x1, x2 = sorted((round(u), round(u + width)))
        y1, y2 = sorted((round(v), round(v + height)))
        pixels.update((x, y) for y in range(y1, y2) for x in range(x1, x2))
    return pixels


def is_rift_purple(red: int, green: int, blue: int) -> bool:
    if max(red, green, blue) == 0:
        return False
    hue, saturation, _ = colorsys.rgb_to_hsv(red / 255, green / 255, blue / 255)
    return saturation >= 0.08 and 0.68 <= hue <= 0.94


def rna_blue(red: int, green: int, blue: int, *, brightness_scale: float) -> tuple[int, int, int]:
    _, saturation, value = colorsys.rgb_to_hsv(red / 255, green / 255, blue / 255)
    value = min(1.0, value * brightness_scale)

    # Dark pixels remain deep navy; brighter fractures move toward RNA cyan-blue.
    hue = 0.625 - 0.052 * value
    saturation = min(0.96, max(0.48, saturation * 1.08))
    new_red, new_green, new_blue = colorsys.hsv_to_rgb(hue, saturation, value)
    return round(new_red * 255), round(new_green * 255), round(new_blue * 255)


def main() -> None:
    geometry = json.loads(GEO.read_text(encoding="utf-8"))["minecraft:geometry"][0]
    base_pixels: set[tuple[int, int]] = set()
    overlay_pixels: set[tuple[int, int]] = set()
    focal_pixels: set[tuple[int, int]] = set()

    for bone in geometry["bones"]:
        for cube in bone.get("cubes", []):
            pixels = cube_pixels(cube)
            if cube.get("inflate", 0) > 0:
                overlay_pixels.update(pixels)
            else:
                base_pixels.update(pixels)

            # Keep the eye and central torso scars as the brightest landmarks.
            if bone["name"] in {"head", "torso", "chest", "pelvis"}:
                north = cube["uv"]["north"]
                u, v = north["uv"]
                width, height = north["uv_size"]
                x1, x2 = sorted((round(u), round(u + width)))
                y1, y2 = sorted((round(v), round(v + height)))
                focal_pixels.update((x, y) for y in range(y1, y2) for x in range(x1, x2))

    image = Image.open(TEXTURE).convert("RGBA")
    if image.size != (128, 128):
        raise ValueError(f"Expected a 128x128 Rift Splinter texture, got {image.size}")

    BACKUP.parent.mkdir(parents=True, exist_ok=True)
    if not BACKUP.exists():
        shutil.copyfile(TEXTURE, BACKUP)
    pixels = image.load()

    for y in range(image.height):
        for x in range(image.width):
            red, green, blue, alpha = pixels[x, y]
            if alpha == 0 or not is_rift_purple(red, green, blue):
                continue

            coordinate = (x, y)
            if coordinate in overlay_pixels:
                red, green, blue = rna_blue(red, green, blue, brightness_scale=0.90)
                alpha = min(132, round(alpha * 0.72))
            elif coordinate in focal_pixels:
                red, green, blue = rna_blue(red, green, blue, brightness_scale=1.08)
            elif coordinate in base_pixels:
                # The authored limbs contain broad bright fills. Compress them
                # into darker fracture detail so the body reads as black first.
                _, _, original_value = colorsys.rgb_to_hsv(red / 255, green / 255, blue / 255)
                scale = 0.68 if original_value > 0.34 else 0.88
                red, green, blue = rna_blue(red, green, blue, brightness_scale=scale)
            else:
                red, green, blue = rna_blue(red, green, blue, brightness_scale=0.82)

            pixels[x, y] = red, green, blue, alpha

    image.save(TEXTURE)

    checker = Image.new("RGBA", image.size)
    checker_pixels = checker.load()
    for y in range(image.height):
        for x in range(image.width):
            shade = 36 if (x // 4 + y // 4) % 2 == 0 else 62
            checker_pixels[x, y] = shade, shade, shade, 255
    checker.alpha_composite(image)
    checker.resize((1024, 1024), Image.Resampling.NEAREST).save(PREVIEW)
    print(f"Saved {TEXTURE.relative_to(ROOT)}")
    print(f"Saved {PREVIEW.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
