# Push-up training vertical slice

## Player flow

1. Place the Push-up Mat. It appears as a one-block rolled fitness mat.
2. Shift-right-click the rolled mat to unfold it into the two-block training surface.
3. Right-click the unfolded mat to begin. Right-click it again to stop.
4. The server anchors the player at the center of the full mat and lowers them into the waiting position.
5. Press Space, LMB, or RMB once to push up.
6. Reaching the top records one repetition and adds `100 / 15` daily Strength progress.
7. After a short top hold, the player lowers automatically and waits for the next input. There are no rhythm windows, misses, or failure pauses.
8. Press Shift at any time to leave the mat and end the session.

The server owns timing, repetitions, failures, position locking, mat occupancy, and Strength rewards. Client input is only a request and cannot award progress directly.

## Vanilla player rendering

The exercise does not replace or cancel the normal player renderer. While a synchronized
session is active, a client mixin rotates the ordinary player model into a plank and applies
a phase-dependent pose to its head, body, arms and legs. Bendable Cuboids deforms the arm and
outer-sleeve cubes into actual elbows. The feet stay anchored at mat height; the repetition is
conveyed by rotating the plank around that lower support point and bending the elbows.

Because the original renderer stays active:

- the actual regular or slim skin remains intact;
- vanilla armor, held items and render layers remain visible;
- first-person arms are unaffected;
- no GeckoLib player geometry or animation resource is involved;
- the normal skin and its outer sleeve layer bend together.

The body's yaw is synchronized from the mat and remains fixed even when the local player
turns the camera. In first person, the camera is detached from the normal standing eye height
and moves vertically between `0.32` and `0.62` blocks above the mat with the repetition.

The animation foundation and dependency provenance are documented in
`docs/PLAYER_ANIMATION_FOUNDATION.md`.

## Assets

- Mat textures/models: `assets/riftborne/textures/block/pushup_mat*.png`,
  `models/block/pushup_mat_rolled.json`, `models/block/pushup_mat_foot.json`,
  and `models/block/pushup_mat_head.json`
- Generator: `tools/generate_pushup_training_assets.py`

The generator now owns only the mat assets and cannot recreate the removed Gecko player rig.
