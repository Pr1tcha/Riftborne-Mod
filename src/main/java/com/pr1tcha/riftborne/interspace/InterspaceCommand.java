package com.pr1tcha.riftborne.interspace;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class InterspaceCommand {
    private InterspaceCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> category() {
        return Commands.literal("interspace")
                .then(Commands.literal("rna").executes(context ->
                        enter(context.getSource(), InterspaceDimensions.RNA_INTERSPACE, "dimension.riftborne.rna_interspace")))
                .then(Commands.literal("riftwalker").executes(context ->
                        enter(context.getSource(), InterspaceDimensions.RIFTWALKER_INTERSPACE, "dimension.riftborne.riftwalker_interspace")))
                .then(Commands.literal("return").executes(context -> returnToOrigin(context.getSource())));
    }

    private static int enter(CommandSourceStack source, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, String dimensionKey)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!InterspaceDimensions.enter(player, dimension)) {
            source.sendFailure(Component.translatable("command.riftborne.interspace.not_loaded",
                    Component.translatable(dimensionKey)));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.riftborne.interspace.entered",
                Component.translatable(dimensionKey)), false);
        return 1;
    }

    private static int returnToOrigin(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!InterspaceDimensions.isInterspace(player.serverLevel().dimension())) {
            source.sendFailure(Component.translatable("command.riftborne.interspace.not_inside"));
            return 0;
        }
        InterspaceDimensions.returnToOrigin(player);
        source.sendSuccess(() -> Component.translatable("command.riftborne.interspace.returned"), false);
        return 1;
    }
}
