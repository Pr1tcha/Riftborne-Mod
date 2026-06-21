package com.pr1tcha.riftborne.rna.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.pr1tcha.riftborne.rna.RnaApi;
import com.pr1tcha.riftborne.rna.data.FormationPath;
import com.pr1tcha.riftborne.rna.data.RnaData;
import com.pr1tcha.riftborne.rna.data.RnaStat;
import java.util.Arrays;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class RnaCommand {
    private RnaCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> rnaCategory() {
        return Commands.literal("rna")
                .then(Commands.literal("get")
                        .executes(context -> show(context.getSource(), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> show(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("init")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> initialize(context.getSource())))
                .then(Commands.literal("reset")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> reset(context.getSource())))
                .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("stat", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(RnaStat.values()).map(RnaStat::id), builder))
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                        .executes(context -> setStat(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "stat"),
                                                IntegerArgumentType.getInteger(context, "value"))))))
                .then(Commands.literal("path")
                        .then(Commands.literal("set")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("path", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(FormationPath.values())
                                                        .filter(FormationPath::isSelectable)
                                                        .map(Enum::name),
                                                builder))
                                        .executes(context -> setPath(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "path"))))));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> metaWearCategory() {
        return Commands.literal("metawear")
                .then(Commands.literal("get")
                        .executes(context -> showMetaWear(context.getSource())))
                .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                .executes(context -> setMetaWear(context.getSource(), IntegerArgumentType.getInteger(context, "value")))))
                .then(Commands.literal("add")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 100))
                                .executes(context -> addMetaWear(context.getSource(), IntegerArgumentType.getInteger(context, "value")))))
                .then(Commands.literal("clear")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> setMetaWear(context.getSource(), 0)))
                .then(Commands.literal("collapse")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> collapse(context.getSource())));
    }

    private static int show(CommandSourceStack source, ServerPlayer player) {
        RnaData data = RnaApi.get(player);
        source.sendSuccess(() -> Component.translatable("command.riftborne.rna.header", player.getGameProfile().getName())
                .withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.translatable("command.riftborne.rna.state",
                localizedBoolean(data.hasRNA()),
                Component.translatable(data.formationPath().translationKey()),
                localizedBoolean(data.rnaCollapsed())), false);
        source.sendSuccess(() -> Component.translatable("command.riftborne.rna.stats",
                data.nodeDensity(), data.connectivity(), data.throughput(), data.overloadResistance()), false);
        source.sendSuccess(() -> Component.translatable("command.riftborne.rna.meta_wear_state",
                data.metaWear(),
                Component.translatable(stageKey(data.metaWearStage().name())),
                localizedBoolean(data.criticalInstability())), false);
        return 1;
    }

    private static int initialize(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        RnaApi.initialize(player, FormationPath.TRAINING);
        source.sendSuccess(() -> Component.translatable("command.riftborne.rna.initialized"), false);
        return 1;
    }

    private static int reset(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        RnaApi.reset(source.getPlayerOrException());
        source.sendSuccess(() -> Component.translatable("command.riftborne.rna.reset"), false);
        return 1;
    }

    private static int setStat(CommandSourceStack source, String statId, int value) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        RnaStat stat = RnaStat.fromId(statId);
        if (stat == null) {
            source.sendFailure(Component.translatable("command.riftborne.rna.unknown_stat", statId));
            return 0;
        }
        RnaApi.setStat(source.getPlayerOrException(), stat, value);
        source.sendSuccess(() -> Component.translatable("command.riftborne.rna.stat_set",
                Component.translatable(stat.translationKey()), value), false);
        return 1;
    }

    private static int setPath(CommandSourceStack source, String pathId) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        FormationPath path = FormationPath.fromId(pathId);
        if (!RnaApi.setFormationPath(source.getPlayerOrException(), path)) {
            source.sendFailure(Component.translatable("command.riftborne.rna.unknown_path"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.riftborne.rna.path_set",
                Component.translatable(path.translationKey())), false);
        return 1;
    }

    private static int showMetaWear(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        RnaData data = RnaApi.get(source.getPlayerOrException());
        source.sendSuccess(() -> Component.translatable("command.riftborne.rna.meta_wear",
                data.metaWear(), Component.translatable(stageKey(data.metaWearStage().name()))), false);
        return 1;
    }

    private static int setMetaWear(CommandSourceStack source, int value) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        RnaApi.setMetaWear(player, value, "command");
        return showMetaWear(source);
    }

    private static int addMetaWear(CommandSourceStack source, int value) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!RnaApi.addMetaWear(player, value, "command")) {
            source.sendFailure(Component.translatable("command.riftborne.rna.no_active_rna"));
            return 0;
        }
        return showMetaWear(source);
    }

    private static int collapse(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        RnaApi.collapse(source.getPlayerOrException(), "command");
        return 1;
    }

    private static Component localizedBoolean(boolean value) {
        return Component.translatable("options." + (value ? "on" : "off"));
    }

    private static String stageKey(String stage) {
        return "rna.riftborne.meta_wear_stage." + stage.toLowerCase(java.util.Locale.ROOT);
    }
}
