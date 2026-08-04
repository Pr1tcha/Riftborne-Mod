"""Generate per-level worldgen for the five rift dimensions.

All five used to be superflat worlds that differed only in the block of the middle layer, so a
player could not tell L1 from L5 except by the item in hand. Each level now gets its own terrain
shape, palette and biome tint, following the depth ladder of PF09 — the further in, the less the
place behaves like ordinary matter and the closer it sits to the Limit.

    L1 Поверхностный осколок  fragments of ordinary rock torn loose and left hanging
    L2 Смещённый слой         strata that slid apart into offset plates
    L3 Узловой разлом         a junction: vertical columns in a lattice
    L4 Глубинный разлом       deep hollow rock, oppressive and cavernous
    L5 Предельный срез        near-empty void cut by one vast flat plane

Terrain comes from density functions, which are pure maths, so the shapes can be reasoned about
without opening the game. Positive density means solid.
"""

import io
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = os.path.join(ROOT, "src/main/resources/data/riftborne")

HEIGHT = 256


# --- density function helpers ------------------------------------------------------------------

def const(v):
    return {"type": "minecraft:constant", "argument": v}


def add(a, b):
    return {"type": "minecraft:add", "argument1": a, "argument2": b}


def mul(a, b):
    return {"type": "minecraft:mul", "argument1": a, "argument2": b}


def dmin(a, b):
    return {"type": "minecraft:min", "argument1": a, "argument2": b}


def dmax(a, b):
    return {"type": "minecraft:max", "argument1": a, "argument2": b}


def gradient(from_y, to_y, from_value, to_value):
    return {"type": "minecraft:y_clamped_gradient",
            "from_y": from_y, "to_y": to_y,
            "from_value": from_value, "to_value": to_value}


def noise(name, xz=1.0, y=1.0):
    return {"type": "minecraft:noise", "noise": name, "xz_scale": xz, "y_scale": y}


def flat(fn):
    """Collapse a function to 2D so it produces vertical columns rather than blobs."""
    return {"type": "minecraft:flat_cache", "argument": {"type": "minecraft:cache_2d", "argument": fn}}


def band(centre, half, peak=0.5, floor_value=-1.2):
    """A horizontal slab: peaks at `centre`, falls away over `half` blocks."""
    return dmin(
        gradient(centre - half, centre, floor_value, peak),
        gradient(centre, centre + half, peak, floor_value),
    )


# --- the five levels ---------------------------------------------------------------------------

def l1_surface_shard():
    """Islands: a broad band of allowed height, eaten into fragments by 3D noise."""
    return add(
        dmin(gradient(48, 72, -1.3, 0.45), gradient(104, 132, 0.45, -1.3)),
        mul(noise("minecraft:cave_cheese", 0.7, 0.9), const(0.75)),
    )


def l2_shifted_layer():
    """Three plates at different heights, each present only where its own noise allows."""
    plates = [
        (58, 6, "minecraft:continentalness"),
        (92, 5, "minecraft:erosion"),
        (126, 4, "minecraft:ridge"),
    ]
    result = None
    for centre, half, gate in plates:
        # The gate is 2D, so a plate is either present across a region or absent from it —
        # that is what reads as displacement rather than as noise.
        plate = add(band(centre, half, 0.6), mul(flat(noise(gate, 0.35, 0.0)), const(0.9)))
        result = plate if result is None else dmax(result, plate)
    return result


def l3_node_rift():
    """A lattice: columns that run the full height, plus a floor to stand on."""
    columns = add(
        mul({"type": "minecraft:abs", "argument": flat(noise("minecraft:ridge", 1.6, 0.0))},
            const(2.2)),
        const(-0.62),
    )
    # Taper the columns off before the ceiling so the level does not become a solid block.
    shaped = dmin(columns, gradient(196, 232, 0.6, -1.4))
    floor = band(20, 12, 0.8)
    return dmax(shaped, floor)


def l4_deep_rift():
    """Solid rock carved by large cavities: the level is read from the inside."""
    solid = dmin(gradient(4, 24, -1.0, 0.9), gradient(180, 216, 0.9, -1.2))
    caves = mul(noise("minecraft:cave_cheese", 0.55, 0.45), const(1.35))
    return add(solid, mul(caves, const(-1.0)))


def l5_limit_slice():
    """Almost nothing: one vast plane cut through the void, barely perturbed."""
    plane = band(128, 3, 0.9)
    tremor = mul(noise("minecraft:ridge", 0.25, 0.0), const(0.18))
    return add(plane, flat(tremor))


