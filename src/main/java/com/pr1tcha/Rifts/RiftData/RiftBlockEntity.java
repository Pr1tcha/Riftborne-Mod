package com.pr1tcha.Rifts.RiftData;

import com.pr1tcha.Rifts.Config;
import com.pr1tcha.Rifts.ModContent;
import com.pr1tcha.Rifts.entity.RiftSplinterEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class RiftBlockEntity extends BlockEntity {
    private static final int REQUIRED_WAVES = 3;
    private static final int UNSTABLE_LIFETIME_PERCENT = 80;

    private final RiftData data = new RiftData();

    public RiftBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.RIFT_BE_TYPE.get(), pos, state);
        this.data.centerPos = pos;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RiftBlockEntity blockEntity) {
        RiftData rift = blockEntity.data;
        rift.ticksExisted++;
        rift.stageTicks++;

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            tickServer(serverLevel, pos, blockEntity, rift);
        }

        // The rift visual is handled by the block entity renderer. Vanilla particles read as large square sprites at distance.
    }

    private static void tickServer(ServerLevel level, BlockPos pos, RiftBlockEntity blockEntity, RiftData rift) {
        String minionTag = "rift_minion_" + pos.toShortString();

        if (rift.stage != RiftStage.COLLAPSING && rift.ticksExisted >= rift.maxLifetimeTicks) {
            failRift(level, pos, rift, minionTag);
            return;
        }

        AABB triggerArea = new AABB(pos).inflate(rift.radius);
        List<Player> playersNear = level.getEntitiesOfClass(Player.class, triggerArea);

        if ((RiftData.RIFT_TYPE.equals(rift.riftType) || RiftData.isContourRift(rift.riftType)) && rift.useProceduralVisual) {
            tickProceduralRiftAwakening(level, pos, blockEntity, rift);
        } else if (rift.stage == RiftStage.OPENING && !playersNear.isEmpty()) {
            activateRift(level, pos, blockEntity, rift, playersNear);
        }

        if (rift.stage == RiftStage.ACTIVE && isNearLifetimeLimit(rift)) {
            setStage(blockEntity, rift, RiftStage.UNSTABLE);
        }

        if (rift.stage == RiftStage.ACTIVE || rift.stage == RiftStage.UNSTABLE) {
            tickCombat(level, pos, blockEntity, rift, minionTag);
        }

        if (rift.stage == RiftStage.COLLAPSING) {
            completeRift(level, pos);
        }
    }

    private static void tickProceduralRiftAwakening(ServerLevel level, BlockPos pos, RiftBlockEntity blockEntity, RiftData rift) {
        if (rift.stage == RiftStage.ACTIVE || rift.stage == RiftStage.UNSTABLE || rift.stage == RiftStage.COLLAPSING || rift.stage == RiftStage.SCAR) {
            return;
        }

        if (rift.stage == RiftStage.OPENING) {
            if (rift.stageTicks >= Config.riftOpeningDurationTicks.get()) {
                activateRift(level, pos, blockEntity, rift, level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(rift.radius)));
            }
            return;
        }

        double nearestDistance = nearestPlayerDistance(level, pos, Config.riftReactingRadius.get());
        RiftStage nextStage = rift.stage;
        if (nearestDistance <= Config.riftOpeningRadius.get()) {
            nextStage = RiftStage.OPENING;
        } else if (nearestDistance <= Config.riftCrackingRadius.get()) {
            nextStage = RiftStage.CRACKING;
        } else if (nearestDistance <= Config.riftReactingRadius.get()) {
            nextStage = RiftStage.REACTING;
        }

        if (isAwakeningProgression(nextStage, rift.stage)) {
            setStage(blockEntity, rift, nextStage);
            playStageSound(level, pos, nextStage);
        }
    }

    private static boolean isAwakeningProgression(RiftStage nextStage, RiftStage currentStage) {
        return awakeningRank(nextStage) > awakeningRank(currentStage);
    }

    private static int awakeningRank(RiftStage stage) {
        return switch (stage) {
            case DORMANT -> 0;
            case REACTING -> 1;
            case CRACKING -> 2;
            case OPENING -> 3;
            case ACTIVE, UNSTABLE, COLLAPSING, SCAR -> 4;
        };
    }

    private static double nearestPlayerDistance(ServerLevel level, BlockPos pos, double maxDistance) {
        AABB scanArea = new AABB(pos).inflate(maxDistance);
        double nearest = Double.MAX_VALUE;
        for (Player player : level.getEntitiesOfClass(Player.class, scanArea)) {
            nearest = Math.min(nearest, player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D));
        }

        return nearest == Double.MAX_VALUE ? Double.MAX_VALUE : Math.sqrt(nearest);
    }

    private static void playStageSound(ServerLevel level, BlockPos pos, RiftStage stage) {
        switch (stage) {
            case REACTING -> level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.35f, 0.55f);
            case CRACKING -> level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.55f, 0.72f);
            case OPENING -> level.playSound(null, pos, ModContent.RIFT_OPENING.get(), SoundSource.BLOCKS, 1.05f, 0.88f);
            default -> {
            }
        }
    }

    private static void activateRift(ServerLevel level, BlockPos pos, RiftBlockEntity blockEntity, RiftData rift, List<Player> playersNear) {
        setStage(blockEntity, rift, RiftStage.ACTIVE);
        rift.spawnCooldown = 40;

        for (Player player : playersNear) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false));
        }

        if (!RiftData.RIFT_TYPE.equals(rift.riftType) || !rift.useProceduralVisual) {
            level.playSound(null, pos, ModContent.RIFT_OPENING.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    private static boolean isNearLifetimeLimit(RiftData rift) {
        return rift.maxLifetimeTicks > 0
                && rift.ticksExisted >= (rift.maxLifetimeTicks * UNSTABLE_LIFETIME_PERCENT) / 100;
    }

    private static void tickCombat(ServerLevel level, BlockPos pos, RiftBlockEntity blockEntity, RiftData rift, String minionTag) {
        if (rift.spawnCooldown > 0) {
            rift.spawnCooldown--;
        }

        if (rift.currentWaveMobsLeft <= 0 && rift.spawnCooldown <= 0) {
            if (rift.wavesCleared >= REQUIRED_WAVES) {
                setStage(blockEntity, rift, RiftStage.COLLAPSING);
            } else {
                spawnWave(level, pos, rift, minionTag);
                rift.wavesCleared++;
                blockEntity.sync();
            }
            return;
        }

        if (level.getGameTime() % 20 == 0 && rift.currentWaveMobsLeft > 0) {
            AABB scanArea = new AABB(pos).inflate(rift.radius + 15);
            List<RiftSplinterEntity> aliveSplinters = level.getEntities(ModContent.RIFT_SPLINTER.get(), scanArea, m -> m.getTags().contains(minionTag));

            rift.currentWaveMobsLeft = aliveSplinters.size();
            if (rift.currentWaveMobsLeft <= 0) {
                rift.spawnCooldown = 60;
                blockEntity.sync();
            }
        }
    }

    private static void failRift(ServerLevel level, BlockPos pos, RiftData rift, String minionTag) {
        cleanupMinions(level, pos, rift, minionTag);
        level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0f, 0.5f);
        level.removeBlock(pos, false);
    }

    private static void completeRift(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.WITHER_DEATH, SoundSource.BLOCKS, 1.0f, 1.0f);
        Block.popResource(level, pos, new ItemStack(ModContent.RIFT_SHARD.get(), 1 + level.random.nextInt(3)));
        level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
    }

    private static void spawnWave(ServerLevel level, BlockPos pos, RiftData rift, String tag) {
        int count = 3 + level.random.nextInt(3);
        for (int i = 0; i < count; i++) {
            RiftSplinterEntity splinter = ModContent.RIFT_SPLINTER.get().create(level);
            if (splinter != null) {
                double rx = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 4;
                double rz = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 4;
                splinter.moveTo(rx, pos.getY(), rz, level.random.nextFloat() * 360, 0);
                splinter.addTag(tag);
                level.addFreshEntity(splinter);
            }
        }
        rift.currentWaveMobsLeft = count;
    }

    private static void setStage(RiftBlockEntity blockEntity, RiftData rift, RiftStage stage) {
        if (rift.stage == stage) {
            return;
        }

        rift.stage = stage;
        rift.stageTicks = 0;
        blockEntity.sync();
    }

    private static void cleanupMinions(ServerLevel level, BlockPos pos, RiftData rift, String tag) {
        AABB scanArea = new AABB(pos).inflate(rift.radius + 15);
        level.getEntities(ModContent.RIFT_SPLINTER.get(), scanArea, m -> m.getTags().contains(tag))
                .forEach(RiftSplinterEntity::discard);
    }

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        data.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        data.load(tag);
    }

    public RiftData getData() {
        return data;
    }
}
