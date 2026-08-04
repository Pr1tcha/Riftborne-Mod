"""Check that every craftable item is actually reachable, and that no recipe forms a cycle.

A crafting chain can dead-lock silently: an intermediate that needs a machine, where the machine
needs that same intermediate. Nothing errors — the item is simply impossible to obtain. Run this
after touching recipes.
"""
import glob
import io
import json
import os

D = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                 "src/main/resources/data/riftborne")

# Obtainable without any crafting recipe: world drops and machine output.
BASE = {
    "riftborne:rift_shard",            # rift collapse
    "riftborne:resonance_core",        # resonance stabilizer
    "riftborne:rna_conductor",         # smelted from the interspace vein
    "riftborne:damaged_codex_laptop",  # structure / chest loot
    "riftborne:rna_interspace_vein",   # mined
}


def ingredients(recipe):
    found = set()
    for value in list(recipe.get("key", {}).values()) + recipe.get("ingredients", []):
        if isinstance(value, dict) and "item" in value:
            found.add(value["item"])
    single = recipe.get("ingredient")
    if isinstance(single, dict) and "item" in single:
        found.add(single["item"])
    return found


def main():
    recipes = {}
    for path in glob.glob(os.path.join(D, "recipe", "*.json")):
        recipe = json.load(io.open(path, encoding="utf-8"))
        recipes.setdefault(recipe["result"]["id"], []).append(
            (ingredients(recipe), os.path.basename(path)))

    have = set(BASE)
    changed = True
    while changed:
        changed = False
        for output, variants in recipes.items():
            if output in have:
                continue
            for needed, _ in variants:
                if all(i.startswith("minecraft:") or i in have for i in needed):
                    have.add(output)
                    changed = True
                    break

    blocked = sorted(o for o in recipes if o not in have)
    print("recipes: %d" % len(recipes))
    if not blocked:
        print("OK: every result is reachable from base resources; no cycles")
        return 0
    print("UNREACHABLE (cycle or missing source):")
    for output in blocked:
        for needed, name in recipes[output]:
            modded = sorted(i for i in needed if not i.startswith("minecraft:"))
            print("   %s <- %s  [%s]" % (output, modded, name))
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
