# Physical training

Physical condition is a world-level player system. It is deliberately stored outside RNA data so other systems can consume it later without becoming RNA mechanics.

## Four stats

| Stat | Natural activity | Focused equipment | Daily norm |
| --- | --- | --- | --- |
| Endurance | Sprinting (500 blocks = 100%) or swimming (300 blocks = 100%) | 45 seconds on the endurance station | 100% |
| Strength | Carrying the Training Weight (250 blocks = 100%) | 15 push-ups on the two-block Push-up Mat, or 15 strength-station repetitions | 100% |
| Motorics | Disabled outside an active station session | 12 meaningful moving jumps on the Motorics platform | 100% |
| Stability | — | 40 seconds crouched and aligned with the stability core | 100% |

Ordinary sprinting, swimming, and carrying the training weight award daily progress at full rate alongside dedicated equipment, so a player can reach the daily norm through either path. World traversal jumps still do not count toward Motorics; meaningful jumps are measured by the server only during an active platform session, and jumping in place or repeated landings on the same point do not count. Push-up input is accepted only while the player is fully lowered; raising, holding, and returning are server-controlled.

## Daily cycle

- Every stat has long-term condition `0..100`, initially `40`.
- Every stat also has daily progress `0..100`.
- At least `60%` maintains the stat.
- `100%` increases the long-term stat by `0.5` when the next active day begins.
- The first two missed active days are grace days.
- From the third missed active day onward the stat loses `0.5` per day.
- Offline world time is ignored. Sleeping while online completes the current active training day normally.

## RNA integration

The four long-term stats are averaged. Nothing else in RNA reads individual physical stats.

```text
overall condition = average(endurance, strength, motorics, stability)
RNA overload capacity = 75 + overall condition * 0.5
```

The existing RNA load HUD remains normalized to `0..100%`. Positive load, venting, and passive load recovery are converted through the capacity multiplier, so physical condition changes capacity only. It does not alter damage, technique cost, cooldown, barrier integrity, or RNA stat growth.

## Codex diagnostics

Rift OS contains a read-only Physical Status application. It displays overall condition, RNA overload capacity, and for each of the four stats: long-term condition, current daily norm progress, and missed-day count. The Codex does not award progress or start training.

Riftwalker integration is intentionally not implemented here. The physical profile is neutral and exposes a stable condition value for that future connection.

The first complete focused exercise is documented in `docs/PUSHUP_TRAINING.md`. Its successful repetitions feed this same Strength daily-progress channel and do not create a second stat system.
