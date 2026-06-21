package com.pr1tcha.riftborne.rift.dimension;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

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
                .then(Commands.literal("exit")
                        .executes(context -> exit(context.getSource())))
                .then(Commands.literal("info")
                        .executes(context -> info(context.getSource())));
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
                tier.dimension().location()).withStyle(ChatFormatting.DARK_GRAY), false);
        return tier.level();
    }
}
