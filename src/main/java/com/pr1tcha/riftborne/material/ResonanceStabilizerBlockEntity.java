package com.pr1tcha.riftborne.material;

import com.pr1tcha.riftborne.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * State of one stabilizer: how many raw shards are queued, how far the current one has been
 * refined, and how many finished cores are waiting to be taken out.
 *
 * <p>Interaction is deliberately container-less — right-click with shards to load them, right-click
 * empty-handed to take the cores. That keeps the block readable without a whole GUI.
 */
public final class ResonanceStabilizerBlockEntity extends BlockEntity {
    /** Ticks of unattended work one shard needs. Stabilization can cut this short. */
    public static final int REFINE_TICKS = 400;
    public static final int MAX_QUEUE = 16;

    private int queuedShards;
    private int readyCores;
    private int progress;

    public ResonanceStabilizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.RESONANCE_STABILIZER_BE_TYPE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  ResonanceStabilizerBlockEntity stabilizer) {
        if (level instanceof ServerLevel serverLevel) {
            stabilizer.tick(serverLevel, pos, state);
        }
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        boolean working = queuedShards > 0;
        if (working) {
            progress++;
            if (progress >= REFINE_TICKS) {
                finishOne(level, pos);
            } else if (progress % 20 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                        3, 0.2D, 0.1D, 0.2D, 0.01D);
            }
        }
        if (state.getValue(ResonanceStabilizerBlock.WORKING) != working) {
            level.setBlock(pos, state.setValue(ResonanceStabilizerBlock.WORKING, working), Block.UPDATE_ALL);
        }
    }

    private void finishOne(ServerLevel level, BlockPos pos) {
        queuedShards--;
        readyCores++;
        progress = 0;
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.6F, 1.4F);
        level.sendParticles(ParticleTypes.END_ROD,
                pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                12, 0.25D, 0.2D, 0.25D, 0.01D);
        setChanged();
    }

    /**
     * A Synchron pushing Stabilization into the block skips a chunk of the wait. Returns false when
     * there is nothing being refined, so the primitive can report an honest failure.
     */
    public boolean accelerate(int ticks) {
        if (queuedShards <= 0) {
            return false;
        }
        progress = Math.min(REFINE_TICKS, progress + Math.max(1, ticks));
        setChanged();
        return true;
    }

    public void interact(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (held.is(ModContent.RIFT_SHARD.get())) {
            load(player, held);
            return;
        }
        collect(player);
    }

    private void load(ServerPlayer player, ItemStack held) {
        if (queuedShards >= MAX_QUEUE) {
            player.displayClientMessage(Component.translatable("message.riftborne.stabilizer.full"), true);
            return;
        }
        int accepted = Math.min(held.getCount(), MAX_QUEUE - queuedShards);
        held.shrink(accepted);
        queuedShards += accepted;
        setChanged();
        player.displayClientMessage(Component.translatable(
                "message.riftborne.stabilizer.loaded", queuedShards, MAX_QUEUE), true);
    }

    private void collect(ServerPlayer player) {
        if (readyCores <= 0) {
            player.displayClientMessage(Component.translatable(
                    "message.riftborne.stabilizer.status",
                    queuedShards,
                    Math.round(progress * 100.0F / REFINE_TICKS)), true);
            return;
        }
        ItemStack cores = new ItemStack(ModContent.RESONANCE_CORE.get(), readyCores);
        readyCores = 0;
        setChanged();
        if (!player.getInventory().add(cores)) {
            player.drop(cores, false);
        }
        player.displayClientMessage(Component.translatable("message.riftborne.stabilizer.collected"), true);
    }

    /** Nothing is swallowed when the block is broken: queued shards and finished cores both fall out. */
    public void dropContents(Level level, BlockPos pos) {
        if (queuedShards > 0) {
            Block.popResource(level, pos, new ItemStack(ModContent.RIFT_SHARD.get(), queuedShards));
        }
        if (readyCores > 0) {
            Block.popResource(level, pos, new ItemStack(ModContent.RESONANCE_CORE.get(), readyCores));
        }
        queuedShards = 0;
        readyCores = 0;
        progress = 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("QueuedShards", queuedShards);
        tag.putInt("ReadyCores", readyCores);
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        queuedShards = Math.max(0, tag.getInt("QueuedShards"));
        readyCores = Math.max(0, tag.getInt("ReadyCores"));
        progress = Math.max(0, tag.getInt("Progress"));
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
