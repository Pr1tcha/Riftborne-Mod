package com.pr1tcha.riftborne.codex;

import com.pr1tcha.riftborne.codex.data.PocketCodexData;
import com.pr1tcha.riftborne.codex.data.entry.CodexEntryDefinition;
import com.pr1tcha.riftborne.codex.data.entry.CodexEntryRegistry;
import com.pr1tcha.riftborne.codex.scan.CodexScanTargetDefinition;
import com.pr1tcha.riftborne.codex.scan.CodexScanTargetRegistry;
import com.pr1tcha.riftborne.config.Config;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class PocketCodexScanner {
    private static final int PULSE_RADIUS = 10;
    private static final int PULSE_VERTICAL_RADIUS = 4;

    private PocketCodexScanner() {
    }

    public static ScanReport scan(ServerPlayer player, ItemStack codex) {
        double distance = Config.codexScanDistance.get();
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getViewVector(1.0F);
        Vec3 end = start.add(direction.scale(distance));
        BlockHitResult blockHit = player.level().clip(new ClipContext(
                start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player
        ));
        double maxEntityDistance = blockHit.getType() == HitResult.Type.MISS
                ? distance * distance
                : start.distanceToSqr(blockHit.getLocation());
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player,
                start,
                end,
                player.getBoundingBox().expandTowards(direction.scale(distance)).inflate(1.0D),
                PocketCodexScanner::isScannable,
                maxEntityDistance
        );

        CodexScanTargetDefinition target = entityHit != null
                ? CodexScanTargetRegistry.resolve(entityHit.getEntity())
                : blockHit.getType() == HitResult.Type.MISS
                        ? null
                        : CodexScanTargetRegistry.resolve(player.level().getBlockState(blockHit.getBlockPos()));
        if (target == null) {
            return ScanReport.empty(Result.NO_TARGET);
        }

        PocketCodexData.RecordResult record = PocketCodexData.recordObservation(
                codex, target.entryId(), target.damaged()
        );
        Result result = switch (record) {
            case DISCOVERED -> Result.DISCOVERED;
            case UPDATED -> Result.UPDATED;
            case DAMAGED -> Result.DAMAGED;
            case BUFFER_FULL -> Result.BUFFER_FULL;
            case INVALID -> Result.INVALID_TARGET;
        };
        return new ScanReport(
                result,
                target.entryId(),
                PocketCodexData.observationCount(codex, target.entryId()),
                PocketCodexData.MAX_OBSERVATIONS,
                threat(target.entryId())
        );
    }

    public static PulseReport pulse(ServerPlayer player, ItemStack codex) {
        Set<String> signatures = new LinkedHashSet<>();
        String nearestEntry = "";
        double nearestDistance = Double.MAX_VALUE;
        AABB area = player.getBoundingBox().inflate(PULSE_RADIUS, PULSE_VERTICAL_RADIUS, PULSE_RADIUS);

        for (Entity entity : player.level().getEntities(player, area, PocketCodexScanner::isScannable)) {
            CodexScanTargetDefinition target = CodexScanTargetRegistry.resolve(entity);
            if (target == null) {
                continue;
            }
            signatures.add(target.entryId());
            double distance = player.distanceToSqr(entity);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestEntry = target.entryId();
            }
        }

        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-PULSE_RADIUS, -PULSE_VERTICAL_RADIUS, -PULSE_RADIUS),
                center.offset(PULSE_RADIUS, PULSE_VERTICAL_RADIUS, PULSE_RADIUS)
        )) {
            CodexScanTargetDefinition target = CodexScanTargetRegistry.resolve(player.level().getBlockState(pos));
            if (target == null) {
                continue;
            }
            signatures.add(target.entryId());
            double distance = pos.distSqr(center);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestEntry = target.entryId();
            }
        }

        int distance = nearestDistance == Double.MAX_VALUE ? -1 : (int) Math.round(Math.sqrt(nearestDistance));
        PocketCodexData.recordPulse(codex, signatures.size(), nearestEntry, distance);
        return new PulseReport(signatures.size(), nearestEntry, distance);
    }

    private static boolean isScannable(Entity entity) {
        return !entity.isSpectator()
                && entity.isPickable()
                && CodexScanTargetRegistry.resolve(entity) != null;
    }

    private static int threat(String entryId) {
        CodexEntryDefinition entry = CodexEntryRegistry.get(entryId);
        if (entry != null) {
            return entry.threatLevel();
        }
        String legacyId = entryId.contains(":") ? entryId.substring(entryId.indexOf(':') + 1) : entryId;
        var legacy = CodexEntries.get(legacyId);
        return legacy == null ? 0 : legacy.threatLevel();
    }

    public enum Result {
        DISCOVERED,
        UPDATED,
        DAMAGED,
        BUFFER_FULL,
        INVALID_TARGET,
        NO_TARGET
    }

    public record ScanReport(Result result, String entryId, int observations, int maximumObservations, int threat) {
        private static ScanReport empty(Result result) {
            return new ScanReport(result, "", 0, PocketCodexData.MAX_OBSERVATIONS, 0);
        }
    }

    public record PulseReport(int signalCount, String nearestEntryId, int nearestDistance) {
    }
}
