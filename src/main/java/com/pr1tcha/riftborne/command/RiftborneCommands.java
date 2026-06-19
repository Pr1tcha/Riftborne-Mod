package com.pr1tcha.riftborne.command;

import com.mojang.brigadier.CommandDispatcher;
import com.pr1tcha.riftborne.rift.command.RiftCommand;
import com.pr1tcha.riftborne.interspace.InterspaceCommand;
import com.pr1tcha.riftborne.telekinesis.TelekinesisCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class RiftborneCommands {
    private RiftborneCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("riftborne")
                .requires(source -> source.hasPermission(2))
                .then(RiftCommand.riftsCategory())
                .then(RiftCommand.contourCategory())
                .then(InterspaceCommand.category())
                .then(TelekinesisCommand.category())
        );
    }
}
