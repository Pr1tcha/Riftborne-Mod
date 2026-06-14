package com.pr1tcha.Rifts;

import com.pr1tcha.Rifts.RiftData.RiftBlock;
import com.pr1tcha.Rifts.RiftData.RiftBlockEntity;
import com.pr1tcha.Rifts.entity.RiftSplinterEntity;
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

import java.util.function.Supplier;

public class ModContent {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, RiftborneRift.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, RiftborneRift.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, RiftborneRift.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, RiftborneRift.MODID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, RiftborneRift.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, RiftborneRift.MODID);

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

    public static final Supplier<SoundEvent> RIFT_OPENING = SOUND_EVENTS.register("rift_opening",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(RiftborneRift.MODID, "rift_opening")));

    public static final Supplier<EntityType<RiftSplinterEntity>> RIFT_SPLINTER = ENTITY_TYPES.register("rift_splinter",
            () -> EntityType.Builder.of(RiftSplinterEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.35F)
                    .clientTrackingRange(8)
                    .build(ResourceLocation.fromNamespaceAndPath(RiftborneRift.MODID, "rift_splinter").toString()));

    public static final Supplier<Item> RIFT_SPLINTER_SPAWN_EGG = ITEMS.register("rift_splinter_spawn_egg",
            () -> new SpawnEggItem(RIFT_SPLINTER.get(), 0x1B1425, 0x8E35FF, new Item.Properties()));

    public static final Supplier<CreativeModeTab> RIFTBORNE_TAB = CREATIVE_MODE_TABS.register("riftborne",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.riftborne_rift.riftborne"))
                    .icon(() -> new ItemStack(CONTOUR_VEIN.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(RIFT_SHARD.get());
                        output.accept(CONTOUR_STONE_ITEM.get());
                        output.accept(CONTOUR_SURFACE_ITEM.get());
                        output.accept(CONTOUR_TRACE_ITEM.get());
                        output.accept(CONTOUR_STONE_VEIN_ITEM.get());
                        output.accept(CONTOUR_WEEPING_STONE_ITEM.get());
                        output.accept(CONTOUR_VEIN_ITEM.get());
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
}
