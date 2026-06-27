package com.pr1tcha.riftborne.config;

import com.pr1tcha.riftborne.Riftborne;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = Riftborne.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue riftReactingRadius;
    public static final ModConfigSpec.IntValue riftCrackingRadius;
    public static final ModConfigSpec.IntValue riftOpeningRadius;
    public static final ModConfigSpec.IntValue riftOpeningDurationTicks;
    public static final ModConfigSpec.IntValue metaWearStableDecayInterval;
    public static final ModConfigSpec.IntValue metaWearStrainDecayInterval;
    public static final ModConfigSpec.IntValue metaWearDistortionDecayInterval;
    public static final ModConfigSpec.IntValue metaWearRejectionDecayInterval;
    public static final ModConfigSpec.DoubleValue basicRnaLoadDecayPerSecond;
    public static final ModConfigSpec.IntValue basicRnaOverloadWarningThreshold;
    public static final ModConfigSpec.IntValue basicRnaOverloadLockThreshold;
    public static final ModConfigSpec.IntValue basicRnaFocusCooldown;
    public static final ModConfigSpec.IntValue basicRnaImpulseStepCooldown;
    public static final ModConfigSpec.IntValue basicRnaOverloadVentCooldown;
    public static final ModConfigSpec.DoubleValue codexScanDistance;

    static {
        BUILDER.comment("Rift system settings").push("rifts");

        riftReactingRadius = BUILDER
                .comment("Distance in blocks where a dormant normal rift starts reacting to players")
                .defineInRange("reactingRadius", 16, 1, 128);

        riftCrackingRadius = BUILDER
                .comment("Distance in blocks where a normal rift grows visible side cracks")
                .defineInRange("crackingRadius", 10, 1, 128);

        riftOpeningRadius = BUILDER
                .comment("Distance in blocks where a normal rift begins its one-way opening burst")
                .defineInRange("openingRadius", 6, 1, 128);

        riftOpeningDurationTicks = BUILDER
                .comment("Duration of the normal rift opening burst before it becomes active")
                .defineInRange("openingDurationTicks", 60, 1, 1200);

        BUILDER.pop();

        BUILDER.comment("Codex settings").push("codex");

        codexScanDistance = BUILDER
                .comment("Maximum Pocket Codex scan distance in blocks")
                .defineInRange("scanDistance", 12.0D, 2.0D, 64.0D);

        BUILDER.pop();

        BUILDER.comment("RNA meta-wear settings").push("rna");

        metaWearStableDecayInterval = BUILDER
                .comment("Ticks required to passively remove one meta-wear point in STABLE")
                .defineInRange("stableDecayInterval", 200, 20, 72000);

        metaWearStrainDecayInterval = BUILDER
                .comment("Ticks required to passively remove one meta-wear point in STRAIN")
                .defineInRange("strainDecayInterval", 400, 20, 72000);

        metaWearDistortionDecayInterval = BUILDER
                .comment("Ticks required to passively remove one meta-wear point in DISTORTION")
                .defineInRange("distortionDecayInterval", 800, 20, 72000);

        metaWearRejectionDecayInterval = BUILDER
                .comment("Ticks required to passively remove one meta-wear point in REJECTION")
                .defineInRange("rejectionDecayInterval", 2400, 20, 72000);

        BUILDER.pop();

        BUILDER.comment("Basic pre-aspect RNA combat settings").push("basic_rna_combat");

        basicRnaLoadDecayPerSecond = BUILDER
                .comment("Resonant combat load removed per second while the player is not adding new load")
                .defineInRange("loadDecayPerSecond", 4.0D, 0.0D, 100.0D);

        basicRnaOverloadWarningThreshold = BUILDER
                .comment("Combat load where overload warnings and unstable skill behavior may begin")
                .defineInRange("overloadWarningThreshold", 75, 1, 100);

        basicRnaOverloadLockThreshold = BUILDER
                .comment("Combat load where new basic RNA skills are blocked")
                .defineInRange("overloadLockThreshold", 90, 1, 100);

        basicRnaFocusCooldown = BUILDER
                .comment("Cooldown for rna_focus in ticks")
                .defineInRange("rnaFocusCooldown", 80, 0, 72000);

        basicRnaImpulseStepCooldown = BUILDER
                .comment("Cooldown for rna_impulse_step in ticks")
                .defineInRange("rnaImpulseStepCooldown", 60, 0, 72000);

        basicRnaOverloadVentCooldown = BUILDER
                .comment("Cooldown for rna_overload_vent in ticks")
                .defineInRange("rnaOverloadVentCooldown", 400, 0, 72000);

        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // Config values are read directly from their ModConfigSpec entries.
    }
}
