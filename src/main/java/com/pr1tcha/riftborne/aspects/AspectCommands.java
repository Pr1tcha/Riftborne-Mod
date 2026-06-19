package com.pr1tcha.riftborne.aspects;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.pr1tcha.riftborne.aspects.telekinesis.TelekinesisCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class AspectCommands {
    private AspectCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> category() {
        return Commands.literal("aspects")
                .then(TelekinesisCommand.category());
    }
}
