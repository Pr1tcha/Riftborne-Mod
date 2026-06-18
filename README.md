# Riftborne

Riftborne is a Minecraft 1.21.1 NeoForge mod for the Riftborne modpack.

The current version is the first working foundation for rift events: a server-driven rift system with commands, configurable natural spawning, lifetime handling, area effects, visual feedback, and rewards.

## Current Scope

- Spawns rifts by command.
- Supports natural rift spawning near players through config.
- Places a rift anchor block in the world.
- Stores rift data in a block entity.
- Tracks lifetime, radius, stage, instability, and rift type.
- Applies effects to nearby players.
- Shows rift info through commands.
- Removes rifts through commands.
- Drops `Rift Shard` rewards and leaves a simple scar after successful collapse.

## Commands

- `/rift spawn`
- `/rift spawn <pos>`
- `/rift spawn <pos> <amount> <sec|t>`
- `/rift spawn <pos> <amount> <sec|t> <radius>`
- `/rift spawn_contour`
- `/rift spawn_contour <pos>`
- `/rift spawn_contour <pos> <amount> <sec|t>`
- `/rift spawn_contour <pos> <amount> <sec|t> <radius>`
- `/rift spawn_portal`
- `/rift spawn_portal <pos>`
- `/rift spawn_portal <pos> <amount> <sec|t>`
- `/rift spawn_portal <pos> <amount> <sec|t> <radius>`
- `/rift spawn_archived`
- `/rift spawn_archived <pos>`
- `/rift spawn_archived <pos> <amount> <sec|t>`
- `/rift spawn_archived <pos> <amount> <sec|t> <radius>`
- `/rift info`
- `/rift info <searchRadius>`
- `/rift contour escape`
- `/rift kill`
- `/rift kill <radius>`
- `/rift stage get`
- `/rift stage set <stage>`

All commands require operator permission level 2.

`/rift spawn` now uses the current procedural visual and saves as `riftborne:rift`.
`/rift spawn_contour` creates a Discard Contour Rift that leads to `riftborne:discard_contour`.
`/rift spawn_portal` is kept only as a legacy alias for existing scripts.
`/rift spawn_archived` keeps the old classic visual available as `riftborne:rift_archived`.

The Discard Contour is a trapping dimension: death respawns the player back inside the Contour, beds do not work, teleport commands are blocked for trapped players, the compass is unreliable because the dimension is non-natural, and no return rift is created at the arrival anchor.

Operators can use `/rift contour escape` as an emergency test command to leave the Discard Contour.

Rift spawn commands search for a valid clear volume near the source when no position is provided. Explicit positions are also validated so rifts do not appear inside solid blocks, liquids, ceilings, or cramped spaces.

## Config

The common config controls the current natural rift spawning behavior:

- `rifts.checkInterval`
- `rifts.spawnChance`
- `rifts.minRadiusFromPlayer`
- `rifts.maxRadiusFromPlayer`
- `rifts.defaultLifetimeTicks`
- `rifts.lifetimeVariationPercent`

Successful rift collapse currently drops `Rift Shard` items. Failed/expired rifts disappear without rewards.

Natural spawning is gated behind the persistent `riftborneRiftStage` gamerule:

- Stage 0: natural rifts disabled.
- Stage 1+: weak natural rifts enabled.

Use `/rift stage set 1` to enable natural spawning after the relevant quest/progression step.

## Riftborne Direction

This mod should support the Riftborne progression loop:

`survival -> infrastructure -> old signals -> first record -> rift activation -> expeditions -> network recovery -> technological escalation -> collapse`

The mod should stay gameplay-first. Legion: Temporal Code is the lore foundation, but the first implementation should remain a clean Minecraft system that can later connect to FTB Quests, world stages, records, and deeper rift types.

## Near-Term Roadmap

1. Clean repository tracking and keep generated files out of Git.
2. Review the current rift architecture without rewriting it.
3. Add FTB Quests-friendly scripted event commands beyond the basic `/rift` tools.
4. Add the first simple record/log item system for Vitalik-related fragments.
5. Expand rift stages toward `UNSTABLE`, `COLLAPSING`, and `SCAR`.
