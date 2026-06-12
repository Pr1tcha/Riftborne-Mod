package com.kwinta.Rifts.RiftData;

import com.kwinta.Rifts.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class RiftBlockEntity extends BlockEntity {
    private final RiftData data = new RiftData();

    public RiftBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.RIFT_BE_TYPE.get(), pos, state);
        this.data.centerPos = pos;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RiftBlockEntity blockEntity) {
        RiftData rift = blockEntity.data;
        rift.ticksExisted++;

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            String minionTag = "rift_minion_" + pos.toShortString();

            // 1. ПРОВЕРКА НА ТАЙМАУТ (Провал)
            if (rift.stage != RiftStage.COLLAPSE && rift.ticksExisted >= rift.maxLifetimeTicks) {
                cleanupMinions(serverLevel, pos, rift, minionTag);
                serverLevel.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0f, 0.5f);
                level.removeBlock(pos, false);
                return;
            }

            AABB triggerArea = new AABB(pos).inflate(rift.radius);
            List<Player> playersNear = level.getEntitiesOfClass(Player.class, triggerArea);

            // 2. СТАДИЯ OPENING (Ожидание игрока)
            if (rift.stage == RiftStage.OPENING) {
                if (!playersNear.isEmpty()) {
                    rift.stage = RiftStage.ACTIVE;
                    rift.spawnCooldown = 40;
                    for (Player player : playersNear) {
                        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false));
                    }
                    serverLevel.playSound(null, pos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.BLOCKS, 1.0f, 1.0f);
                    blockEntity.sync(); // Синхроним смену стадии
                }
            }

            // 3. СТАДИЯ ACTIVE (Бой)
            if (rift.stage == RiftStage.ACTIVE) {
                if (rift.spawnCooldown > 0) {
                    rift.spawnCooldown--;
                }

                // Если мобы текущей волны кончились и кулдаун прошел — спавним новых или завершаем
                if (rift.currentWaveMobsLeft <= 0 && rift.spawnCooldown <= 0) {
                    if (rift.wavesCleared >= 3) {
                        rift.stage = RiftStage.COLLAPSE;
                        blockEntity.sync();
                    } else {
                        spawnWave(serverLevel, pos, rift, minionTag);
                        rift.wavesCleared++;
                        blockEntity.sync();
                    }
                }
                // Проверка живых мобов каждые 20 тиков (1 сек)
                else if (level.getGameTime() % 20 == 0 && rift.currentWaveMobsLeft > 0) {
                    AABB scanArea = new AABB(pos).inflate(rift.radius + 15);
                    List<Endermite> aliveMites = serverLevel.getEntities(EntityType.ENDERMITE, scanArea, m -> m.getTags().contains(minionTag));

                    rift.currentWaveMobsLeft = aliveMites.size();
                    if (rift.currentWaveMobsLeft <= 0) {
                        rift.spawnCooldown = 60; // Пауза перед следующей волной
                        blockEntity.sync();
                    }
                }
            }

            // 4. СТАДИЯ COLLAPSE (Победа)
            if (rift.stage == RiftStage.COLLAPSE) {
                serverLevel.playSound(null, pos, SoundEvents.WITHER_DEATH, SoundSource.BLOCKS, 1.0f, 1.0f);
                level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
                return; // BlockEntity удалится сам, так как блок заменен
            }
        }

        // КЛИЕНТСКАЯ ЛОГИКА (Визуал)
        if (level.isClientSide) {
            spawnParticles(level, pos, rift);
        }
    }

    private static void spawnWave(ServerLevel level, BlockPos pos, RiftData rift, String tag) {
        int count = 3 + level.random.nextInt(3); // 3-5 эндермитов
        for (int i = 0; i < count; i++) {
            Endermite mite = EntityType.ENDERMITE.create(level);
            if (mite != null) {
                double rx = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 4;
                double rz = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 4;
                mite.moveTo(rx, pos.getY(), rz, level.random.nextFloat() * 360, 0);
                mite.addTag(tag);
                level.addFreshEntity(mite);
            }
        }
        rift.currentWaveMobsLeft = count;
    }

    private static void cleanupMinions(ServerLevel level, BlockPos pos, RiftData rift, String tag) {
        AABB scanArea = new AABB(pos).inflate(rift.radius + 15);
        level.getEntities(EntityType.ENDERMITE, scanArea, m -> m.getTags().contains(tag))
                .forEach(Endermite::discard);
    }

    private static void spawnParticles(Level level, BlockPos pos, RiftData rift) {
        if (rift.stage == RiftStage.OPENING && level.random.nextFloat() < 0.05f) {
            level.addParticle(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0, 0);
        } else if (rift.stage == RiftStage.ACTIVE) {
            for (int i = 0; i < 2; i++) {
                double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 1.5;
                double y = pos.getY() + 0.5 + (level.random.nextDouble() - 0.5) * 1.5;
                double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 1.5;
                level.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, 0, 0.05, 0);
            }
        }
    }

    // ВАЖНО: Метод для принудительной синхронизации клиента и сервера
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

    public RiftData getData() { return data; }
}
