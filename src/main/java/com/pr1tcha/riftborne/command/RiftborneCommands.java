package com.pr1tcha.riftborne.command;

import com.mojang.brigadier.CommandDispatcher;
import com.pr1tcha.riftborne.rift.command.RiftCommand;
import com.pr1tcha.riftborne.aspects.AspectCommands;
import com.pr1tcha.riftborne.rna.command.RnaCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class RiftborneCommands {
    private RiftborneCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("riftborne")
                .then(RiftCommand.riftsCategory())
                .then(RiftCommand.contourCategory())
                .then(AspectCommands.category())
                .then(RnaCommand.rnaCategory())
                .then(RnaCommand.metaWearCategory())
        );
    }
}
