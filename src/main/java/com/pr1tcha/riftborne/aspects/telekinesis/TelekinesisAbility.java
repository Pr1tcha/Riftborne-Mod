package com.pr1tcha.riftborne.aspects.telekinesis;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.rna.RnaApi;
import com.pr1tcha.riftborne.rna.data.RnaData;
import com.pr1tcha.riftborne.aspects.telekinesis.entity.TelekineticBlockEntity;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Riftborne.MODID)
public final class TelekinesisAbility {
    private static final String ABILITY_TAG = "RiftborneTelekinesis";
    private static final double MAX_GRAB_RANGE = 22.0D;
    private static final double MIN_HOLD_DISTANCE = 2.5D;
    private static final double MIN_ITEM_HOLD_DISTANCE = 0.75D;
    private static final double MAX_HOLD_DISTANCE = 20.0D;
    private static final double MAX_LINK_DISTANCE = 32.0D;
    private static final double SCROLL_STEP = 0.75D;
    private static final double ITEM_PICKUP_EYE_DISTANCE = 1.35D;
    private static final double PULL_STRENGTH = 0.38D;
    private static final double HOLD_MOTION_DAMPING = 0.72D;
    private static final double MAX_PULL_SPEED = 1.15D;
    private static final double PUSH_STRENGTH = 1.9D;
    private static final int THROWN_IMPACT_TRACK_TICKS = 45;
    private static final int BLOCK_CHARGE_TICKS = 20;
    private static final float MAX_TARGET_WIDTH = 3.2F;
    private static final float MAX_TARGET_HEIGHT = 4.2F;
    private static final Set<UUID> HOLDING_PLAYERS = new HashSet<>();
    private static final Map<UUID, GrabState> ACTIVE_GRABS = new HashMap<>();
    private static final Map<UUID, BlockChargeState> BLOCK_CHARGES = new HashMap<>();
    private static final Map<UUID, ThrownImpactState> THROWN_IMPACTS = new HashMap<>();

    private TelekinesisAbility() {
    }

    public static boolean hasAbility(Player player) {
        return player.getPersistentData().getBoolean(ABILITY_TAG) && RnaApi.hasActiveRna(player);
    }

    public static void setAbility(ServerPlayer player, boolean enabled) {
        player.getPersistentData().putBoolean(ABILITY_TAG, enabled);
        if (!enabled) {
            HOLDING_PLAYERS.remove(player.getUUID());
            cancelBlockCharge(player.getUUID());
            stopGrab(player);
            TelekinesisNetwork.sendGrabState(player, false);
        }
    }

    public static void handleHoldInput(ServerPlayer player, boolean holding) {
        if (!hasAbility(player)) {
            HOLDING_PLAYERS.remove(player.getUUID());
            cancelBlockCharge(player.getUUID());
            stopGrab(player);
            TelekinesisNetwork.sendGrabState(player, false);
            return;
        }

        if (holding) {
            HOLDING_PLAYERS.add(player.getUUID());
            if (!ACTIVE_GRABS.containsKey(player.getUUID())) {
                if (!tryStartEntityGrab(player)) {
                    tickBlockCharge(player);
                }
            }
        } else {
            HOLDING_PLAYERS.remove(player.getUUID());
            cancelBlockCharge(player.getUUID());
            stopGrab(player);
            TelekinesisNetwork.sendGrabState(player, false);
        }
    }

    public static void adjustDistance(ServerPlayer player, float scrollSteps) {
        GrabState state = ACTIVE_GRABS.get(player.getUUID());
        if (state == null || !hasAbility(player)) {
            return;
        }

        double clampedSteps = Mth.clamp(scrollSteps, -5.0F, 5.0F);
        state.distance = Mth.clamp(state.distance + clampedSteps * SCROLL_STEP, minHoldDistance(state), maxHoldDistance(player));
    }

