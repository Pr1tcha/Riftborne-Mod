package com.pr1tcha.riftborne.rift.dimension;

import com.pr1tcha.riftborne.Riftborne;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public enum RiftTier {
    SURFACE_SHARD(1, "surface_shard", "rift_l1_surface_shard", Blocks.STONE),
    SHIFTED_LAYER(2, "shifted_layer", "rift_l2_shifted_layer", Blocks.DEEPSLATE),
    NODE_RIFT(3, "node_rift", "rift_l3_node_rift", Blocks.POLISHED_BLACKSTONE),
    DEEP_RIFT(4, "deep_rift", "rift_l4_deep_rift", Blocks.OBSIDIAN),
    LIMIT_SLICE(5, "limit_slice", "rift_l5_limit_slice", Blocks.CRYING_OBSIDIAN);

    private final int level;
    private final String translationKey;
    private final ResourceKey<Level> dimension;
    private final Block platformBlock;

    RiftTier(int level, String name, String dimensionPath, Block platformBlock) {
        this.level = level;
        this.translationKey = "dimension.riftborne.rift_tier." + name;
        this.dimension = ResourceKey.create(
                Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, dimensionPath)
        );
        this.platformBlock = platformBlock;
    }

    public int level() {
        return level;
    }

    public String translationKey() {
        return translationKey;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public Block platformBlock() {
        return platformBlock;
    }

    public static Optional<RiftTier> fromLevel(int level) {
        return Arrays.stream(values()).filter(tier -> tier.level == level).findFirst();
    }

    public static Optional<RiftTier> fromDimension(ResourceKey<Level> dimension) {
        return Arrays.stream(values()).filter(tier -> tier.dimension.equals(dimension)).findFirst();
    }
}
