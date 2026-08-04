"""Generate the Abandoned Field Post structure file.

This writes a real Minecraft `.nbt` structure template so the piece exists and generates in world.
It is a *starting point*, not final level design: open it with a structure block in game, rebuild it
by hand, and save over the same file. Nothing in the datapack cares how the blocks got there.

Structure NBT layout (gzipped, unnamed root compound):
    size     list of 3 ints
    palette  list of {Name, Properties?}
    blocks   list of {pos, state, nbt?}
    entities list
    DataVersion int
"""

import gzip
import os
import struct

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src/main/resources/data/riftborne/structure/abandoned_field_post.nbt")
DATA_VERSION = 3955  # 1.21.1

TAG_END, TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 3, 8, 9, 10


def _string(value):
    raw = value.encode("utf-8")
    return struct.pack(">H", len(raw)) + raw


def _named(tag_id, name, payload):
    return struct.pack(">B", tag_id) + _string(name) + payload


def _int(value):
    return struct.pack(">i", value)


def _compound(entries):
    """entries: list of already-encoded named tags."""
    return b"".join(entries) + struct.pack(">B", TAG_END)


def _list(tag_id, payloads):
    return struct.pack(">Bi", tag_id, len(payloads)) + b"".join(payloads)


def _int_list(values):
    return _list(TAG_INT, [_int(v) for v in values])


class Template:
    """Collects blocks and builds the palette on the fly."""

    def __init__(self):
        self.palette = []
        self.index = {}
        self.blocks = []

    def state_id(self, name, properties=None):
        key = (name, tuple(sorted((properties or {}).items())))
        if key in self.index:
            return self.index[key]
        entries = [_named(TAG_STRING, "Name", _string(name))]
        if properties:
            props = [_named(TAG_STRING, k, _string(v)) for k, v in sorted(properties.items())]
            entries.append(_named(TAG_COMPOUND, "Properties", _compound(props)))
        self.palette.append(_compound(entries))
        self.index[key] = len(self.palette) - 1
        return self.index[key]

    def set(self, x, y, z, name, properties=None, block_entity=None):
        entries = [
            _named(TAG_LIST, "pos", _int_list([x, y, z])),
            _named(TAG_INT, "state", _int(self.state_id(name, properties))),
        ]
        if block_entity:
            entries.append(_named(TAG_COMPOUND, "nbt", _compound(block_entity)))
        self.blocks.append(_compound(entries))

    def write(self, path, size):
        root = _compound([
            _named(TAG_LIST, "size", _int_list(list(size))),
            _named(TAG_LIST, "palette", _list(TAG_COMPOUND, self.palette)),
            _named(TAG_LIST, "blocks", _list(TAG_COMPOUND, self.blocks)),
            _named(TAG_LIST, "entities", _list(TAG_COMPOUND, [])),
            _named(TAG_INT, "DataVersion", _int(DATA_VERSION)),
        ])
        payload = _named(TAG_COMPOUND, "", root)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with gzip.open(path, "wb") as fh:
            fh.write(payload)
        print("wrote %s (%d blocks, %d palette entries)"
              % (os.path.relpath(path, ROOT), len(self.blocks), len(self.palette)))


# --- the post itself -------------------------------------------------------------------------
# A prefab shelter that has been standing empty long enough to start coming apart: intact floor,
# walls that have lost their upper courses on the weather side, a roof that only partly survived.
W, H, D = 11, 6, 9

FLOOR = "minecraft:polished_deepslate"
FLOOR_WORN = "minecraft:cracked_deepslate_tiles"
WALL = "minecraft:deepslate_bricks"
WALL_WORN = "minecraft:cracked_deepslate_bricks"
TRIM = "minecraft:weathered_copper"
ROOF = "minecraft:deepslate_tile_slab"
AIR = "minecraft:air"

t = Template()


def floor_material(x, z):
    # Wear concentrates near the doorway and the collapsed corner.
    if (x + z) % 5 == 0 or x >= W - 3 and z <= 2:
        return FLOOR_WORN
    return FLOOR


for x in range(W):
    for z in range(D):
        t.set(x, 0, z, floor_material(x, z))

# Walls. The north-east corner has come down, so the wall height drops off toward it.
for x in range(W):
    for z in range(D):
        on_edge = x in (0, W - 1) or z in (0, D - 1)
        if not on_edge:
            continue
        collapse = max(0, (x - 5) + (2 - z)) // 2  # taller damage toward the ruined corner
        height = max(0, 3 - collapse)
        for y in range(1, height + 1):
            material = WALL_WORN if (x * 3 + z * 7 + y) % 4 == 0 else WALL
            t.set(x, y, z, material)

# Doorway on the south face.
for y in (1, 2):
    t.set(5, y, D - 1, AIR)
t.set(4, 3, D - 1, TRIM)
t.set(5, 3, D - 1, TRIM)
t.set(6, 3, D - 1, TRIM)

# Window slits, already broken out on the damaged side.
for z in (3, 5):
    t.set(0, 2, z, "minecraft:iron_bars", {"east": "false", "north": "true",
                                           "south": "true", "west": "false", "waterlogged": "false"})

# What is left of the roof: the intact half only.
for x in range(W):
    for z in range(D):
        if x > 6 and z < 3:
            continue  # open to the sky above the collapse
        if (x + z * 3) % 7 == 0:
            continue  # panels that have fallen through
        t.set(x, 4, z, ROOF, {"type": "bottom", "waterlogged": "false"})

# Interior fittings. The chest is the point of the whole structure.
t.set(2, 1, 2, "minecraft:chest",
      {"facing": "south", "type": "single", "waterlogged": "false"},
      block_entity=[_named(TAG_STRING, "LootTable", _string("riftborne:chests/abandoned_field_post"))])
t.set(3, 1, 2, TRIM)
t.set(2, 1, 3, TRIM)
t.set(8, 1, 6, "minecraft:crafting_table")
t.set(7, 1, 6, "minecraft:barrel", {"facing": "up", "open": "false"})

# A monitoring mast, bent and still standing in the intact corner.
for y in range(1, 4):
    t.set(1, y, D - 2, "minecraft:iron_bars", {"east": "false", "north": "true",
                                               "south": "true", "west": "false", "waterlogged": "false"})
t.set(1, 4, D - 2, "minecraft:lightning_rod", {"facing": "up", "powered": "false"})

# Rubble from the collapse, spilled inside and out.
for x, z in ((7, 1), (8, 2), (9, 1), (6, 2), (8, 4), (9, 5)):
    t.set(x, 1, z, "minecraft:deepslate_tile_slab", {"type": "bottom", "waterlogged": "false"})

t.write(OUT, (W, H, D))
