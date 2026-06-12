# Riftborne Rift Architecture Notes

This document describes the current v0.1 rift implementation as it exists in code.

## Core Classes

- `RiftborneRift`
  - Main NeoForge mod entry point.
  - Registers mod content, common config, and `/rift` commands.

- `ModContent`
  - Registers the invisible rift anchor block.
  - Registers the rift block entity type.

- `RiftCommand`
  - Provides operator commands for testing and quest scripting.
  - Current commands:
    - `/rift spawn`
    - `/rift spawn <pos>`
    - `/rift spawn <pos> <amount> <sec|t>`
    - `/rift spawn <pos> <amount> <sec|t> <radius>`
    - `/rift info [searchRadius]`
    - `/rift kill [radius]`
    - `/rift stage get`
    - `/rift stage set <stage>`

- `RiftSpawner`
  - Handles natural rift spawn checks on overworld server ticks.
  - Uses config values for interval, chance, spawn distance, lifetime, and lifetime variance.
  - Only runs when `riftborneRiftStage` is at least `1`.

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

1. A rift anchor appears by command or natural spawn.
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

## Config

Config keys live under `rifts`:

- `checkInterval`
- `spawnChance`
- `minRadiusFromPlayer`
- `maxRadiusFromPlayer`
- `defaultLifetimeTicks`
- `lifetimeVariationPercent`

## Known Gaps

- World stage gating currently controls natural spawning.
- Stage changes are command-driven and ready for FTB Quests command actions.
- `SCAR` exists as a stage enum, but the current scar implementation is still represented by an obsidian block after collapse.
- Rewards are currently hardcoded as `Rift Shard`, not a dedicated reward table.
- Command messages and comments still need cleanup/localization.
- FTB Quests integration is not formalized yet, but commands are already usable as quest actions.

## Next Intended Step

Connect Stage 1 to the first Riftborne quest unlock, then add scripted rift events and records/logs.
