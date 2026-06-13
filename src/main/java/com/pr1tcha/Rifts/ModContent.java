package com.pr1tcha.Rifts;

import com.pr1tcha.Rifts.RiftData.RiftBlock;
import com.pr1tcha.Rifts.RiftData.RiftBlockEntity;
import com.pr1tcha.Rifts.entity.RiftSplinterEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
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
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, RiftborneRift.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, RiftborneRift.MODID);

    public static final Supplier<Block> RIFT_BLOCK = BLOCKS.register("rift",
            () -> new RiftBlock(BlockBehaviour.Properties.of().noCollission().noLootTable().strength(-1.0F, 3600000.0F)));

    public static final Supplier<BlockEntityType<RiftBlockEntity>> RIFT_BE_TYPE = BLOCK_ENTITIES.register("rift_be",
            () -> BlockEntityType.Builder.of(RiftBlockEntity::new, RIFT_BLOCK.get()).build(null));

    public static final Supplier<Item> RIFT_SHARD = ITEMS.register("rift_shard",
            () -> new Item(new Item.Properties()));

    public static final Supplier<SoundEvent> RIFT_OPENING = SOUND_EVENTS.register("rift_opening",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(RiftborneRift.MODID, "rift_opening")));

    public static final Supplier<EntityType<RiftSplinterEntity>> RIFT_SPLINTER = ENTITY_TYPES.register("rift_splinter",
            () -> EntityType.Builder.of(RiftSplinterEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.35F)
                    .clientTrackingRange(8)
                    .build(ResourceLocation.fromNamespaceAndPath(RiftborneRift.MODID, "rift_splinter").toString()));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        ITEMS.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
    }

    public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(RIFT_SPLINTER.get(), RiftSplinterEntity.createAttributes().build());
    }
}
