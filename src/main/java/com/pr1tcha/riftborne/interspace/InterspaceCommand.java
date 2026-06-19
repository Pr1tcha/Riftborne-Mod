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
                        enter(context.getSource(), InterspaceDimensions.RNA_INTERSPACE, "RNA Interspace")))
                .then(Commands.literal("riftwalker").executes(context ->
                        enter(context.getSource(), InterspaceDimensions.RIFTWALKER_INTERSPACE, "Riftwalker Interspace")))
                .then(Commands.literal("return").executes(context -> returnToOrigin(context.getSource())));
    }

    private static int enter(CommandSourceStack source, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, String name)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!InterspaceDimensions.enter(player, dimension)) {
            source.sendFailure(Component.literal(name + " is not loaded."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Entered " + name + "."), false);
        return 1;
    }

    private static int returnToOrigin(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!InterspaceDimensions.isInterspace(player.serverLevel().dimension())) {
            source.sendFailure(Component.literal("You are not inside an interspace."));
            return 0;
        }
        InterspaceDimensions.returnToOrigin(player);
        source.sendSuccess(() -> Component.literal("Returned from the interspace."), false);
        return 1;
    }
}
