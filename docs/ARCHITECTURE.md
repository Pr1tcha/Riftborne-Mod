# Riftborne Architecture Notes

This document describes the current v0.1 rift implementation as it exists in code.

## Package Layout

- `com.pr1tcha.riftborne`
  - Main mod entry point.
- `config`
  - Common mod configuration.
- `registry`
  - Blocks, items, entities, sounds, and creative tabs.
- `command`
  - Root `/riftborne` command tree.
- `rift`
  - Shared rift spawning, progression, and type logic.
- `rift.block`
  - Rift anchor block and block entity.
- `rift.data`
  - Serializable rift state and stages.
- `rift.command`
  - Rift operator and testing commands.
- `rift.contour`
  - Discard Contour generation, rules, and teleportation.
- `interspace`
  - RNA and Riftwalker interspace dimensions, terrain decoration, test access, and return-point handling.
- `rift.entity`
  - Rift-specific entities.
- `rift.client`
  - Rift renderers, procedural visuals, and distortion integration.
- `telekinesis`
  - Telekinesis gameplay, commands, and networking.
- `telekinesis.entity`
  - Telekinesis-specific entities.
- `telekinesis.client`
  - Telekinesis input and rendering.

## Core Classes

- `Riftborne`
  - Main NeoForge mod entry point.
  - Registers mod content, common config, and `/riftborne` commands.

- `ModContent`
  - Registers the invisible rift anchor block.
  - Registers the rift block entity type.

- `RiftCommand`
  - Provides operator commands for testing and quest scripting.
  - Current commands:
    - `/riftborne rifts spawn`
    - `/riftborne rifts spawn <pos>`
    - `/riftborne rifts spawn <pos> <amount> <sec|t>`
    - `/riftborne rifts spawn <pos> <amount> <sec|t> <radius>`
    - `/riftborne contour spawn`
    - `/riftborne rifts spawn_archived`
    - `/riftborne rifts info [searchRadius]`
    - `/riftborne contour escape`
    - `/riftborne rifts kill [radius]`
    - `/riftborne rifts stage get`
    - `/riftborne rifts stage set <stage>`

- `RiftSpawner`
  - Reserved as an empty integration point for the future natural spawning rework.
  - Natural rift spawning is currently disabled.

- `RiftWorldStage`
  - Registers the persistent `riftborneRiftStage` gamerule.
  - Stage `0`: natural rifts disabled.
  - Stage `1`: weak natural rifts enabled.

- `RiftBlock`
  - Invisible, non-colliding rift anchor block.
  - Provides the block entity ticker.

- `RiftBlockEntity`
  - Owns runtime rift behavior.
  - Advances lifetime.
  - Switches stages.
  - Applies player trigger behavior.
  - Spawns and tracks rift minions.
  - Handles collapse behavior.
  - Saves and loads `RiftData`.
  - Current rifts use type `riftborne:rift`; archived classic-visual rifts use `riftborne:rift_archived`.
  - Discard Contour Rifts use type `riftborne:discard_contour_rift` and lead to `riftborne:discard_contour`.
  - Legacy `riftborne:rift_portal` data is loaded as a Discard Contour Rift.

- `RiftSpawnLocator`
  - Shared volume validator and position finder for normal rifts and Discard Contour Rifts.
  - Rejects solid blocks, liquids, ceilings, cramped spaces, and unsupported placement.

- `RiftContourTeleporter`
  - Handles Discard Contour entry through an active Contour Rift.
  - Sends players falling below the Overworld void into the Discard Contour.
  - Builds the arrival anchor platform inside the Discard Contour without a return rift.
  - Provides the admin/tester emergency escape used by `/riftborne contour escape`.

- `InterspaceDimensions`
  - Declares `riftborne:rna_interspace` and `riftborne:riftwalker_interspace`.
  - Stores the player's entry dimension and coordinates before transit.
  - Builds a safe arrival platform and returns the player to the stored origin.

- `InterspaceFeatures`
  - Gives both interspaces independent terrain palettes and floating formations.
  - RNA Interspace uses a restrained blue/cyan palette.
  - Riftwalker Interspace uses a brighter violet/lilac palette and more floating fragments.

- `InterspaceCommand`
  - Temporary development access:
    - `/riftborne interspace rna`
    - `/riftborne interspace riftwalker`
    - `/riftborne interspace return`
  - These commands are test scaffolding, not the final lore-facing entry mechanic.

- `DiscardContourRules`
  - Keeps player death respawns inside the Discard Contour.
  - Blocks teleport commands for players trapped inside the Contour.
  - Explicitly denies sleeping in the Contour; the dimension type also has `bed_works: false`.
  - Compass behavior is intentionally unreliable through the Discard Contour dimension type's `natural: false`.

- `RiftBlockEntityRenderer`
  - Client-side renderer for the visible rift.
  - Draws layered translucent/emissive vertical tear planes.
  - Uses `story_rift.png` as a temporary purple story-rift texture.
  - Scales and pulses by rift stage.

- `RiftData`
  - Serializable rift state:
    - id
    - type
    - center position
    - radius
    - stage
    - lifetime
    - instability
    - command/quest flags
    - wave counters
    - spawn cooldown

- `RiftStage`
  - Current stages are:
    - `OPENING`
    - `ACTIVE`
    - `UNSTABLE`
    - `COLLAPSING`
    - `SCAR`

## Current Gameplay Flow

1. A rift anchor appears by command or a scripted system.
2. The rift starts in `OPENING`.
3. When a player enters the radius:
   - the rift switches to `ACTIVE`;
   - the player receives darkness;
   - the rift starts combat behavior.
4. During `ACTIVE`, the rift spawns waves of tagged endermites.
5. Near the end of its lifetime, an active rift enters `UNSTABLE`.
6. After three cleared waves, the rift enters `COLLAPSING`.
7. On successful collapse, it drops `Rift Shard` items and turns into obsidian as a simple scar.
8. If lifetime expires before completion, the rift disappears and cleans up its minions.

## Visual Direction

The first visual pass is a purple story rift inspired by Vitalik:

- dark vertical tear;
- purple/white edges;
- translucent layered cross-planes;
- pulsing stage-dependent scale;
- stronger flicker and lightning slash during `UNSTABLE` / `COLLAPSING`.

This is intentionally shader-free for now. Future rift types can swap texture/color/scale behavior before adding optional screen-space distortion.

## Config

Natural spawning currently has no config keys. The remaining `rifts` settings
control the player-triggered activation behavior of existing rifts.

## Known Gaps

- Natural spawning is disabled pending a full rework.
- Stage changes are command-driven and ready for FTB Quests command actions.
- `SCAR` exists as a stage enum, but the current scar implementation is still represented by an obsidian block after collapse.
- Rewards are currently hardcoded as `Rift Shard`, not a dedicated reward table.
- Command messages and comments still need cleanup/localization.
- FTB Quests integration is not formalized yet, but commands are already usable as quest actions.

## Next Intended Step

Connect Stage 1 to the first Riftborne quest unlock, then add scripted rift events and records/logs.
