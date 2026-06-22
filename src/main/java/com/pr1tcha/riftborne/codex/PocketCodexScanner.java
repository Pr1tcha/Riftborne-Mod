package com.pr1tcha.riftborne.codex;

import com.pr1tcha.riftborne.codex.data.PocketCodexData;
import com.pr1tcha.riftborne.config.Config;
import com.pr1tcha.riftborne.registry.ModContent;
import com.pr1tcha.riftborne.rift.entity.RiftSplinterEntity;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class PocketCodexScanner {
    private PocketCodexScanner() {
    }

    public static Result scan(ServerPlayer player, ItemStack codex) {
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

        Optional<ScanTarget> target = entityHit != null
                ? resolve(entityHit.getEntity())
                : blockHit.getType() == HitResult.Type.MISS
                        ? Optional.empty()
                        : resolve(player.level().getBlockState(blockHit.getBlockPos()));
        if (target.isEmpty()) {
            return Result.NO_TARGET;
        }
        ScanTarget scanTarget = target.get();
        return PocketCodexData.discover(codex, scanTarget.entryId(), scanTarget.damaged())
                ? Result.DISCOVERED
                : Result.KNOWN;
    }

    private static boolean isScannable(Entity entity) {
        return !entity.isSpectator() && entity.isPickable() && resolve(entity).isPresent();
    }

    private static Optional<ScanTarget> resolve(Entity entity) {
        return entity instanceof RiftSplinterEntity
                ? Optional.of(new ScanTarget("rift_splinter", false))
                : Optional.empty();
    }

    private static Optional<ScanTarget> resolve(BlockState state) {
        if (state.is(ModContent.RIFT_BLOCK.get())) {
            return Optional.of(new ScanTarget("rift_basic", false));
        }
        if (state.is(ModContent.CONTOUR_STONE.get())
                || state.is(ModContent.CONTOUR_SURFACE.get())
                || state.is(ModContent.CONTOUR_TRACE.get())
                || state.is(ModContent.CONTOUR_STONE_VEIN.get())
                || state.is(ModContent.CONTOUR_WEEPING_STONE.get())
                || state.is(ModContent.CONTOUR_VEIN.get())) {
            return Optional.of(new ScanTarget("discard_contour", true));
        }
        if (state.is(ModContent.RNA_INTERSPACE_STONE.get())
                || state.is(ModContent.RNA_INTERSPACE_SURFACE.get())
                || state.is(ModContent.RNA_INTERSPACE_VEIN.get())) {
            return Optional.of(new ScanTarget("rna_interspace", false));
        }
        if (state.is(ModContent.RIFTWALKER_INTERSPACE_STONE.get())
                || state.is(ModContent.RIFTWALKER_INTERSPACE_SURFACE.get())
                || state.is(ModContent.RIFTWALKER_INTERSPACE_VEIN.get())) {
            return Optional.of(new ScanTarget("riftwalker_interspace", false));
        }
        return Optional.empty();
    }

    public enum Result {
        DISCOVERED,
        KNOWN,
        NO_TARGET
    }

    private record ScanTarget(String entryId, boolean damaged) {
    }
}
