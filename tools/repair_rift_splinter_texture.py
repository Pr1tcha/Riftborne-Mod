from __future__ import annotations

import json
import base64
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
SOURCE_MODEL = Path(r"C:\Users\gamet\Downloads\Rift Splinter.bbmodel")
SOURCE_TEXTURE = Path(r"C:\Users\gamet\Downloads\texture_8453.png")
OUTPUT_TEXTURE = ROOT / "src/main/resources/assets/riftborne/textures/entity/rift_splinter.png"
OUTPUT_MODEL = ROOT / "bbmodels/Rift_Splinter.bbmodel"
PREVIEW = ROOT / "tmp/rift_splinter_texture_repaired_preview.png"


def uv_rect(face):
    u1, v1, u2, v2 = face["uv"]
    return (
        max(0, int(min(u1, u2))),
        max(0, int(min(v1, v2))),
        min(128, int(max(u1, u2))),
        min(128, int(max(v1, v2))),
    )


def main():
    model = json.loads(SOURCE_MODEL.read_text(encoding="utf-8-sig"))
    image = Image.open(SOURCE_TEXTURE).convert("RGBA")
    pixels = image.load()

    # Cubes 0-7 are the solid body. Their UV islands must never contain
    # transparency or near-black holes. Cubes 8-11 are intentional translucent
    # rift overlays and retain their alpha.
    solid_rects = []
    for element in model["elements"][:8]:
        solid_rects.extend(uv_rect(face) for face in element["faces"].values())

    for x1, y1, x2, y2 in solid_rects:
        for y in range(y1, y2):
            for x in range(x1, x2):
                r, g, b, _ = pixels[x, y]

                # Keep all authored highlights, but lift black pixels into a
                # readable obsidian-purple range so parts do not disappear
                # against Blockbench or Minecraft darkness.
                brightness = max(r, g, b)
                if brightness < 20:
                    r, g, b = 15, 9, 23
                elif brightness < 34:
                    r = max(r, 18)
                    g = max(g, 10)
                    b = max(b, 29)
                elif b >= r and b >= g:
                    b = min(255, max(b, int(b * 1.08)))

                pixels[x, y] = (r, g, b, 255)

    # Restore a crisp reference-like rift eye on the head's north face.
    # The north face occupies x=10..22, y=10..22 in Quinta's UV layout.
    eye = {
        (15, 14): (113, 34, 183, 255),
        (16, 14): (172, 65, 238, 255),
        (14, 15): (102, 28, 170, 255),
        (15, 15): (207, 103, 255, 255),
        (16, 15): (245, 177, 255, 255),
        (17, 15): (172, 65, 238, 255),
        (13, 16): (76, 20, 129, 255),
        (14, 16): (166, 55, 230, 255),
        (15, 16): (245, 177, 255, 255),
        (16, 16): (255, 222, 255, 255),
        (17, 16): (207, 103, 255, 255),
        (18, 16): (96, 26, 158, 255),
        (14, 17): (102, 28, 170, 255),
        (15, 17): (190, 77, 246, 255),
        (16, 17): (231, 135, 255, 255),
        (17, 17): (150, 47, 216, 255),
        (15, 18): (90, 24, 151, 255),
        (16, 18): (137, 41, 203, 255),
    }
    for (x, y), color in eye.items():
        pixels[x, y] = color

    # Strengthen the translucent fracture overlays without filling their
    # intentionally empty pixels.
    overlay_rects = []
    for element in model["elements"][8:]:
        overlay_rects.extend(uv_rect(face) for face in element["faces"].values())

    for x1, y1, x2, y2 in overlay_rects:
        for y in range(y1, y2):
            for x in range(x1, x2):
                r, g, b, a = pixels[x, y]
                if a == 0:
                    continue
                pixels[x, y] = (
                    min(255, max(r, int(r * 1.08))),
                    min(255, max(g, int(g * 1.04))),
                    min(255, max(b, int(b * 1.14))),
                    max(a, 110),
                )

    OUTPUT_TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    image.save(OUTPUT_TEXTURE)

    # Replace the embedded Blockbench texture too; otherwise Blockbench keeps
    # displaying the stale source PNG even though the runtime asset is fixed.
    if OUTPUT_MODEL.exists():
        prepared_model = json.loads(OUTPUT_MODEL.read_text(encoding="utf-8"))
        encoded = base64.b64encode(OUTPUT_TEXTURE.read_bytes()).decode("ascii")
        prepared_model["textures"][0]["source"] = "data:image/png;base64," + encoded
        prepared_model["textures"][0]["internal"] = True
        prepared_model["textures"][0]["saved"] = True
        OUTPUT_MODEL.write_text(
            json.dumps(prepared_model, ensure_ascii=False, separators=(",", ":")),
            encoding="utf-8",
        )

    PREVIEW.parent.mkdir(parents=True, exist_ok=True)
    image.resize((1024, 1024), Image.Resampling.NEAREST).save(PREVIEW)
    print(f"Saved {OUTPUT_TEXTURE.relative_to(ROOT)}")
    print(f"Saved {PREVIEW.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
