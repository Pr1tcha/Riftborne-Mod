package com.pr1tcha.riftborne.rna.power;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Transient per-player field state backing two lore rules that the primitives depend on:
 *
 * <ul>
 *   <li><b>Опора</b> — a time effect only holds while a spatial anchor (P2) is live nearby; without
 *       one the effect has nothing to stand on and is refused.</li>
 *   <li><b>Фазовый вход</b> — V3 may only shift a tempo the Synchron has already synchronised with
 *       via V2.</li>
 * </ul>
 *
 * Deliberately not persisted: an anchor is a held condition, not an achievement, so it should not
 * survive a relog.
 */
public final class PowerFieldState {
    /** How far a time effect may reach from its supporting anchor. */
    public static final double ANCHOR_RADIUS = 12.0D;

    private static final Map<UUID, Anchor> ANCHORS = new HashMap<>();
    private static final Map<UUID, Long> PHASE_UNTIL = new HashMap<>();

    private PowerFieldState() {
    }

    public record Anchor(ResourceLocation dimension, BlockPos pos, long expiresAtTick) {
    }

    public static void setAnchor(ServerPlayer player, BlockPos pos, int durationTicks) {
        ANCHORS.put(player.getUUID(), new Anchor(
                player.level().dimension().location(),
                pos.immutable(),
                player.serverLevel().getGameTime() + durationTicks
        ));
    }

    /** The player's anchor if it is still live, in this dimension, and within reach of {@code near}. */
    public static Anchor activeAnchor(ServerPlayer player, BlockPos near) {
        Anchor anchor = ANCHORS.get(player.getUUID());
        if (anchor == null) {
            return null;
        }
        ServerLevel level = player.serverLevel();
        if (level.getGameTime() > anchor.expiresAtTick()
                || !anchor.dimension().equals(level.dimension().location())) {
            ANCHORS.remove(player.getUUID());
            return null;
        }
        return anchor.pos().distSqr(near) <= ANCHOR_RADIUS * ANCHOR_RADIUS ? anchor : null;
    }

    public static Anchor rawAnchor(ServerPlayer player) {
        return ANCHORS.get(player.getUUID());
    }

    public static void extendAnchor(ServerPlayer player, int extraTicks) {
        Anchor anchor = ANCHORS.get(player.getUUID());
        if (anchor != null) {
            ANCHORS.put(player.getUUID(), new Anchor(
                    anchor.dimension(), anchor.pos(), anchor.expiresAtTick() + extraTicks));
        }
    }

    public static void enterPhase(ServerPlayer player, int durationTicks) {
        PHASE_UNTIL.put(player.getUUID(), player.serverLevel().getGameTime() + durationTicks);
    }

    public static void extendPhase(ServerPlayer player, int extraTicks) {
        long now = player.serverLevel().getGameTime();
        long until = Math.max(now, PHASE_UNTIL.getOrDefault(player.getUUID(), now));
        PHASE_UNTIL.put(player.getUUID(), until + extraTicks);
    }

    public static boolean inPhase(ServerPlayer player) {
        Long until = PHASE_UNTIL.get(player.getUUID());
        if (until == null) {
            return false;
        }
        if (player.serverLevel().getGameTime() > until) {
            PHASE_UNTIL.remove(player.getUUID());
            return false;
        }
        return true;
    }

    public static void clear(ServerPlayer player) {
        ANCHORS.remove(player.getUUID());
        PHASE_UNTIL.remove(player.getUUID());
    }
}