    public static void pushHeldTarget(ServerPlayer player) {
        GrabState state = ACTIVE_GRABS.remove(player.getUUID());
        if (state == null) {
            return;
        }

        TelekinesisNetwork.sendGrabState(player, false);
        if (!hasAbility(player) || !player.serverLevel().dimension().equals(state.dimension)) {
            restoreTargetGravity(player.getServer(), state, player);
            return;
        }

        HOLDING_PLAYERS.remove(player.getUUID());
        cancelBlockCharge(player.getUUID());

        RnaApi.addMetaWear(player, 2, "telekinesis_push");
        if (!hasAbility(player)) {
            restoreTargetGravity(player.getServer(), state, player);
            return;
        }

        Entity target = player.serverLevel().getEntity(state.entityId);
        if (!canGrabTarget(player, target)) {
            restoreTargetGravity(player.getServer(), state);
            return;
        }

        target.setNoGravity(state.originalNoGravity);
        target.setDeltaMovement(player.getLookAngle().normalize().scale(pushStrength(player)));
        target.hasImpulse = true;
        target.hurtMarked = true;
        restoreItemPickupTarget(target, state);

        if (target instanceof TelekineticBlockEntity blockEntity) {
            blockEntity.markLaunched(player);
        }

        if (target instanceof LivingEntity) {
            THROWN_IMPACTS.put(target.getUUID(), new ThrownImpactState(
                    state.dimension,
                    target.getId(),
                    target.getDeltaMovement(),
                    player.serverLevel().getGameTime() + THROWN_IMPACT_TRACK_TICKS
            ));
        }
    }

    private static boolean tryStartEntityGrab(ServerPlayer player) {
        HitResult hit = ProjectileUtil.getHitResultOnViewVector(player, entity -> canGrabTarget(player, entity), maxGrabRange(player));
        if (!(hit instanceof EntityHitResult entityHit)) {
            return false;
        }

        Entity target = entityHit.getEntity();
        return beginGrab(player, target, player.getEyePosition().distanceTo(entityHit.getLocation()));
    }

    private static boolean beginGrab(ServerPlayer player, Entity target, double distance) {
        if (!canGrabTarget(player, target)) {
            return false;
        }
        int wear = target instanceof TelekineticBlockEntity ? 2 : 1;
        if (!RnaApi.addMetaWear(player, wear, "telekinesis_grab") || !RnaApi.hasActiveRna(player)) {
            return false;
        }

        UUID originalItemTarget = target instanceof ItemEntity item ? item.getTarget() : null;
        GrabState state = new GrabState(
                player.serverLevel().dimension(),
                target.getId(),
                target.isNoGravity(),
                target instanceof ItemEntity,
                originalItemTarget,
                Mth.clamp(distance, minHoldDistance(target), maxHoldDistance(player))
        );

        cancelBlockCharge(player.getUUID());
        target.setNoGravity(true);
        makeHeldItemPickupable(target);
        if (target instanceof TelekineticBlockEntity blockEntity) {
            blockEntity.markHeld(player);
        }
        ACTIVE_GRABS.put(player.getUUID(), state);
        TelekinesisNetwork.sendGrabState(player, true);
        return true;
    }

    private static void stopGrab(ServerPlayer player) {
        GrabState state = ACTIVE_GRABS.remove(player.getUUID());
        if (state != null) {
            restoreTargetGravity(player.getServer(), state, player);
            TelekinesisNetwork.sendGrabState(player, false);
        }
    }

    private static void restoreTargetGravity(MinecraftServer server, GrabState state) {
        restoreTargetGravity(server, state, null);
    }

    private static void restoreTargetGravity(MinecraftServer server, GrabState state, ServerPlayer releasingPlayer) {
        if (server == null) {
            return;
        }

        ServerLevel level = server.getLevel(state.dimension);
        if (level == null) {
            return;
        }

        Entity target = level.getEntity(state.entityId);
        if (target != null && target.isAlive()) {
            target.setNoGravity(state.originalNoGravity);
            restoreItemPickupTarget(target, state);
            if (releasingPlayer != null && target instanceof TelekineticBlockEntity blockEntity) {
                blockEntity.markReleased(releasingPlayer);
            }
        }
    }

