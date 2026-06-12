package com.pr1tcha.Rifts;

import com.pr1tcha.Rifts.RiftData.RiftBlock;
import com.pr1tcha.Rifts.RiftData.RiftBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModContent {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, Eventmod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Eventmod.MODID);


    public static final Supplier<Block> RIFT_BLOCK = BLOCKS.register("rift", 
            () -> new RiftBlock(BlockBehaviour.Properties.of().noCollission().noLootTable().strength(-1.0F, 3600000.0F)));

    public static final Supplier<BlockEntityType<RiftBlockEntity>> RIFT_BE_TYPE = BLOCK_ENTITIES.register("rift_be",
            () -> BlockEntityType.Builder.of(RiftBlockEntity::new, RIFT_BLOCK.get()).build(null));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
    }
}
