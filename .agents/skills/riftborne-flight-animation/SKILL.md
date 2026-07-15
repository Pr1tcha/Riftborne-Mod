---
name: riftborne-flight-animation
description: Design and implement Riftborne player flight animations inspired by Palladium 1.20.1 and Fisk's Superheroes 1.7.10, including hover, levitation, boosted flight, camera roll, first-person arm handling, and NeoForge client rendering hooks.
---

# Riftborne Flight Animation

Use this skill when implementing or reviewing player flight animation for the Riftborne Minecraft 1.21.1 NeoForge mod.

## Local Context

- Project package: `com.pr1tcha.riftborne`.
- Current flight code lives in `src/main/java/com/pr1tcha/riftborne/flight/`.
- Current client flight entry point: `src/main/java/com/pr1tcha/riftborne/flight/client/FlightClient.java`.
- Palladium source reference is available under `build/palladium-src`; treat it as reference only.
- Fisk's Superheroes source is not currently present in this workspace. If it is added later, search for player render mixins, flight handlers, keybind state, and model part transforms before designing changes.

## Required References

Before changing animation code, read these Palladium references:

- `build/palladium-src/common/src/main/java/net/threetag/palladium/entity/FlightHandler.java`
- `build/palladium-src/common/src/main/java/net/threetag/palladium/client/model/animation/FlightAnimation.java`
- `build/palladium-src/common/src/main/java/net/threetag/palladium/client/model/animation/HoveringAnimation.java`
- `build/palladium-src/common/src/main/java/net/threetag/palladium/client/model/animation/LevitationAnimation.java`
- `build/palladium-src/common/src/main/java/net/threetag/palladium/client/model/animation/PalladiumAnimationRegistry.java`
- `build/palladium-src/common/src/main/java/net/threetag/palladium/mixin/client/PlayerRendererMixin.java`
- `build/palladium-src/common/src/main/java/net/threetag/palladium/mixin/PlayerMixin.java`

Also read the current Riftborne flight files:

- `src/main/java/com/pr1tcha/riftborne/flight/RiftFlightController.java`
- `src/main/java/com/pr1tcha/riftborne/flight/FlightNetwork.java`
- `src/main/java/com/pr1tcha/riftborne/flight/client/FlightClient.java`
- `src/main/java/com/pr1tcha/riftborne/flight/FlightSettings.java`

## Animation Model

Keep these animation channels separate:

- `hover`: active flight with no forward boost; upright pose, loose limbs, subtle sine bob.
- `levitation`: slow horizontal movement below boosted flight; slight body pitch and restrained limb drift.
- `flight`: boosted forward movement; body pitches toward horizontal, legs trail, arms settle into normal or heroic pose.
- `bank`: derived from horizontal angle between `flightVector` and look direction; affects body/camera roll.
- `firstPerson`: arm-only adjustment; do not blindly reuse third-person body transforms.

Palladium-like values worth preserving conceptually:

- Hover pose uses sinusoidal limb variation from tick time.
- Boosted flight starts once boost exceeds the regular flight tier, not merely whenever flight is active.
- Body roll comes from signed horizontal angle between motion vector and look vector.
- Camera roll should be smaller than model roll and eased in/out.
- Heroic flight can raise the main arm while normal flight keeps arms lower and slightly back.

## Implementation Shape For Riftborne

Prefer this staged approach:

1. Extend `VisualStatePayload` or add a compact animation payload carrying active/hover/levitation/boost values, `flightVector`, and maybe look vector or bank.
2. Store interpolated client-side animation state by entity id in a client-only class.
3. Implement third-person model transforms through a small dedicated client renderer/animation helper.
4. Add first-person arm transforms only after third-person animation is stable.
5. Add camera roll as an optional config/profile setting, because some users dislike roll.

Avoid enabling vanilla elytra physics for animation. It is acceptable to use `Pose.FALL_FLYING` as a temporary visual hint, but the full animation pass should eventually drive model parts directly.

## NeoForge Notes

- Keep client-only code behind `Dist.CLIENT` subscribers or classes only loaded from client paths.
- If NeoForge render events are not enough to alter model parts at the right point, add minimal mixins rather than broad renderer rewrites.
- If using mixins, add only the smallest injection points needed:
  - after `PlayerModel.setupAnim` for model part transforms,
  - around first-person hand render for arm transforms,
  - around camera angle computation for roll.
- Make animation state server-authoritative enough for other players to see it, but keep interpolation and easing client-side.

## Verification

After implementation:

- Run `./gradlew.bat build`.
- Launch `./gradlew.bat runClient`.
- Test hover, slow flight, sprint boost, release, landing, first-person, third-person front/back view.
- Confirm landing disables active flight and clears animation state.
- Confirm boosted pose does not leave the player stuck in a fall-flying pose after landing, logout, death, dimension change, or ability revoke.