    private static boolean canGrabTarget(ServerPlayer player, Entity entity) {
        if (entity == null || entity == player) {
            return false;
        }

        if (entity instanceof ItemEntity item) {
            return item.isAlive() && !item.getItem().isEmpty();
        }

        if (entity instanceof TelekineticBlockEntity blockEntity) {
            return blockEntity.isAlive();
        }

        if (entity instanceof FallingBlockEntity fallingBlock) {
            return fallingBlock.isAlive();
        }

        if (entity instanceof LivingEntity living) {
            return living.isAlive()
                    && !living.isSpectator()
                    && !living.isVehicle()
                    && !living.isPassenger()
                    && living.getBbWidth() <= MAX_TARGET_WIDTH
                    && living.getBbHeight() <= MAX_TARGET_HEIGHT;
        }

        return false;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            tickGrab(player);
        }
    }

    private static void tickGrab(ServerPlayer player) {
        GrabState state = ACTIVE_GRABS.get(player.getUUID());
        if (state == null) {
            if (!HOLDING_PLAYERS.contains(player.getUUID())) {
                return;
            }
            if (!hasAbility(player)) {
                HOLDING_PLAYERS.remove(player.getUUID());
                cancelBlockCharge(player.getUUID());
                TelekinesisNetwork.sendGrabState(player, false);
                return;
            }
            if (!tryStartEntityGrab(player)) {
                tickBlockCharge(player);
            }
            return;
        }

        if (!hasAbility(player) || !player.serverLevel().dimension().equals(state.dimension)) {
            HOLDING_PLAYERS.remove(player.getUUID());
            cancelBlockCharge(player.getUUID());
            stopGrab(player);
            return;
        }

        Entity target = player.serverLevel().getEntity(state.entityId);
        double linkDistance = maxLinkDistance(player);
        if (!canGrabTarget(player, target) || target.distanceToSqr(player) > linkDistance * linkDistance) {
            HOLDING_PLAYERS.remove(player.getUUID());
            cancelBlockCharge(player.getUUID());
            stopGrab(player);
            return;
        }

        if (tryPickupHeldItem(player, target, state)) {
            ACTIVE_GRABS.remove(player.getUUID());
            HOLDING_PLAYERS.remove(player.getUUID());
            cancelBlockCharge(player.getUUID());
            TelekinesisNetwork.sendGrabState(player, false);
            return;
        }

        target.setNoGravity(true);
        makeHeldItemPickupable(target);

        Vec3 desiredCenter = player.getEyePosition().add(player.getLookAngle().normalize().scale(state.distance));
        Vec3 currentCenter = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        Vec3 offset = desiredCenter.subtract(currentCenter);
        Vec3 motion = target.getDeltaMovement().scale(HOLD_MOTION_DAMPING).add(offset.scale(pullStrength(player)));

        double maxPullSpeed = maxPullSpeed(player);
        if (motion.lengthSqr() > maxPullSpeed * maxPullSpeed) {
            motion = motion.normalize().scale(maxPullSpeed);
        }
        if (offset.lengthSqr() < 0.0025D && motion.lengthSqr() < 0.0025D) {
            motion = Vec3.ZERO;
        }

        target.setDeltaMovement(motion);
        target.hasImpulse = true;
        target.hurtMarked = true;
    }

    private static void tickBlockCharge(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        UUID playerId = player.getUUID();
        BlockHitResult hit = findBlockInView(player);
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            cancelBlockCharge(playerId);
            return;
        }

        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!canLiftBlock(level, pos, state)) {
            cancelBlockCharge(playerId);
            return;
        }

        long gameTime = level.getGameTime();
        BlockChargeState charge = BLOCK_CHARGES.get(playerId);
        if (charge == null || !charge.matches(level.dimension(), pos, state)) {
            charge = new BlockChargeState(level.dimension(), pos, state, gameTime);
            BLOCK_CHARGES.put(playerId, charge);
        }

        if (gameTime - charge.startedAtTick >= blockChargeTicks(player)) {
            cancelBlockCharge(playerId);
            liftBlock(player, pos, state);
        }
    }

    private static BlockHitResult findBlockInView(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().normalize().scale(maxGrabRange(player)));
        HitResult hit = player.serverLevel().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit instanceof BlockHitResult blockHit ? blockHit : null;
    }

    private static boolean canLiftBlock(ServerLevel level, BlockPos pos, BlockState state) {
        return !state.isAir()
                && state.getRenderShape() == RenderShape.MODEL
                && level.getBlockEntity(pos) == null
                && state.getDestroySpeed(level, pos) >= 0.0F;
    }

    private static void liftBlock(ServerPlayer player, BlockPos pos, BlockState state) {
        ServerLevel level = player.serverLevel();
        if (!canLiftBlock(level, pos, state)) {
            return;
        }

        float hardness = state.getDestroySpeed(level, pos);
        TelekineticBlockEntity blockEntity = new TelekineticBlockEntity(
                level,
                pos.getX() + 0.5D,
                pos.getY(),
                pos.getZ() + 0.5D,
                state,
                hardness
        );
        blockEntity.markHeld(player);

        if (!level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) {
            return;
        }
        if (!level.addFreshEntity(blockEntity)) {
            level.setBlock(pos, state, 3);
            return;
        }

        level.levelEvent(2001, pos, Block.getId(state));
        beginGrab(player, blockEntity, player.getEyePosition().distanceTo(Vec3.atCenterOf(pos)));
    }

    private static void cancelBlockCharge(UUID playerId) {
        BLOCK_CHARGES.remove(playerId);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            tickThrownImpacts(level);
        }
    }

    private static void tickThrownImpacts(ServerLevel level) {
        Iterator<Map.Entry<UUID, ThrownImpactState>> iterator = THROWN_IMPACTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ThrownImpactState> entry = iterator.next();
            ThrownImpactState state = entry.getValue();

            if (!level.dimension().equals(state.dimension)) {
                continue;
            }
            if (level.getGameTime() > state.expiresAtTick) {
                iterator.remove();
                continue;
            }

            Entity entity = level.getEntity(state.entityId);
            if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
                iterator.remove();
                continue;
            }

            double previousSpeed = state.lastMotion.horizontalDistance();
            double currentSpeed = target.getDeltaMovement().horizontalDistance();
            if (target.horizontalCollision) {
                float damage = (float) ((previousSpeed - currentSpeed) * 10.0D - 3.0D);
                if (damage > 0.0F) {
                    target.hurt(target.damageSources().flyIntoWall(), damage);
                }
                iterator.remove();
                continue;
            }

            Vec3 currentMotion = target.getDeltaMovement();
            if (currentMotion.lengthSqr() > 0.01D) {
                state.lastMotion = currentMotion;
            }
        }
    }

    private static boolean tryPickupHeldItem(ServerPlayer player, Entity target, GrabState state) {
        if (!state.itemEntity || !(target instanceof ItemEntity item)) {
            return false;
        }

        Vec3 itemCenter = item.position().add(0.0D, item.getBbHeight() * 0.5D, 0.0D);
        if (player.getEyePosition().distanceToSqr(itemCenter) > ITEM_PICKUP_EYE_DISTANCE * ITEM_PICKUP_EYE_DISTANCE) {
            return false;
        }

        item.setTarget(null);
        item.setNoPickUpDelay();
        item.playerTouch(player);
        return item.isRemoved() || item.getItem().isEmpty();
    }

    private static void makeHeldItemPickupable(Entity target) {
        if (target instanceof ItemEntity item) {
            item.setTarget(null);
            item.setNoPickUpDelay();
        }
    }

    private static void restoreItemPickupTarget(Entity target, GrabState state) {
        if (state.itemEntity && target instanceof ItemEntity item) {
            item.setTarget(state.originalItemTarget);
            item.setDefaultPickUpDelay();
        }
    }

    private static double minHoldDistance(GrabState state) {
        return minHoldDistance(state.itemEntity);
    }

    private static double minHoldDistance(Entity target) {
        return minHoldDistance(target instanceof ItemEntity);
    }

    private static double minHoldDistance(boolean itemEntity) {
        return itemEntity ? MIN_ITEM_HOLD_DISTANCE : MIN_HOLD_DISTANCE;
    }

    private static double maxGrabRange(Player player) {
        return MAX_GRAB_RANGE + RnaApi.get(player).throughput() * 0.03D;
    }

    private static double maxHoldDistance(Player player) {
        return MAX_HOLD_DISTANCE + RnaApi.get(player).nodeDensity() * 0.02D;
    }

    private static double maxLinkDistance(Player player) {
        return MAX_LINK_DISTANCE + RnaApi.get(player).connectivity() * 0.03D;
    }

    private static double pullStrength(Player player) {
        return PULL_STRENGTH + RnaApi.get(player).connectivity() * 0.0015D;
    }

    private static double maxPullSpeed(Player player) {
        return MAX_PULL_SPEED + RnaApi.get(player).throughput() * 0.002D;
    }

    private static double pushStrength(Player player) {
        return PUSH_STRENGTH + RnaApi.get(player).throughput() * 0.004D;
    }

    private static int blockChargeTicks(Player player) {
        RnaData data = RnaApi.get(player);
        return Math.max(12, BLOCK_CHARGE_TICKS - data.connectivity() / 20);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer originalPlayer) {
            HOLDING_PLAYERS.remove(originalPlayer.getUUID());
            cancelBlockCharge(originalPlayer.getUUID());
            stopGrab(originalPlayer);
        }

        if (event.getOriginal().getPersistentData().getBoolean(ABILITY_TAG)) {
            event.getEntity().getPersistentData().putBoolean(ABILITY_TAG, true);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HOLDING_PLAYERS.remove(player.getUUID());
            cancelBlockCharge(player.getUUID());
            stopGrab(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HOLDING_PLAYERS.remove(player.getUUID());
            cancelBlockCharge(player.getUUID());
            stopGrab(player);
        }
    }

    private static final class GrabState {
        private final ResourceKey<Level> dimension;
        private final int entityId;
        private final boolean originalNoGravity;
        private final boolean itemEntity;
        private final UUID originalItemTarget;
        private double distance;

        private GrabState(ResourceKey<Level> dimension, int entityId, boolean originalNoGravity, boolean itemEntity, UUID originalItemTarget, double distance) {
            this.dimension = dimension;
            this.entityId = entityId;
            this.originalNoGravity = originalNoGravity;
            this.itemEntity = itemEntity;
            this.originalItemTarget = originalItemTarget;
            this.distance = distance;
        }
    }

    private static final class ThrownImpactState {
        private final ResourceKey<Level> dimension;
        private final int entityId;
        private final long expiresAtTick;
        private Vec3 lastMotion;

        private ThrownImpactState(ResourceKey<Level> dimension, int entityId, Vec3 lastMotion, long expiresAtTick) {
            this.dimension = dimension;
            this.entityId = entityId;
            this.lastMotion = lastMotion;
            this.expiresAtTick = expiresAtTick;
        }
    }

    private static final class BlockChargeState {
        private final ResourceKey<Level> dimension;
        private final BlockPos pos;
        private final BlockState state;
        private final long startedAtTick;

        private BlockChargeState(ResourceKey<Level> dimension, BlockPos pos, BlockState state, long startedAtTick) {
            this.dimension = dimension;
            this.pos = pos.immutable();
            this.state = state;
            this.startedAtTick = startedAtTick;
        }

        private boolean matches(ResourceKey<Level> dimension, BlockPos pos, BlockState state) {
            return this.dimension.equals(dimension) && this.pos.equals(pos) && this.state.equals(state);
        }
    }
}
