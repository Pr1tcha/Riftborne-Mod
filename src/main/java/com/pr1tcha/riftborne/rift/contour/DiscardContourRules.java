package com.pr1tcha.riftborne.rift.contour;

import com.pr1tcha.riftborne.Riftborne;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player.BedSleepingProblem;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;

@EventBusSubscriber(modid = Riftborne.MODID)
public final class DiscardContourRules {
    private static final Component CONTOUR_HOLDS_MESSAGE =
            Component.translatable("message.riftborne.contour.holds");

    private DiscardContourRules() {
    }

    @SubscribeEvent
    public static void keepDeathInsideContour(PlayerRespawnPositionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!player.serverLevel().dimension().equals(RiftContourTeleporter.DISCARD_CONTOUR)) {
            return;
        }

        ServerLevel contour = player.getServer().getLevel(RiftContourTeleporter.DISCARD_CONTOUR);
        if (contour == null) {
            return;
        }

        RiftContourTeleporter.buildContourAnchor(contour);
        event.setDimensionTransition(new DimensionTransition(
                contour,
                new Vec3(RiftContourTeleporter.contourSpawn().getX() + 0.5D, RiftContourTeleporter.contourSpawn().getY(), RiftContourTeleporter.contourSpawn().getZ() + 0.5D),
                Vec3.ZERO,
                player.getYRot(),
                player.getXRot(),
                DimensionTransition.DO_NOTHING
        ));
    }

    @SubscribeEvent
    public static void blockTeleportCommandsFromContour(EntityTeleportEvent.TeleportCommand event) {
        if (event.getEntity() instanceof ServerPlayer player && player.serverLevel().dimension().equals(RiftContourTeleporter.DISCARD_CONTOUR)) {
            event.setCanceled(true);
            player.displayClientMessage(CONTOUR_HOLDS_MESSAGE, true);
        }
    }

    @SubscribeEvent
    public static void blockSpreadPlayersCommandFromContour(EntityTeleportEvent.SpreadPlayersCommand event) {
        if (event.getEntity() instanceof ServerPlayer player && player.serverLevel().dimension().equals(RiftContourTeleporter.DISCARD_CONTOUR)) {
            event.setCanceled(true);
            player.displayClientMessage(CONTOUR_HOLDS_MESSAGE, true);
        }
    }

    @SubscribeEvent
    public static void blockTeleportCommandInputFromContour(CommandEvent event) {
        if (!(event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer player)
                || !player.serverLevel().dimension().equals(RiftContourTeleporter.DISCARD_CONTOUR)) {
            return;
        }

        String command = event.getParseResults().getReader().getString().trim().toLowerCase(java.util.Locale.ROOT);
        if (command.equals("tp") || command.startsWith("tp ")
                || command.equals("teleport") || command.startsWith("teleport ")
                || command.equals("spreadplayers") || command.startsWith("spreadplayers ")) {
            event.setCanceled(true);
            player.displayClientMessage(CONTOUR_HOLDS_MESSAGE, true);
        }
    }

    @SubscribeEvent
    public static void blockSleepingInContour(CanPlayerSleepEvent event) {
        if (event.getEntity().serverLevel().dimension().equals(RiftContourTeleporter.DISCARD_CONTOUR)) {
            event.setProblem(BedSleepingProblem.NOT_POSSIBLE_HERE);
        }
    }
}
