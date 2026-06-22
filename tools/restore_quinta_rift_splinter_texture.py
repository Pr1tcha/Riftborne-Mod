from __future__ import annotations

import base64
import hashlib
import json
import shutil
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE_MODEL = Path(r"C:\Users\gamet\Downloads\Rift Splinter.bbmodel")
SOURCE_TEXTURE = Path(r"C:\Users\gamet\Downloads\texture_8453.png")
OUTPUT_MODEL = ROOT / "bbmodels/Rift_Splinter.bbmodel"
RESTORED_MODEL = ROOT / "bbmodels/Rift_Splinter_Restored.bbmodel"
OUTPUT_TEXTURE = ROOT / "src/main/resources/assets/riftborne/textures/entity/rift_splinter.png"


def main():
    shutil.copyfile(SOURCE_TEXTURE, OUTPUT_TEXTURE)

    model = json.loads(OUTPUT_MODEL.read_text(encoding="utf-8"))
    source_model = json.loads(SOURCE_MODEL.read_text(encoding="utf-8-sig"))

    # Restore Quinta's UV rectangles exactly while keeping the prepared bones
    # and scaled geometry.
    for source_element, output_element in zip(source_model["elements"], model["elements"]):
        for direction, source_face in source_element["faces"].items():
            output_element["faces"][direction]["uv"] = source_face["uv"]
            output_element["faces"][direction]["texture"] = 0

    original_texture = source_model["textures"][1].copy()
    original_texture.update({
        "name": "rift_splinter.png",
        "id": "0",
        "path": "../src/main/resources/assets/riftborne/textures/entity/rift_splinter.png",
        "relative_path": "entity/rift_splinter.png",
        "folder": "entity",
        "namespace": "riftborne",
        "source": "data:image/png;base64,"
        + base64.b64encode(SOURCE_TEXTURE.read_bytes()).decode("ascii"),
        "internal": True,
        "saved": True,
    })
    model["textures"] = [original_texture]

    restored_text = json.dumps(model, ensure_ascii=False, separators=(",", ":"))
    try:
        OUTPUT_MODEL.write_text(restored_text, encoding="utf-8")
    except OSError:
        # Blockbench can lock the currently opened .bbmodel on Windows.
        RESTORED_MODEL.write_text(restored_text, encoding="utf-8")
        print(f"Current model is locked; wrote {RESTORED_MODEL.relative_to(ROOT)}")

    source_hash = hashlib.sha256(SOURCE_TEXTURE.read_bytes()).hexdigest()
    output_hash = hashlib.sha256(OUTPUT_TEXTURE.read_bytes()).hexdigest()
    if source_hash != output_hash:
        raise RuntimeError("Restored texture does not match the original")

    print("Original Quinta texture and UV rectangles restored exactly.")


if __name__ == "__main__":
    main()
