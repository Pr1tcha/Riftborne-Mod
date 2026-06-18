package com.pr1tcha.riftborne.telekinesis;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class TelekinesisCommand {
    private TelekinesisCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> category() {
        return Commands.literal("telekinesis")
                .then(Commands.literal("grant")
                        .executes(context -> setAbility(context.getSource(), List.of(context.getSource().getPlayerOrException()), true))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> setAbility(context.getSource(), EntityArgument.getPlayers(context, "targets"), true))
                        )
                )
                .then(Commands.literal("revoke")
                        .executes(context -> setAbility(context.getSource(), List.of(context.getSource().getPlayerOrException()), false))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> setAbility(context.getSource(), EntityArgument.getPlayers(context, "targets"), false))
                        )
                );
    }

    private static int setAbility(CommandSourceStack source, Collection<ServerPlayer> targets, boolean enabled) throws CommandSyntaxException {
        if (targets.isEmpty()) {
            source.sendFailure(Component.literal("No players selected."));
            return 0;
        }

        for (ServerPlayer player : targets) {
            TelekinesisAbility.setAbility(player, enabled);
        }

        String action = enabled ? "granted to" : "revoked from";
        source.sendSuccess(() -> Component.literal("Telekinesis " + action + " " + targets.size() + " player(s)."), true);
        return targets.size();
    }
}
