package com.pr1tcha.riftborne.command;

import com.mojang.brigadier.CommandDispatcher;
import com.pr1tcha.riftborne.codex.command.CodexCommand;
import com.pr1tcha.riftborne.interspace.InterspaceCommand;
import com.pr1tcha.riftborne.rna.command.RnaCommand;
import com.pr1tcha.riftborne.rift.command.RiftCommand;
import com.pr1tcha.riftborne.rift.dimension.RiftDimensionCommand;
import com.pr1tcha.riftborne.riftwalker.command.RiftwalkerCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class RiftborneCommands {
    private RiftborneCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("riftborne")
                .then(RiftDimensionCommand.category())
                .then(RiftCommand.riftsCategory())
                .then(RiftCommand.contourCategory())
                .then(RnaCommand.rnaCategory())
                .then(InterspaceCommand.category())
                .then(CodexCommand.category())
                .then(RiftwalkerCommand.category())
        );
    }
}
