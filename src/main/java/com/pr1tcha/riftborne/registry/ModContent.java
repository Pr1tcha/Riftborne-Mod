package com.pr1tcha.riftborne.registry;

import com.pr1tcha.riftborne.rift.block.RiftBlock;
import com.pr1tcha.riftborne.rift.block.RiftBlockEntity;
import com.pr1tcha.riftborne.rift.entity.RiftSplinterEntity;
import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.codex.block.CodexLaptopBlock;
import com.pr1tcha.riftborne.aspects.telekinesis.entity.TelekineticBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModContent {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, Riftborne.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Riftborne.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, Riftborne.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, Riftborne.MODID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Riftborne.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Riftborne.MODID);

    public static final Supplier<Block> RIFT_BLOCK = BLOCKS.register("rift",
            () -> new RiftBlock(BlockBehaviour.Properties.of().noCollission().noLootTable().strength(-1.0F, 3600000.0F)));

    public static final Supplier<Block> CONTOUR_STONE = BLOCKS.register("contour_stone",
            () -> new Block(BlockBehaviour.Properties.of().strength(4.2F, 9.0F).requiresCorrectToolForDrops()));

    public static final Supplier<Block> CONTOUR_SURFACE = BLOCKS.register("contour_surface",
            () -> new Block(BlockBehaviour.Properties.of().strength(4.8F, 10.0F).requiresCorrectToolForDrops()));

    public static final Supplier<Block> CONTOUR_TRACE = BLOCKS.register("contour_trace",
            () -> new Block(BlockBehaviour.Properties.of().strength(4.6F, 10.0F).requiresCorrectToolForDrops()));

    public static final Supplier<Block> CONTOUR_STONE_VEIN = BLOCKS.register("contour_stone_vein",
            () -> new Block(BlockBehaviour.Properties.of().strength(5.0F, 11.0F).requiresCorrectToolForDrops()));

    public static final Supplier<Block> CONTOUR_WEEPING_STONE = BLOCKS.register("contour_weeping_stone",
            () -> new Block(BlockBehaviour.Properties.of().lightLevel(state -> 4).strength(5.8F, 14.0F).requiresCorrectToolForDrops()));

    public static final Supplier<Block> CONTOUR_VEIN = BLOCKS.register("contour_vein",
            () -> new Block(BlockBehaviour.Properties.of().lightLevel(state -> 3).strength(5.2F, 12.0F).requiresCorrectToolForDrops()));

    public static final Supplier<Block> CODEX_LAPTOP = BLOCKS.register("codex_laptop",
            () -> new CodexLaptopBlock(BlockBehaviour.Properties.of().strength(2.5F, 5.0F).noOcclusion()));
    public static final Supplier<Block> RNA_INTERSPACE_STONE = interspaceBlock("rna_interspace_stone", 1);
    public static final Supplier<Block> RNA_INTERSPACE_SURFACE = interspaceBlock("rna_interspace_surface", 3);
    public static final Supplier<Block> RNA_INTERSPACE_VEIN = interspaceBlock("rna_interspace_vein", 8);
    public static final Supplier<Block> RIFTWALKER_INTERSPACE_STONE = interspaceBlock("riftwalker_interspace_stone", 2);
    public static final Supplier<Block> RIFTWALKER_INTERSPACE_SURFACE = interspaceBlock("riftwalker_interspace_surface", 5);
    public static final Supplier<Block> RIFTWALKER_INTERSPACE_VEIN = interspaceBlock("riftwalker_interspace_vein", 10);

    public static final Supplier<BlockEntityType<RiftBlockEntity>> RIFT_BE_TYPE = BLOCK_ENTITIES.register("rift_be",
            () -> BlockEntityType.Builder.of(RiftBlockEntity::new, RIFT_BLOCK.get()).build(null));

    public static final Supplier<Item> RIFT_SHARD = ITEMS.register("rift_shard",
            () -> new Item(new Item.Properties()));

    public static final Supplier<Item> CONTOUR_STONE_ITEM = ITEMS.register("contour_stone",
            () -> new BlockItem(CONTOUR_STONE.get(), new Item.Properties()));

    public static final Supplier<Item> CONTOUR_SURFACE_ITEM = ITEMS.register("contour_surface",
            () -> new BlockItem(CONTOUR_SURFACE.get(), new Item.Properties()));

    public static final Supplier<Item> CONTOUR_TRACE_ITEM = ITEMS.register("contour_trace",
            () -> new BlockItem(CONTOUR_TRACE.get(), new Item.Properties()));

    public static final Supplier<Item> CONTOUR_STONE_VEIN_ITEM = ITEMS.register("contour_stone_vein",
            () -> new BlockItem(CONTOUR_STONE_VEIN.get(), new Item.Properties()));

    public static final Supplier<Item> CONTOUR_WEEPING_STONE_ITEM = ITEMS.register("contour_weeping_stone",
            () -> new BlockItem(CONTOUR_WEEPING_STONE.get(), new Item.Properties()));

    public static final Supplier<Item> CONTOUR_VEIN_ITEM = ITEMS.register("contour_vein",
            () -> new BlockItem(CONTOUR_VEIN.get(), new Item.Properties()));

    public static final Supplier<Item> CODEX_LAPTOP_ITEM = ITEMS.register("codex_laptop",
            () -> new BlockItem(CODEX_LAPTOP.get(), new Item.Properties()));
    public static final Supplier<Item> RNA_INTERSPACE_STONE_ITEM = blockItem("rna_interspace_stone", RNA_INTERSPACE_STONE);
    public static final Supplier<Item> RNA_INTERSPACE_SURFACE_ITEM = blockItem("rna_interspace_surface", RNA_INTERSPACE_SURFACE);
    public static final Supplier<Item> RNA_INTERSPACE_VEIN_ITEM = blockItem("rna_interspace_vein", RNA_INTERSPACE_VEIN);
    public static final Supplier<Item> RIFTWALKER_INTERSPACE_STONE_ITEM = blockItem("riftwalker_interspace_stone", RIFTWALKER_INTERSPACE_STONE);
    public static final Supplier<Item> RIFTWALKER_INTERSPACE_SURFACE_ITEM = blockItem("riftwalker_interspace_surface", RIFTWALKER_INTERSPACE_SURFACE);
    public static final Supplier<Item> RIFTWALKER_INTERSPACE_VEIN_ITEM = blockItem("riftwalker_interspace_vein", RIFTWALKER_INTERSPACE_VEIN);

    public static final Supplier<SoundEvent> RIFT_OPENING = SOUND_EVENTS.register("rift_opening",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "rift_opening")));

    public static final Supplier<EntityType<RiftSplinterEntity>> RIFT_SPLINTER = ENTITY_TYPES.register("rift_splinter",
            () -> EntityType.Builder.of(RiftSplinterEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.35F)
                    .clientTrackingRange(8)
                    .build(ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "rift_splinter").toString()));

    public static final Supplier<EntityType<TelekineticBlockEntity>> TELEKINETIC_BLOCK = ENTITY_TYPES.register("telekinetic_block",
            () -> EntityType.Builder.<TelekineticBlockEntity>of(TelekineticBlockEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "telekinetic_block").toString()));

    public static final Supplier<Item> RIFT_SPLINTER_SPAWN_EGG = ITEMS.register("rift_splinter_spawn_egg",
            () -> new SpawnEggItem(RIFT_SPLINTER.get(), 0x1B1425, 0x8E35FF, new Item.Properties()));

    public static final Supplier<CreativeModeTab> RIFTBORNE_TAB = CREATIVE_MODE_TABS.register("riftborne",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.riftborne"))
                    .icon(() -> new ItemStack(CONTOUR_VEIN.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(RIFT_SHARD.get());
                        output.accept(CONTOUR_STONE_ITEM.get());
                        output.accept(CONTOUR_SURFACE_ITEM.get());
                        output.accept(CONTOUR_TRACE_ITEM.get());
                        output.accept(CONTOUR_STONE_VEIN_ITEM.get());
                        output.accept(CONTOUR_WEEPING_STONE_ITEM.get());
                        output.accept(CONTOUR_VEIN_ITEM.get());
                        output.accept(CODEX_LAPTOP_ITEM.get());
                        output.accept(RNA_INTERSPACE_STONE_ITEM.get());
                        output.accept(RNA_INTERSPACE_SURFACE_ITEM.get());
                        output.accept(RNA_INTERSPACE_VEIN_ITEM.get());
                        output.accept(RIFTWALKER_INTERSPACE_STONE_ITEM.get());
                        output.accept(RIFTWALKER_INTERSPACE_SURFACE_ITEM.get());
                        output.accept(RIFTWALKER_INTERSPACE_VEIN_ITEM.get());
                        output.accept(RIFT_SPLINTER_SPAWN_EGG.get());
                    })
                    .build());

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
    }

    public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(RIFT_SPLINTER.get(), RiftSplinterEntity.createAttributes().build());
    }

    private static Supplier<Block> interspaceBlock(String name, int lightLevel) {
        return BLOCKS.register(name, () -> new Block(BlockBehaviour.Properties.of()
                .lightLevel(state -> lightLevel)
                .strength(4.5F, 10.0F)
                .requiresCorrectToolForDrops()));
    }

    private static Supplier<Item> blockItem(String name, Supplier<Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
