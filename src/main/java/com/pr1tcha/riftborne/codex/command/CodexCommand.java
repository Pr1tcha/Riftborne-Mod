package com.pr1tcha.riftborne.codex.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.pr1tcha.riftborne.codex.data.entry.CodexEntryDefinition;
import com.pr1tcha.riftborne.codex.data.entry.CodexEntryRegistry;
import com.pr1tcha.riftborne.codex.data.state.CodexEntryState;
import com.pr1tcha.riftborne.codex.storage.CodexEntryProgress;
import com.pr1tcha.riftborne.codex.storage.CodexPlayerProgress;
import com.pr1tcha.riftborne.codex.storage.CodexStorage;
import java.util.Arrays;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class CodexCommand {
    private CodexCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> category() {
        return Commands.literal("codex")
                .then(Commands.literal("list")
                        .executes(context -> list(context.getSource())))
                .then(Commands.literal("status")
                        .then(entryArgument()
                                .executes(context -> status(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "entry")))))
                .then(Commands.literal("unlock")
                        .requires(source -> source.hasPermission(2))
                        .then(entryArgument()
                                .executes(context -> changeState(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "entry"),
                                        CodexEntryState.UNLOCKED))))
                .then(Commands.literal("lock")
                        .requires(source -> source.hasPermission(2))
                        .then(entryArgument()
                                .executes(context -> lock(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "entry")))))
                .then(Commands.literal("set_state")
                        .requires(source -> source.hasPermission(2))
                        .then(entryArgument()
                                .then(Commands.argument("state", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(CodexEntryState.values()).map(Enum::name),
                                                builder
                                        ))
                                        .executes(context -> setState(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "entry"),
                                                StringArgumentType.getString(context, "state"))))))
                .then(Commands.literal("reset")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> reset(context.getSource())))
                .then(Commands.literal("give_fragment")
                        .requires(source -> source.hasPermission(2))
                        .then(entryArgument()
                                .executes(context -> giveFragments(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "entry"),
                                        1))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(context -> giveFragments(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "entry"),
                                                IntegerArgumentType.getInteger(context, "amount"))))))
                .then(Commands.literal("decrypt")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("start")
                                .then(entryArgument()
                                        .executes(context -> changeState(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "entry"),
                                                CodexEntryState.NEEDS_DECRYPTION))))
                        .then(Commands.literal("complete")
                                .then(entryArgument()
                                        .executes(context -> completeDecryption(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "entry"))))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> entryArgument() {
        return Commands.argument("entry", StringArgumentType.string())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(CodexEntryRegistry.ids(), builder));
    }

    private static int list(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CodexPlayerProgress progress = CodexStorage.get(player);
        source.sendSuccess(() -> Component.literal("Codex: " + player.getGameProfile().getName())
                .withStyle(ChatFormatting.AQUA), false);
        int known = 0;
        for (CodexEntryDefinition entry : CodexEntryRegistry.all()) {
            CodexEntryProgress state = CodexStorage.status(progress, entry);
            if (state.known()) {
                known++;
                source.sendSuccess(() -> line(entry, state), false);
            }
        }
        if (known == 0) {
            source.sendSuccess(() -> Component.literal("Нет известных записей.")
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return known;
    }

    private static int status(CommandSourceStack source, String id)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CodexEntryDefinition entry = CodexEntryRegistry.get(id);
        if (entry == null) {
            return unknown(source, id);
        }
        CodexEntryProgress progress = CodexStorage.status(
                CodexStorage.get(source.getPlayerOrException()),
                entry
        );
        source.sendSuccess(() -> line(entry, progress), false);
        source.sendSuccess(() -> Component.literal(entry.summary()).withStyle(ChatFormatting.GRAY), false);
        if (entry.decryptData().enabled()) {
            source.sendSuccess(() -> Component.literal("Дешифровка: " + progress.decryptProgress()
                    + "/" + entry.decryptData().requiredFragments()).withStyle(ChatFormatting.DARK_AQUA), false);
        }
        return 1;
    }

    private static Component line(CodexEntryDefinition entry, CodexEntryProgress progress) {
        return Component.literal(entry.id() + " [" + progress.state().name() + "] ")
                .withStyle(stateColor(progress.state()))
                .append(Component.literal(entry.title()).withStyle(ChatFormatting.WHITE));
    }

    private static int changeState(CommandSourceStack source, String id, CodexEntryState state)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (!CodexStorage.setState(source.getPlayerOrException(), id, state)) {
            return unknown(source, id);
        }
        source.sendSuccess(() -> Component.literal(id + " -> " + state.name()), true);
        return 1;
    }

    private static int lock(CommandSourceStack source, String id)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (!CodexStorage.lock(source.getPlayerOrException(), id)) {
            return unknown(source, id);
        }
        source.sendSuccess(() -> Component.literal(id + " -> LOCKED"), true);
        return 1;
    }

    private static int setState(CommandSourceStack source, String id, String rawState)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        try {
            return changeState(source, id, CodexEntryState.fromString(rawState));
        } catch (IllegalArgumentException exception) {
            source.sendFailure(Component.literal("Неизвестное состояние: " + rawState));
            return 0;
        }
    }

    private static int reset(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CodexStorage.reset(source.getPlayerOrException());
        source.sendSuccess(() -> Component.literal("Прогресс Кодекса сброшен."), true);
        return 1;
    }

    private static int giveFragments(CommandSourceStack source, String id, int amount)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        int progress = CodexStorage.giveFragments(source.getPlayerOrException(), id, amount);
        if (progress < 0) {
            source.sendFailure(Component.literal("Запись не найдена или не требует фрагментов: " + id));
            return 0;
        }
        CodexEntryDefinition entry = CodexEntryRegistry.get(id);
        source.sendSuccess(() -> Component.literal("Дешифровка " + id + ": " + progress
                + "/" + entry.decryptData().requiredFragments()), true);
        return progress;
    }

    private static int completeDecryption(CommandSourceStack source, String id)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (!CodexStorage.completeDecryption(source.getPlayerOrException(), id)) {
            source.sendFailure(Component.literal("Запись не найдена или не имеет настроек дешифровки: " + id));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Дешифровка завершена: " + id), true);
        return 1;
    }

    private static int unknown(CommandSourceStack source, String id) {
        source.sendFailure(Component.literal("Неизвестная запись Кодекса: " + id));
        return 0;
    }

    private static ChatFormatting stateColor(CodexEntryState state) {
        return switch (state) {
            case UNLOCKED -> ChatFormatting.GREEN;
            case PARTIAL -> ChatFormatting.YELLOW;
            case DAMAGED, NEEDS_DECRYPTION -> ChatFormatting.RED;
            case ENCRYPTED -> ChatFormatting.LIGHT_PURPLE;
            case LOCKED -> ChatFormatting.DARK_GRAY;
        };
    }
}