LEVELS = [
    {
        "path": "rift_l1_surface_shard",
        "vein_size": 6, "vein_count": 8,
        "default_block": "riftborne:rift_l1_stone",
        "surface": "riftborne:rift_l1_crust",
        "density": l1_surface_shard(),
        "sky": 0x0F1622, "fog": 0x1A2B3D, "water": 0x2F6E8C, "water_fog": 0x11202B,
    },
    {
        "path": "rift_l2_shifted_layer",
        "vein_size": 7, "vein_count": 10,
        "default_block": "riftborne:rift_l2_stone",
        "surface": "riftborne:rift_l2_crust",
        "density": l2_shifted_layer(),
        "sky": 0x0C1018, "fog": 0x231C33, "water": 0x3B5A78, "water_fog": 0x0E1722,
    },
    {
        "path": "rift_l3_node_rift",
        "vein_size": 8, "vein_count": 12,
        "default_block": "riftborne:rift_l3_stone",
        "surface": "riftborne:rift_l3_crust",
        "density": l3_node_rift(),
        "sky": 0x090A12, "fog": 0x2C2140, "water": 0x4A3A7A, "water_fog": 0x140F22,
    },
    {
        "path": "rift_l4_deep_rift",
        "vein_size": 9, "vein_count": 14,
        "default_block": "riftborne:rift_l4_stone",
        "surface": "riftborne:rift_l4_crust",
        "density": l4_deep_rift(),
        "sky": 0x05060B, "fog": 0x1B1030, "water": 0x2A1E4E, "water_fog": 0x0A0716,
    },
    {
        "path": "rift_l5_limit_slice",
        "vein_size": 5, "vein_count": 6,
        "default_block": "riftborne:rift_l5_stone",
        "surface": "riftborne:rift_l5_crust",
        "density": l5_limit_slice(),
        "sky": 0x000000, "fog": 0x3A0E3A, "water": 0x51216B, "water_fog": 0x1A0620,
    },
]


def write(path, obj):
    full = os.path.join(DATA, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    json.dump(obj, io.open(full, "w", encoding="utf-8"), ensure_ascii=False, indent=2)


def noise_settings(level):
    zero = const(0.0)
    return {
        "sea_level": -64,
        "disable_mob_generation": False,
        "aquifers_enabled": False,
        "ore_veins_enabled": False,
        "legacy_random_source": False,
        "default_block": {"Name": level["default_block"]},
        "default_fluid": {"Name": "minecraft:air"},
        "noise": {"min_y": 0, "height": HEIGHT, "size_horizontal": 1, "size_vertical": 2},
        "noise_router": {
            "barrier": zero,
            "fluid_level_floodedness": zero,
            "fluid_level_spread": zero,
            "lava": zero,
            "temperature": zero,
            "vegetation": zero,
            "continents": zero,
            "erosion": zero,
            "depth": zero,
            "ridges": zero,
            "initial_density_without_jaggedness": zero,
            "final_density": level["density"],
            "vein_toggle": zero,
            "vein_ridged": zero,
            "vein_gap": zero,
        },
        "spawn_target": [],
        "surface_rule": {
            "type": "minecraft:condition",
            "if_true": {"type": "minecraft:stone_depth", "offset": 0, "surface_type": "floor",
                        "add_surface_depth": False, "secondary_depth_range": 0},
            "then_run": {"type": "minecraft:block", "result_state": {"Name": level["surface"]}},
        },
    }


def vein_features(level):
    """Vein pockets seeded through the level's own rock, so mining has a target."""
    tag = level["path"].split("_")[1]
    stone = "riftborne:rift_%s_stone" % tag
    vein_block = "riftborne:rift_%s_vein" % tag
    write("worldgen/configured_feature/%s_vein.json" % level["path"], {
        "type": "minecraft:ore",
        "config": {
            "discard_chance_on_air_exposure": 0.0,
            "size": level["vein_size"],
            "targets": [{
                "target": {"predicate_type": "minecraft:block_match", "block": stone},
                "state": {"Name": vein_block},
            }],
        },
    })
    write("worldgen/placed_feature/%s_vein.json" % level["path"], {
        "feature": "riftborne:%s_vein" % level["path"],
        "placement": [
            {"type": "minecraft:count", "count": level["vein_count"]},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {
                "type": "minecraft:uniform",
                "min_inclusive": {"absolute": 0},
                "max_inclusive": {"absolute": HEIGHT - 8},
            }},
            {"type": "minecraft:biome"},
        ],
    })


def biome(level):
    return {
        "temperature": 0.5,
        "downfall": 0.0,
        "has_precipitation": False,
        "effects": {
            "sky_color": level["sky"],
            "fog_color": level["fog"],
            "water_color": level["water"],
            "water_fog_color": level["water_fog"],
            "mood_sound": {
                "sound": "minecraft:ambient.cave",
                "tick_delay": 6000,
                "block_search_extent": 8,
                "offset": 2.0,
            },
        },
        "spawners": {"monster": [], "creature": [], "ambient": [],
                     "axolotls": [], "underground_water_creature": [],
                     "water_creature": [], "water_ambient": [], "misc": []},
        "spawn_costs": {},
        "carvers": {},
        "features": [[] if step != 6 else ["riftborne:%s_vein" % level["path"]]
                     for step in range(11)],
    }


for lv in LEVELS:
    name = lv["path"]
    write("worldgen/noise_settings/%s.json" % name, noise_settings(lv))
    vein_features(lv)
    write("worldgen/biome/%s.json" % name, biome(lv))
    write("dimension/%s.json" % name, {
        "type": "riftborne:rift_dimension",
        "generator": {
            "type": "minecraft:noise",
            "settings": "riftborne:" + name,
            "biome_source": {"type": "minecraft:fixed", "biome": "riftborne:" + name},
        },
    })
    print("wrote", name)
