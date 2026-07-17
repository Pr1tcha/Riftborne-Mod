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
 * <p>Vertical-slice scope: P1 Reading and P3 Shift are wired to real effects; the other seven
 * primitives return {@link Result#NOT_IMPLEMENTED} until their phases land.
 */
public final class PowerCast {
    private static final double REACH = 5.0D;

    private PowerCast() {
    }

    public enum Result {
        OK, NO_RNA, NO_CONNECTIVITY, ANOMALY_BLOCKED, COMPENSATION, NO_TARGET, INVALID_TARGET, NOT_IMPLEMENTED
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
            compensationBacklash(player);
            return new Outcome(Result.COMPENSATION, load, window, load > profile.throughput());
        }

        Result effect = applyEffect(player, primitive);
        boolean overload = load > profile.throughput();
        if (effect == Result.OK) {
            PowerApi.addMetaWear(player, PowerRules.metaWearForLoad(load));
        }
        return new Outcome(effect, load, window, overload);
    }

    private static Result applyEffect(ServerPlayer player, Primitive primitive) {
        return switch (primitive) {
            case P1_READING -> readEffect(player);
            case P3_SHIFT -> shiftEffect(player);
            default -> Result.NOT_IMPLEMENTED;
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
