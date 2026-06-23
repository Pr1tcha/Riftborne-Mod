package com.pr1tcha.riftborne.riftwalker.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.pr1tcha.riftborne.registry.ModContent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class RiftwalkerCommand {
    private RiftwalkerCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> category() {
        return Commands.literal("riftwalker")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("give").executes(context -> giveSet(context.getSource())));
    }

    private static int giveSet(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        giveOrDrop(player, new ItemStack(ModContent.RIFTWALKER_HOOD.get()));
        giveOrDrop(player, new ItemStack(ModContent.RIFTWALKER_COAT.get()));
        giveOrDrop(player, new ItemStack(ModContent.RIFTWALKER_LEGGINGS.get()));
        giveOrDrop(player, new ItemStack(ModContent.RIFTWALKER_BOOTS.get()));
        source.sendSuccess(() -> net.minecraft.network.chat.Component.translatable(
                "command.riftborne.riftwalker.given"), false);
        return 1;
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
