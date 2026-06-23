# Riftborne

Riftborne is a Minecraft 1.21.1 NeoForge mod for the Riftborne modpack.

The current version is the first working foundation for rift events: a server-driven rift system with commands, configurable natural spawning, lifetime handling, area effects, visual feedback, and rewards.

It also includes the first RNA/Codex foundation:

- persistent per-player Resonant Neural Architecture data;
- formation paths and four 0-100 RNA stats;
- meta-wear stages, passive recovery, critical instability, and collapse;
- a placeable field laptop with a desktop and Codex GUI;
- an extensible Codex entry catalog and per-player unlock state;
- telekinesis integrated with active RNA stats and meta-wear.

The independent, data-driven Codex backend is configured through JSON/datapacks.
See [docs/CODEX_BACKEND_ENTRIES.md](docs/CODEX_BACKEND_ENTRIES.md) for the copy-and-edit entry template and test commands.

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

- `/riftborne rifts spawn`
- `/riftborne rifts spawn <pos>`
- `/riftborne rifts spawn <pos> <amount> <sec|t>`
- `/riftborne rifts spawn <pos> <amount> <sec|t> <radius>`
- `/riftborne contour spawn`
- `/riftborne contour spawn <pos>`
- `/riftborne contour spawn <pos> <amount> <sec|t>`
- `/riftborne contour spawn <pos> <amount> <sec|t> <radius>`
- `/riftborne rifts spawn_archived`
- `/riftborne rifts spawn_archived <pos>`
- `/riftborne rifts spawn_archived <pos> <amount> <sec|t>`
- `/riftborne rifts spawn_archived <pos> <amount> <sec|t> <radius>`
- `/riftborne rifts info`
- `/riftborne rifts info <searchRadius>`
- `/riftborne contour escape`
- `/riftborne rifts kill`
- `/riftborne rifts kill <radius>`
- `/riftborne rifts stage get`
- `/riftborne rifts stage set <stage>`

Rift, Contour, telekinesis mutation, RNA mutation, and meta-wear mutation commands require operator permission level 2.

RNA read commands are available to the executing player; mutation and test commands require operator permission level 2.

### RNA and Meta-wear Commands

- `/riftborne rna get [player]`
- `/riftborne rna profile init <TRAINING|STRESS|ARTIFICIAL_BORN> [target]`
- `/riftborne rna profile reset [target]`
- `/riftborne rna profile path set <TRAINING|STRESS|ARTIFICIAL_BORN> [target]`
- `/riftborne rna stats set <stat> <0-100> [target]`
- `/riftborne rna stats add <stat> <amount> [source] [target]`
- `/riftborne rna metawear get [target]`
- `/riftborne rna metawear set <0-100> [target]`
- `/riftborne rna metawear add <1-100> [source] [target]`
- `/riftborne rna metawear clear [target]`
- `/riftborne rna metawear collapse [target]`
- `/riftborne rna abilities list [target]`
- `/riftborne rna abilities grant <ability_id> [targets]`
- `/riftborne rna abilities revoke <ability_id> [targets]`
- `/riftborne rna abilities cooldown get <ability_id|all> [target]`
- `/riftborne rna abilities cooldown clear <ability_id|all> [targets]`
- `/riftborne rna abilities debug <ability_id> [target]`

`TECHNOLOGICAL` and `INTERSPATIAL` are reserved save-stable path IDs and cannot be selected yet.

Telekinesis is the first registered RNA combat ability. Grant it with
`/riftborne rna abilities grant riftborne:telekinesis`. It requires an active RNA profile.
Successful grabs and throws use the shared combat backend for meta-wear and protected stat growth.

`/riftborne rifts spawn` now uses the current procedural visual and saves as `riftborne:rift`.
`/riftborne contour spawn` creates a Discard Contour Rift that leads to `riftborne:discard_contour`.
`/riftborne rifts spawn_archived` keeps the old classic visual available as `riftborne:rift_archived`.

The Discard Contour is a trapping dimension: death respawns the player back inside the Contour, beds do not work, teleport commands are blocked for trapped players, the compass is unreliable because the dimension is non-natural, and no return rift is created at the arrival anchor.

Operators can use `/riftborne contour escape` as an emergency test command to leave the Discard Contour.

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

Use `/riftborne rifts stage set 1` to enable natural spawning after the relevant quest/progression step.

## Riftborne Direction

This mod should support the Riftborne progression loop:

`survival -> infrastructure -> old signals -> first record -> rift activation -> expeditions -> network recovery -> technological escalation -> collapse`

The mod should stay gameplay-first. Legion: Temporal Code is the lore foundation, but the first implementation should remain a clean Minecraft system that can later connect to FTB Quests, world stages, records, and deeper rift types.

## Near-Term Roadmap

1. Clean repository tracking and keep generated files out of Git.
2. Review the current rift architecture without rewriting it.
3. Add FTB Quests-friendly scripted event commands beyond the basic `/riftborne` tools.
4. Add the first simple record/log item system for Vitalik-related fragments.
5. Expand rift stages toward `UNSTABLE`, `COLLAPSING`, and `SCAR`.
