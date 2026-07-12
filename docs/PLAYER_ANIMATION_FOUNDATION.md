# Player animation foundation

Riftborne keeps the vanilla `PlayerModel` and extends it with two client libraries:

- Player Animation Library `1.1.4+mc.1.21.1` provides the shared player-animation pipeline.
- Bendable Cuboids supplies deformable vanilla model cubes for elbows, knees, torso bends,
  outer skin layers, held-item transforms, and future procedural poses.

The runtime jars live in `libs/` and are declared as required client dependencies in the
generated NeoForge metadata. Riftborne render code must access cube deformation through
`client.animation.RiftborneBends`; gameplay features should not depend on Bendable Cuboids
implementation classes directly.

## Bendable Cuboids 1.21.1 build provenance

The jar was built from the official `PlayerAnimationLibrary/BendableCuboids` repository,
branch `1.21.1`, commit `46ce7a446327c0b90645a2b6851810e7792ac9a5` (`Port to 1.21.1`).
The upstream branch required two compatibility corrections before its NeoForge build passed:

1. `bendable_cuboids.accesswidener`: `CubeDefinition.dimensions` uses
   `Lorg/joml/Vector3f;`, not `Lorg/joml/Vector3fc;` on this mapping set.
2. `BendableCuboidsModNeo`: the optional 3D Skin Layers check uses
   `ModList.get().isLoaded("skinlayers3d")` instead of the newer
   `FMLLoader.getCurrent()` API.

The resulting jar SHA-256 is
`F66050812270BAE3A124EBB5A1D4A2120C94AD6DA7E6B23925B51175C878FFB2`.
The official Player Animation Library jar SHA-256 is
`07A153F1A285FE2C6E59B9E929DF72053FF44AA58D439718D02CAAC553DA4A1D`.

## Current integration

Push-ups are the first consumer. The existing server-authoritative exercise state still owns
timing and progress, while the client pose bends both arm cuboids and their outer sleeves.
The whole-body mat orientation and first-person camera remain Riftborne-controlled.
Push-up bends are applied from a dedicated `PlayerModel` mixin after Bendable Cuboids'
Player Animation Library sync, because Bendable resets player bends to zero when no PAL
animation is active.

Future animations should reuse the same foundation for Barrier gestures, physical exercises,
technique poses, and story sequences. Long-running or composable animations should be moved
onto Player Animation Library layers; `RiftborneBends` is intended for small procedural bends
and safe access to bendable cubes.

Armor geometry is not automatically deformable merely because the underlying player skin is.
Every armor pipeline must be visually checked before relying on elbow or knee deformation.
