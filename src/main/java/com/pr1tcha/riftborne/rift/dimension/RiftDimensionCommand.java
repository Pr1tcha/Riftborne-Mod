package com.pr1tcha.riftborne.rift.dimension;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.pr1tcha.riftborne.registry.ModContent;
import com.pr1tcha.riftborne.rift.RiftSpawnLocator;
import com.pr1tcha.riftborne.rift.RiftSpawnProfile;
import com.pr1tcha.riftborne.rift.portal.RiftPortalBlockEntity;
import com.pr1tcha.riftborne.rift.run.RiftRunData;
import com.pr1tcha.riftborne.rift.run.RiftRunSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class RiftDimensionCommand {
    private RiftDimensionCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> category() {
        return Commands.literal("rift")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("enter")
                        .then(Commands.argument("tier", IntegerArgumentType.integer(1, 5))
                                .executes(context -> enter(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "tier")
                                ))))
                .then(Commands.literal("open")
                        .then(Commands.argument("tier", IntegerArgumentType.integer(1, 5))
                                .executes(context -> openPortal(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "tier")
                                ))))
                .then(Commands.literal("exit")
                        .executes(context -> exit(context.getSource())))
                .then(Commands.literal("info")
                        .executes(context -> info(context.getSource())))
                .then(Commands.literal("listRuns")
                        .executes(context -> listRuns(context.getSource())));
    }

    private static int enter(CommandSourceStack source, int tierLevel)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        RiftTier tier = RiftTier.fromLevel(tierLevel).orElseThrow();
        if (!RiftDimensions.enter(player, tier)) {
            source.sendFailure(Component.translatable("command.riftborne.rift_dimension.not_loaded", tierLevel));
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("command.riftborne.rift_dimension.entered",
                tier.level(), Component.translatable(tier.translationKey()))
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static int exit(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!RiftDimensions.exit(player)) {
            source.sendFailure(Component.translatable("command.riftborne.rift_dimension.not_inside"));
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("command.riftborne.rift_dimension.extracted"), false);
        return 1;
    }

    private static int info(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        RiftTier tier = RiftDimensions.currentTier(player).orElse(null);
        if (tier == null) {
            source.sendSuccess(() -> Component.translatable("command.riftborne.rift_dimension.current_not_standard"), false);
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("command.riftborne.rift_dimension.info_tier",
                tier.level(), Component.translatable(tier.translationKey()))
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        source.sendSuccess(() -> Component.translatable("command.riftborne.rift_dimension.info_dimension",
                tier.dimension().location().toString()).withStyle(ChatFormatting.DARK_GRAY), false);
        RiftDimensions.currentRun(player).ifPresent(run ->
                source.sendSuccess(() -> Component.literal(
                        "Run: " + run.runId() + " | anchor " + run.anchorPos().toShortString()
                                + " | players " + run.players().size()
                ).withStyle(ChatFormatting.GRAY), false));
        return tier.level();
    }

    private static int openPortal(CommandSourceStack source, int tierLevel) {
        RiftTier tier = RiftTier.fromLevel(tierLevel).orElseThrow();
        ServerLevel level = source.getLevel();
        if (RiftDimensions.isRiftDimension(level.dimension())) {
            source.sendFailure(Component.literal("Rift portals cannot be opened from inside a rift dimension."));
            return 0;
        }
        BlockPos origin = BlockPos.containing(source.getPosition());
        BlockPos pos = RiftSpawnLocator.findValidRiftPosition(level, origin, RiftSpawnProfile.NORMAL)
                .orElse(null);
        if (pos == null) {
            source.sendFailure(Component.literal("No clear 5x5x6 space found for a rift portal."));
            return 0;
        }

        level.setBlock(pos, ModContent.RIFT_PORTAL.get().defaultBlockState(), 3);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof RiftPortalBlockEntity portal)) {
            source.sendFailure(Component.literal("Rift portal block entity was not created."));
            return 0;
        }
        portal.configure(tier);
        source.sendSuccess(() -> Component.literal(
                "Opened tier " + tier.level() + " rift portal at " + pos.toShortString()
        ).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return 1;
    }

    private static int listRuns(CommandSourceStack source) {
        RiftRunSavedData storage = RiftRunSavedData.get(source.getServer());
        if (storage.runs().isEmpty()) {
            source.sendSuccess(() -> Component.literal("No saved rift runs."), false);
            return 0;
        }
        for (RiftRunData run : storage.runs()) {
            source.sendSuccess(() -> Component.literal(
                    run.runId() + " | tier " + run.tier().level() + " | " + run.state()
                            + " | " + run.anchorPos().toShortString()
                            + " | players " + run.players().size()
            ).withStyle(ChatFormatting.GRAY), false);
        }
        return storage.runs().size();
    }
}
