package com.pr1tcha.Rifts;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = Eventmod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue riftCheckInterval;
    public static final ModConfigSpec.DoubleValue riftSpawnChance;
    public static final ModConfigSpec.IntValue riftMinRadius;
    public static final ModConfigSpec.IntValue riftMaxRadius;
    public static final ModConfigSpec.IntValue riftDefaultLifetime;
    public static ModConfigSpec.IntValue riftLifetimeVariationPercent;

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
                .comment("Rift lifetime variance percentage (from 0 to 100) For example, 20 means a random deviation of ±20% from the base lifetime")
                .defineInRange("lifetimeVariationPercent", 20, 0, 100);

        BUILDER.pop();
    }

    // --- СТАНДАРТНАЯ СЕКЦИЯ (Из шаблона) ---
    private static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER.comment("Whether to log the dirt block on common setup").define("logDirtBlock", true);
    private static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER.comment("A magic number").defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);
    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER.comment("What you want the introduction message to be for the magic number").define("magicNumberIntroduction", "The magic number is... ");
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER.comment("A list of items to log on common setup.").defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

    // Финальная сборка спецификации (Вызывается строго в самом конце объявления всех полей BUILDER!)
    static final ModConfigSpec SPEC = BUILDER.build();

    // Публичные переменные для использования в коде
    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static Set<Item> items;

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();

        // Преобразуем строки предметов в Set объектов
        items = ITEM_STRINGS.get().stream().map(itemName -> BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemName))).collect(Collectors.toSet());
    }
}
