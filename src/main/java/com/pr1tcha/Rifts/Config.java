package com.pr1tcha.Rifts;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = RiftborneRift.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue riftCheckInterval;
    public static final ModConfigSpec.DoubleValue riftSpawnChance;
    public static final ModConfigSpec.IntValue riftMinRadius;
    public static final ModConfigSpec.IntValue riftMaxRadius;
    public static final ModConfigSpec.IntValue riftDefaultLifetime;
    public static final ModConfigSpec.IntValue riftLifetimeVariationPercent;

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

        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // Config values are read directly from their ModConfigSpec entries.
    }
}
