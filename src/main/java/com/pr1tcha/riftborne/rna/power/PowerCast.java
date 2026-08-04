package com.pr1tcha.riftborne.rna.power;

import com.pr1tcha.riftborne.rna.power.data.RNAProfile;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The PS V2.5 cast pipeline: turn a primitive request into load, run the regulator checks
 * (connectivity, anomaly, admissibility window), apply the world effect, and pay meta-wear.
 * Server-authoritative — never trust the client for what a cast costs.
 *
 * <p>All nine primitives are wired. Time effects additionally honour the lore support rules: they
 * require a live anchor (P2), and shifting a tempo also requires a phase entry (V2).
 */
public final class PowerCast {
    private static final double REACH = 5.0D;
    private static final int ANCHOR_DURATION = 1200;
    private static final int PHASE_DURATION = 400;
    private static final float STABILIZE_RELIEF = 3.0F;

    private PowerCast() {
    }

    public enum Result {
        OK, NO_RNA, NO_CONNECTIVITY, ANOMALY_BLOCKED, COMPENSATION, NO_TARGET, INVALID_TARGET,
        NOT_IMPLEMENTED, NO_ANCHOR, NO_PHASE
    }

    public record Outcome(Result result, float load, float window, boolean overload) {
        static Outcome fail(Result result) {
            return new Outcome(result, 0.0F, 0.0F, false);
        }
    }

    public static Outcome cast(ServerPlayer player, Primitive primitive) {
        RNAProfile profile = PowerApi.get(player);
        if (!profile.active()) {
            return Outcome.fail(Result.NO_RNA);
        }

        Set<DeltaAxis> axes = PowerRules.defaultAxes(primitive);
        int axisCount = axes.size();
        if (LoadCalculator.isAnomalous(axisCount)) {
            return Outcome.fail(Result.ANOMALY_BLOCKED);
        }
        if (!LoadCalculator.connectivityAllows(axisCount, profile.connectivity())) {
            return Outcome.fail(Result.NO_CONNECTIVITY);
        }

        int depth = PowerRules.depthLevel(profile, primitive);
        float load = LoadCalculator.totalLoad(axes, depth, HoldType.MOMENT);
        float window = PowerRules.admissibilityWindow(profile);

        if (load > window) {
            PowerApi.addMetaWear(player, PowerRules.metaWearForLoad(load) * 1.5F);
            PowerPractice.recordCompensation(player);
            compensationBacklash(player);
            return new Outcome(Result.COMPENSATION, load, window, load > profile.throughput());
        }

        Result effect = applyEffect(player, primitive);
        boolean overload = load > profile.throughput();
        if (effect == Result.OK) {
            PowerApi.addMetaWear(player, PowerRules.metaWearForLoad(load));
            for (DeltaAxis axis : axes) {
                PowerApi.addPractice(player, axis.id(), 1);
            }
            PowerPractice.recordSuccess(player, primitive, load);
        }
        return new Outcome(effect, load, window, overload);
    }

    private static Result applyEffect(ServerPlayer player, Primitive primitive) {
        return switch (primitive) {
            case P1_READING -> readEffect(player);
            case P2_ANCHOR -> anchorEffect(player);
            case P3_SHIFT -> shiftEffect(player);
            case P4_SHAPE -> shapeEffect(player);
            case P5_STABILIZE -> stabilizeEffect(player);
            case V1_TEMPO_READ -> tempoReadEffect(player);
            case V2_PHASE_ENTRY -> phaseEntryEffect(player);
            case V3_TEMPO_SHIFT -> tempoShiftEffect(player);
            case V4_PHASE_HOLD -> phaseHoldEffect(player);
        };
    }

