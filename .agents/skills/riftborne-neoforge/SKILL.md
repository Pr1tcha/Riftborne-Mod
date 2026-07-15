---
name: riftborne-neoforge
description: Work on the Riftborne Minecraft 1.21.1 NeoForge mod, especially gameplay systems, dimensions, networking, client rendering, and Palladium/Fisk-like superhero movement such as flight.
---

# Riftborne NeoForge

Use this skill when changing or designing systems in the Riftborne mod.

## Project Facts

- Minecraft: 1.21.1.
- Loader/API: NeoForge via `net.neoforged.moddev`.
- Java: 21.
- Mod id: `riftborne`.
- Base package: `com.pr1tcha.riftborne`.
- Local runtime jars live in `libs/`: Veil, GeckoLib, Curios.
- Palladium source reference may be present under `build/palladium-src`; treat it as reference material, not code to edit.

## Code Map

- `Riftborne` is the mod entry point and registers content, config, commands, networking, and client renderers.
- `registry/ModContent.java` owns blocks, items, entities, block entities, sounds, creative tabs, and attributes.
- `telekinesis/` contains server gameplay, client input, custom payload networking, and moving entities/blocks.
- `rift/` contains rift spawning, stages, data, commands, block entity behavior, rendering, and contour logic.
- `interspace/` contains custom dimension keys, transfer helpers, commands, and dimension terrain decoration.
- `src/main/resources/assets/riftborne` contains client assets, blockstates, models, lang, sounds, textures, and Veil/Pinwheel shader config.
- `src/main/resources/data/riftborne` contains dimensions, dimension types, and worldgen JSON.

## Development Rules

- Prefer NeoForge 1.21.1 APIs already used in the project.
- Keep client-only classes behind `Dist.CLIENT` subscribers or client-only call paths.
- Make server authoritative for gameplay state, movement permission, damage, cooldowns, and unlock checks.
- Use custom payloads for client input; keep payloads tiny and validate server-side.
- Do not enable vanilla creative flight as the core mechanic unless it is only a temporary debug aid.
- Preserve config and command-driven testability where possible.
- Keep resource JSON names aligned with registered ids.
- Run `./gradlew.bat build` or at least `./gradlew.bat classes` after code changes when practical.

## Flight Feel Target

For Palladium 1.20.1 / Fisk's Superheroes 1.7.10 style flight, aim for an authored movement controller instead of raw vanilla flight.

Core feel:
- Toggle/activate flight through an ability state, not creative abilities.
- Hover when active without forward input.
- Jump rises while hovering; crouch descends.
- Forward input accelerates toward the look vector.
- Sprint plus forward input ramps into a faster boost tier.
- Releasing forward decelerates quickly but not instantly.
- Steering should have limited turn flexibility, so the player banks and arcs instead of snapping.
- Clear fall distance while flight is active.
- Reset flight on ground, swimming, elytra flight, dimension change, death, or ability loss.

Useful Palladium reference concepts:
- `flightBoost` ranges roughly from hover/slow flight to fast flight.
- Normal forward ramp can be slow, sprint ramp faster.
- A persistent `flightVector` lerps toward the look vector with a capped per-tick steering delta.
- `verticalHover` ramps from negative to positive for crouch/jump hover control.
- Camera/model roll can be derived from the horizontal angle between `flightVector` and look direction.

Implementation shape for Riftborne:
- Add a `flight` package rather than mixing this into telekinesis.
- Add `FlightNetwork` payloads for toggle and possibly per-tick compact input state.
- Add `FlightAbility` or `RiftFlightController` with per-player state maps keyed by UUID.
- Add `FlightClient` for keybinds and optional camera/overlay feedback.
- Consider custom NeoForge attributes for speed/flexibility only when the first hardcoded prototype feels right.
- Start with one debug command or keybind-gated prototype, then connect it to progression/items later.

