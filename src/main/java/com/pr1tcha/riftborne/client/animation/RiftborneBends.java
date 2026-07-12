package com.pr1tcha.riftborne.client.animation;

import com.zigythebird.bendable_cuboids.api.BendableCube;
import com.zigythebird.bendable_cuboids.api.BendableModelPart;
import net.minecraft.client.model.geom.ModelPart;

/**
 * Riftborne-facing bridge to Bendable Cuboids.
 *
 * <p>Gameplay render code should use this class instead of depending on the
 * library's implementation helpers. A vanilla or third-party model part that
 * was not baked as bendable is safely ignored.</p>
 */
public final class RiftborneBends {
    private RiftborneBends() {
    }

    public static boolean apply(ModelPart part, float radians) {
        if (!((Object) part instanceof BendableModelPart bendablePart)) {
            return false;
        }
        BendableCube cube = bendablePart.bc$getCuboid(0);
        if (cube == null) {
            return false;
        }
        cube.applyBend(radians);
        return true;
    }
}