    private static Result readEffect(ServerPlayer player) {
        BlockHitResult hit = rayTrace(player);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return Result.NO_TARGET;
        }
        ServerLevel level = player.serverLevel();
        Vec3 center = Vec3.atCenterOf(hit.getBlockPos());
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z,
                24, 0.45D, 0.45D, 0.45D, 0.02D);
        level.playSound(null, hit.getBlockPos(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5F, 1.6F);
        return Result.OK;
    }

    private static Result shiftEffect(ServerPlayer player) {
        BlockHitResult hit = rayTrace(player);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return Result.NO_TARGET;
        }
        ServerLevel level = player.serverLevel();
        BlockPos from = hit.getBlockPos();
        BlockState state = level.getBlockState(from);
        if (state.isAir()
                || level.getBlockEntity(from) != null
                || state.getDestroySpeed(level, from) < 0.0F
                || !state.isCollisionShapeFullBlock(level, from)) {
            return Result.INVALID_TARGET;
        }

        Direction dir = player.getDirection();
        BlockPos to = from.relative(dir);
        if (!level.getBlockState(to).canBeReplaced() || !level.getBlockState(to).getFluidState().isEmpty()) {
            return Result.INVALID_TARGET;
        }

        level.setBlockAndUpdate(to, state);
        level.setBlock(from, Blocks.AIR.defaultBlockState(), 3);
        Vec3 mid = Vec3.atCenterOf(from).add(Vec3.atLowerCornerOf(dir.getNormal()).scale(0.5D));
        level.sendParticles(ParticleTypes.SCULK_SOUL, mid.x, mid.y + 0.3D, mid.z, 10, 0.2D, 0.2D, 0.2D, 0.01D);
        level.playSound(null, to, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.6F, 0.9F);
        return Result.OK;
    }

    /** P2 Якорение: plant the spatial support that every time effect has to stand on. */
    private static Result anchorEffect(ServerPlayer player) {
        BlockHitResult hit = rayTrace(player);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return Result.NO_TARGET;
        }
        BlockPos pos = hit.getBlockPos().relative(hit.getDirection());
        PowerFieldState.setAnchor(player, pos, ANCHOR_DURATION);
        ServerLevel level = player.serverLevel();
        Vec3 center = Vec3.atCenterOf(pos);
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z,
                18, 0.15D, 0.35D, 0.15D, 0.01D);
        level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 0.5F, 1.5F);
        return Result.OK;
    }

    /** P4 Формовка: the area-wide sibling of Shift — moves a 3x3 face one step at once. */
    private static Result shapeEffect(ServerPlayer player) {
        BlockHitResult hit = rayTrace(player);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return Result.NO_TARGET;
        }
        ServerLevel level = player.serverLevel();
        Direction dir = player.getDirection();
        BlockPos origin = hit.getBlockPos();
        Direction.Axis axis = dir.getAxis();

        java.util.List<BlockPos> movable = new java.util.ArrayList<>();
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                BlockPos from = axis == Direction.Axis.Y
                        ? origin.offset(a, 0, b)
                        : origin.offset(axis == Direction.Axis.X ? 0 : a, b, axis == Direction.Axis.Z ? 0 : a);
                if (canShift(level, from, from.relative(dir))) {
                    movable.add(from);
                }
            }
        }
        if (movable.isEmpty()) {
            return Result.INVALID_TARGET;
        }
        // Collect first, then write, so blocks do not consume each other's destination.
        java.util.Map<BlockPos, BlockState> carried = new java.util.LinkedHashMap<>();
        for (BlockPos from : movable) {
            carried.put(from, level.getBlockState(from));
        }
        for (BlockPos from : movable) {
            level.setBlock(from, Blocks.AIR.defaultBlockState(), 3);
        }
        for (java.util.Map.Entry<BlockPos, BlockState> entry : carried.entrySet()) {
            level.setBlockAndUpdate(entry.getKey().relative(dir), entry.getValue());
        }
        Vec3 center = Vec3.atCenterOf(origin);
        level.sendParticles(ParticleTypes.SCULK_SOUL, center.x, center.y, center.z,
                24, 0.8D, 0.8D, 0.8D, 0.01D);
        level.playSound(null, origin, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.7F, 0.7F);
        return Result.OK;
    }

    /** P5 Стабилизация: damp the rollback — steadies the architecture and props up the anchor. */
    private static Result stabilizeEffect(ServerPlayer player) {
        PowerApi.reduceMetaWear(player, STABILIZE_RELIEF);
        PowerFieldState.extendAnchor(player, ANCHOR_DURATION / 2);
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.GLOW, player.getX(), player.getY() + 1.0D, player.getZ(),
                16, 0.4D, 0.6D, 0.4D, 0.01D);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.6F, 0.8F);
        return Result.OK;
    }

    /** V1 Считывание темпа: report how fast the looked-at process is actually running. */
    private static Result tempoReadEffect(ServerPlayer player) {
        BlockHitResult hit = rayTrace(player);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return Result.NO_TARGET;
        }
        ServerLevel level = player.serverLevel();
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        boolean randomTicks = state.isRandomlyTicking();
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                randomTicks ? "message.riftborne.power.tempo_active" : "message.riftborne.power.tempo_inert",
                state.getBlock().getName()), true);
        Vec3 center = Vec3.atCenterOf(pos);
        level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 0.6D, center.z,
                14, 0.3D, 0.3D, 0.3D, 0.4D);
        return Result.OK;
    }

    /** V2 Фазовый вход: synchronise with the local flow — the prerequisite for shifting it. */
    private static Result phaseEntryEffect(ServerPlayer player) {
        if (PowerFieldState.activeAnchor(player, player.blockPosition()) == null) {
            return Result.NO_ANCHOR;
        }
        PowerFieldState.enterPhase(player, PHASE_DURATION);
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0D, player.getZ(),
                24, 0.4D, 0.7D, 0.4D, 0.02D);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.4F, 1.8F);
        return Result.OK;
    }

    /** V3 Смещение темпа: push the flow around the anchor forward. Needs anchor plus phase. */
    private static Result tempoShiftEffect(ServerPlayer player) {
        PowerFieldState.Anchor anchor = PowerFieldState.activeAnchor(player, player.blockPosition());
        if (anchor == null) {
            return Result.NO_ANCHOR;
        }
        if (!PowerFieldState.inPhase(player)) {
            return Result.NO_PHASE;
        }
        ServerLevel level = player.serverLevel();
        BlockPos centre = anchor.pos();
        int radius = 4;
        int touched = 0;
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, -2, -radius),
                centre.offset(radius, 2, radius))) {
            BlockState state = level.getBlockState(pos);
            if (state.isRandomlyTicking() && level.random.nextFloat() < 0.5F) {
                state.randomTick(level, pos.immutable(), level.random);
                touched++;
            }
        }
        if (touched == 0) {
            return Result.INVALID_TARGET;
        }
        Vec3 c = Vec3.atCenterOf(centre);
        level.sendParticles(ParticleTypes.WAX_OFF, c.x, c.y + 1.0D, c.z, 30, radius * 0.5D, 1.0D, radius * 0.5D, 0.01D);
        level.playSound(null, centre, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.7F, 1.4F);
        return Result.OK;
    }

    /** V4 Удержание фазы: hold the altered tempo instead of letting it snap back. */
    private static Result phaseHoldEffect(ServerPlayer player) {
        if (PowerFieldState.activeAnchor(player, player.blockPosition()) == null) {
            return Result.NO_ANCHOR;
        }
        if (!PowerFieldState.inPhase(player)) {
            return Result.NO_PHASE;
        }
        PowerFieldState.extendPhase(player, PHASE_DURATION);
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.2D, player.getZ(),
                12, 0.3D, 0.5D, 0.3D, 0.0D);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.5F, 1.2F);
        return Result.OK;
    }

    private static boolean canShift(ServerLevel level, BlockPos from, BlockPos to) {
        BlockState state = level.getBlockState(from);
        if (state.isAir()
                || level.getBlockEntity(from) != null
                || state.getDestroySpeed(level, from) < 0.0F
                || !state.isCollisionShapeFullBlock(level, from)) {
            return false;
        }
        BlockState target = level.getBlockState(to);
        return target.canBeReplaced() && target.getFluidState().isEmpty();
    }

    private static void compensationBacklash(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0D, player.getZ(),
                18, 0.35D, 0.5D, 0.35D, 0.02D);
        level.playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                SoundSource.PLAYERS, 0.7F, 1.4F);
    }

    private static BlockHitResult rayTrace(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(REACH));
        return player.serverLevel().clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }
}
