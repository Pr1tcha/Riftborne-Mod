package com.pr1tcha.riftborne.config;

import com.pr1tcha.riftborne.Riftborne;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = Riftborne.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue riftCheckInterval;
    public static final ModConfigSpec.DoubleValue riftSpawnChance;
    public static final ModConfigSpec.IntValue riftMinRadius;
    public static final ModConfigSpec.IntValue riftMaxRadius;
    public static final ModConfigSpec.IntValue riftDefaultLifetime;
    public static final ModConfigSpec.IntValue riftLifetimeVariationPercent;
    public static final ModConfigSpec.IntValue riftReactingRadius;
    public static final ModConfigSpec.IntValue riftCrackingRadius;
    public static final ModConfigSpec.IntValue riftOpeningRadius;
    public static final ModConfigSpec.IntValue riftOpeningDurationTicks;
    public static final ModConfigSpec.IntValue metaWearStableDecayInterval;
    public static final ModConfigSpec.IntValue metaWearStrainDecayInterval;
    public static final ModConfigSpec.IntValue metaWearDistortionDecayInterval;
    public static final ModConfigSpec.IntValue metaWearRejectionDecayInterval;

    static {
        BUILDER.comment("Rift system settings").push("rifts");

        riftCheckInterval = BUILDER
                .comment("Rift spawn check interval in ticks")
                .defineInRange("checkInterval", 1200, 20, 72000);

        riftSpawnChance = BUILDER
                .comment("Rift spawn chance per check (from 0.0 to 1.0, where 0.15 = 15%)")
                .defineInRange("spawnChance", 0.01, 0.0, 1.0);

        riftMinRadius = BUILDER
                .comment("Minimum distance in blocks from the player where a rift can open")
                .defineInRange("minRadiusFromPlayer", 30, 10, 128);

        riftMaxRadius = BUILDER
                .comment("Maximum distance in blocks from the player for a rift to open")
                .defineInRange("maxRadiusFromPlayer", 70, 20, 256);

        riftDefaultLifetime = BUILDER
                .comment("Default rift lifetime in ticks")
                .defineInRange("defaultLifetimeTicks", 8000, 200, 1200000);

        riftLifetimeVariationPercent = BUILDER
                .comment("Rift lifetime variance percentage from 0 to 100")
                .defineInRange("lifetimeVariationPercent", 20, 0, 100);

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
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // Config values are read directly from their ModConfigSpec entries.
    }
}
