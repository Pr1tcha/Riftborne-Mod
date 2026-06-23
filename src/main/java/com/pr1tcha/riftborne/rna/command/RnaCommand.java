package com.pr1tcha.riftborne.rna.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.pr1tcha.riftborne.rna.RnaApi;
import com.pr1tcha.riftborne.rna.combat.RnaAbilityManager;
import com.pr1tcha.riftborne.rna.combat.ability.RnaAbility;
import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityData;
import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityResult;
import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityUseContext;
import com.pr1tcha.riftborne.rna.combat.registry.RnaAbilityRegistry;
import com.pr1tcha.riftborne.rna.data.FormationPath;
import com.pr1tcha.riftborne.rna.data.RnaData;
import com.pr1tcha.riftborne.rna.data.RnaStat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class RnaCommand {
    private RnaCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> rnaCategory() {
        return Commands.literal("rna")
                .then(getCommand())
                .then(profileCommand())
                .then(statsCommand())
                .then(metaWearCommand())
                .then(abilitiesCommand())
                // Compatibility aliases. They call the same implementation as the new tree.
                .then(Commands.literal("init")
                        .requires(RnaCommand::operator)
                        .executes(context -> initialize(
                                context.getSource(),
                                FormationPath.TRAINING,
                                context.getSource().getPlayerOrException())))
                .then(Commands.literal("reset")
                        .requires(RnaCommand::operator)
                        .executes(context -> reset(
                                context.getSource(),
                                context.getSource().getPlayerOrException())))
                .then(Commands.literal("set")
                        .requires(RnaCommand::operator)
                        .then(statArgument()
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                        .executes(context -> setStat(
                                                context.getSource(),
                                                context.getSource().getPlayerOrException(),
                                                stat(context),
                                                IntegerArgumentType.getInteger(context, "value"))))))
                .then(Commands.literal("path")
                        .then(Commands.literal("set")
                                .requires(RnaCommand::operator)
                                .then(pathArgument()
                                        .executes(context -> setPath(
                                                context.getSource(),
                                                context.getSource().getPlayerOrException(),
                                                path(context))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> getCommand() {
        return Commands.literal("get")
                .executes(context -> show(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(RnaCommand::operator)
                        .executes(context -> show(
                                context.getSource(),
                                EntityArgument.getPlayer(context, "player"))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> profileCommand() {
        return Commands.literal("profile")
                .then(Commands.literal("init")
                        .requires(RnaCommand::operator)
                        .then(pathArgument()
                                .executes(context -> initialize(
                                        context.getSource(),
                                        path(context),
                                        context.getSource().getPlayerOrException()))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> initialize(
                                                context.getSource(),
                                                path(context),
                                                EntityArgument.getPlayer(context, "target"))))))
                .then(Commands.literal("reset")
                        .requires(RnaCommand::operator)
                        .executes(context -> reset(
                                context.getSource(),
                                context.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> reset(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("path")
                        .then(Commands.literal("set")
                                .requires(RnaCommand::operator)
                                .then(pathArgument()
                                        .executes(context -> setPath(
                                                context.getSource(),
                                                context.getSource().getPlayerOrException(),
                                                path(context)))
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(context -> setPath(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "target"),
                                                        path(context)))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> statsCommand() {
        return Commands.literal("stats")
                .then(Commands.literal("set")
                        .requires(RnaCommand::operator)
                        .then(statArgument()
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                        .executes(context -> setStat(
                                                context.getSource(),
                                                context.getSource().getPlayerOrException(),
                                                stat(context),
                                                IntegerArgumentType.getInteger(context, "value")))
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(context -> setStat(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "target"),
                                                        stat(context),
                                                        IntegerArgumentType.getInteger(context, "value")))))))
                .then(Commands.literal("add")
                        .requires(RnaCommand::operator)
                        .then(statArgument()
                                .then(Commands.argument("amount", IntegerArgumentType.integer(-100, 100))
                                        .executes(context -> addStat(
                                                context.getSource(),
                                                context.getSource().getPlayerOrException(),
                                                stat(context),
                                                IntegerArgumentType.getInteger(context, "amount"),
                                                "command"))
                                        .then(Commands.argument("source", StringArgumentType.word())
                                                .executes(context -> addStat(
                                                        context.getSource(),
                                                        context.getSource().getPlayerOrException(),
                                                        stat(context),
                                                        IntegerArgumentType.getInteger(context, "amount"),
                                                        StringArgumentType.getString(context, "source")))
                                                .then(Commands.argument("target", EntityArgument.player())
                                                        .executes(context -> addStat(
                                                                context.getSource(),
                                                                EntityArgument.getPlayer(context, "target"),
                                                                stat(context),
                                                                IntegerArgumentType.getInteger(context, "amount"),
                                                                StringArgumentType.getString(context, "source"))))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> metaWearCommand() {
        return Commands.literal("metawear")
                .then(Commands.literal("get")
                        .executes(context -> showMetaWear(
                                context.getSource(),
                                context.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .requires(RnaCommand::operator)
                                .executes(context -> showMetaWear(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("set")
                        .requires(RnaCommand::operator)
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                .executes(context -> setMetaWear(
                                        context.getSource(),
                                        context.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(context, "value"),
                                        "command"))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> setMetaWear(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                IntegerArgumentType.getInteger(context, "value"),
                                                "command")))))
                .then(Commands.literal("add")
                        .requires(RnaCommand::operator)
                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 100))
                                .executes(context -> addMetaWear(
                                        context.getSource(),
                                        context.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(context, "value"),
                                        "command"))
                                .then(Commands.argument("source", StringArgumentType.word())
                                        .executes(context -> addMetaWear(
                                                context.getSource(),
                                                context.getSource().getPlayerOrException(),
                                                IntegerArgumentType.getInteger(context, "value"),
                                                StringArgumentType.getString(context, "source")))
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(context -> addMetaWear(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "target"),
                                                        IntegerArgumentType.getInteger(context, "value"),
                                                        StringArgumentType.getString(context, "source")))))))
                .then(Commands.literal("clear")
                        .requires(RnaCommand::operator)
                        .executes(context -> setMetaWear(
                                context.getSource(),
                                context.getSource().getPlayerOrException(),
                                0,
                                "command"))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> setMetaWear(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target"),
                                        0,
                                        "command"))))
                .then(Commands.literal("collapse")
                        .requires(RnaCommand::operator)
                        .executes(context -> collapse(
                                context.getSource(),
                                context.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> collapse(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> abilitiesCommand() {
        return Commands.literal("abilities")
                .then(Commands.literal("list")
                        .executes(context -> listAbilities(
                                context.getSource(),
                                context.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .requires(RnaCommand::operator)
                                .executes(context -> listAbilities(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("grant")
                        .requires(RnaCommand::operator)
                        .then(abilityArgument()
                                .executes(context -> changeAbility(
                                        context.getSource(),
                                        List.of(context.getSource().getPlayerOrException()),
                                        abilityId(context),
                                        true))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> changeAbility(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                abilityId(context),
                                                true)))))
                .then(Commands.literal("revoke")
                        .requires(RnaCommand::operator)
                        .then(abilityArgument()
                                .executes(context -> changeAbility(
                                        context.getSource(),
                                        List.of(context.getSource().getPlayerOrException()),
                                        abilityId(context),
                                        false))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> changeAbility(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                abilityId(context),
                                                false)))))
                .then(Commands.literal("cooldown")
                        .then(Commands.literal("get")
                                .then(abilityOrAllArgument()
                                        .executes(context -> showCooldown(
                                                context.getSource(),
                                                context.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(context, "ability")))
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .requires(RnaCommand::operator)
                                                .executes(context -> showCooldown(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "target"),
                                                        StringArgumentType.getString(context, "ability"))))))
                        .then(Commands.literal("clear")
                                .requires(RnaCommand::operator)
                                .then(abilityOrAllArgument()
                                        .executes(context -> clearCooldown(
                                                context.getSource(),
                                                List.of(context.getSource().getPlayerOrException()),
                                                StringArgumentType.getString(context, "ability")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(context -> clearCooldown(
                                                        context.getSource(),
                                                        EntityArgument.getPlayers(context, "targets"),
                                                        StringArgumentType.getString(context, "ability")))))))
                .then(Commands.literal("debug")
                        .requires(RnaCommand::operator)
                        .then(abilityArgument()
                                .executes(context -> debugAbility(
                                        context.getSource(),
                                        context.getSource().getPlayerOrException(),
                                        abilityId(context)))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> debugAbility(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                abilityId(context))))));
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

    private static int initialize(
            CommandSourceStack source,
            FormationPath path,
            ServerPlayer target
    ) {
        RnaApi.initialize(target, path);
        source.sendSuccess(() -> Component.literal("RNA initialized: "
                + target.getGameProfile().getName() + " [" + path.name() + "]"), true);
        return 1;
    }

    private static int reset(CommandSourceStack source, ServerPlayer target) {
        RnaApi.reset(target);
        source.sendSuccess(() -> Component.literal("RNA reset: " + target.getGameProfile().getName()), true);
        return 1;
    }

    private static int setPath(
            CommandSourceStack source,
            ServerPlayer target,
            FormationPath path
    ) {
        if (!RnaApi.hasActiveRna(target)) {
            source.sendFailure(Component.translatable("command.riftborne.rna.no_active_rna"));
            return 0;
        }
        if (!RnaApi.setFormationPath(target, path)) {
            source.sendFailure(Component.translatable("command.riftborne.rna.unknown_path"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(
                "command.riftborne.rna.path_set",
                Component.translatable(path.translationKey())), true);
        return 1;
    }

    private static int setStat(
            CommandSourceStack source,
            ServerPlayer target,
            RnaStat stat,
            int value
    ) {
        RnaApi.setStat(target, stat, value);
        source.sendSuccess(() -> Component.translatable(
                "command.riftborne.rna.stat_set",
                Component.translatable(stat.translationKey()),
                value), true);
        return 1;
    }

    private static int addStat(
            CommandSourceStack source,
            ServerPlayer target,
            RnaStat stat,
            int amount,
            String sourceId
    ) {
        RnaData data = RnaApi.get(target);
        int previous = data.getStat(stat);
        RnaApi.setStat(target, stat, previous + amount);
        int current = RnaApi.get(target).getStat(stat);
        source.sendSuccess(() -> Component.literal(stat.id() + ": " + previous + " -> " + current
                + " [" + sourceId + "]"), true);
        return Math.abs(current - previous);
    }

    private static int showMetaWear(CommandSourceStack source, ServerPlayer target) {
        RnaData data = RnaApi.get(target);
        source.sendSuccess(() -> Component.literal(target.getGameProfile().getName() + ": ")
                .append(Component.translatable(
                        "command.riftborne.rna.meta_wear",
                        data.metaWear(),
                        Component.translatable(stageKey(data.metaWearStage().name())))), false);
        return 1;
    }

    private static int setMetaWear(
            CommandSourceStack source,
            ServerPlayer target,
            int value,
            String sourceId
    ) {
        if (!RnaApi.hasActiveRna(target)) {
            source.sendFailure(Component.translatable("command.riftborne.rna.no_active_rna"));
            return 0;
        }
        RnaApi.setMetaWear(target, value, sourceId);
        return showMetaWear(source, target);
    }

    private static int addMetaWear(
            CommandSourceStack source,
            ServerPlayer target,
            int value,
            String sourceId
    ) {
        if (!RnaApi.addMetaWear(target, value, sourceId)) {
            source.sendFailure(Component.translatable("command.riftborne.rna.no_active_rna"));
            return 0;
        }
        return showMetaWear(source, target);
    }

    private static int collapse(CommandSourceStack source, ServerPlayer target) {
        RnaApi.collapse(target, "command");
        source.sendSuccess(() -> Component.literal("RNA collapsed: " + target.getGameProfile().getName()), true);
        return 1;
    }

    private static int listAbilities(CommandSourceStack source, ServerPlayer target) {
        RnaAbilityData data = RnaAbilityManager.getData(target);
        source.sendSuccess(() -> Component.literal("RNA abilities ["
                + target.getGameProfile().getName() + "]").withStyle(ChatFormatting.AQUA), false);
        for (RnaAbility ability : RnaAbilityRegistry.all()) {
            boolean unlocked = data.isUnlocked(ability.id().toString());
            long cooldown = RnaAbilityManager.remainingCooldown(target, ability.id());
            source.sendSuccess(() -> Component.literal(
                    (unlocked ? "[OPEN] " : "[LOCKED] ")
                            + ability.id()
                            + " " + ability.type()
                            + " cooldown=" + cooldown + "t"
            ).withStyle(unlocked ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY), false);
        }
        return data.unlockedAbilities().size();
    }

    private static int changeAbility(
            CommandSourceStack source,
            Collection<ServerPlayer> targets,
            ResourceLocation abilityId,
            boolean grant
    ) {
        if (RnaAbilityRegistry.get(abilityId) == null) {
            source.sendFailure(Component.literal("Unknown RNA ability: " + abilityId));
            return 0;
        }
        int changed = 0;
        for (ServerPlayer target : targets) {
            if (grant
                    ? RnaAbilityManager.grant(target, abilityId)
                    : RnaAbilityManager.revoke(target, abilityId)) {
                changed++;
            }
        }
        int result = changed;
        source.sendSuccess(() -> Component.literal((grant ? "Granted " : "Revoked ")
                + abilityId + " for " + result + " player(s)"), true);
        return changed;
    }

    private static int showCooldown(
            CommandSourceStack source,
            ServerPlayer target,
            String ability
    ) {
        if ("all".equalsIgnoreCase(ability)) {
            for (RnaAbility registered : RnaAbilityRegistry.all()) {
                long remaining = RnaAbilityManager.remainingCooldown(target, registered.id());
                source.sendSuccess(() -> Component.literal(
                        registered.id() + ": " + remaining + "t"), false);
            }
            return 1;
        }
        ResourceLocation id = parseAbility(ability);
        if (id == null || RnaAbilityRegistry.get(id) == null) {
            source.sendFailure(Component.literal("Unknown RNA ability: " + ability));
            return 0;
        }
        long remaining = RnaAbilityManager.remainingCooldown(target, id);
        source.sendSuccess(() -> Component.literal(id + ": " + remaining + "t"), false);
        return (int) Math.min(Integer.MAX_VALUE, remaining);
    }

    private static int clearCooldown(
            CommandSourceStack source,
            Collection<ServerPlayer> targets,
            String ability
    ) {
        if ("all".equalsIgnoreCase(ability)) {
            targets.forEach(RnaAbilityManager::clearCooldowns);
        } else {
            ResourceLocation id = parseAbility(ability);
            if (id == null || RnaAbilityRegistry.get(id) == null) {
                source.sendFailure(Component.literal("Unknown RNA ability: " + ability));
                return 0;
            }
            targets.forEach(target -> RnaAbilityManager.clearCooldown(target, id));
        }
        source.sendSuccess(() -> Component.literal("RNA ability cooldown cleared: " + ability), true);
        return targets.size();
    }

    private static int debugAbility(
            CommandSourceStack source,
            ServerPlayer target,
            ResourceLocation abilityId
    ) {
        RnaAbility ability = RnaAbilityRegistry.get(abilityId);
        if (ability == null) {
            source.sendFailure(Component.literal("Unknown RNA ability: " + abilityId));
            return 0;
        }
        RnaAbilityData data = RnaAbilityManager.getData(target);
        RnaAbilityResult result = RnaAbilityManager.checkUse(
                target,
                abilityId,
                new RnaAbilityUseContext(
                        target,
                        target.serverLevel(),
                        null,
                        target.blockPosition(),
                        "debug",
                        "ability_debug",
                        false
                )
        );
        RnaData rna = RnaApi.get(target);
        source.sendSuccess(() -> Component.literal("Ability: " + ability.id()
                + " type=" + ability.type()
                + " unlocked=" + data.isUnlocked(ability.id().toString())
                + " result=" + RnaAbilityManager.describeResult(result)), false);
        source.sendSuccess(() -> Component.literal("RNA: active=" + RnaApi.hasActiveRna(target)
                + " path=" + rna.formationPath()
                + " stage=" + rna.metaWearStage()
                + " wear=" + rna.metaWear()
                + " cooldown=" + RnaAbilityManager.remainingCooldown(target, abilityId) + "t"), false);
        return result == RnaAbilityResult.SUCCESS ? 1 : 0;
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> statArgument() {
        return Commands.argument("stat", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        Arrays.stream(RnaStat.values()).map(RnaStat::id),
                        builder
                ));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> pathArgument() {
        return Commands.argument("path", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        Arrays.stream(FormationPath.values())
                                .filter(FormationPath::isSelectable)
                                .map(Enum::name),
                        builder
                ));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> abilityArgument() {
        return Commands.argument("ability", StringArgumentType.string())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        RnaAbilityRegistry.ids(),
                        builder
                ));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> abilityOrAllArgument() {
        return Commands.argument("ability", StringArgumentType.string())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        java.util.stream.Stream.concat(
                                java.util.stream.Stream.of("all"),
                                RnaAbilityRegistry.ids().stream()
                        ),
                        builder
                ));
    }

    private static RnaStat stat(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        RnaStat stat = RnaStat.fromId(StringArgumentType.getString(context, "stat"));
        if (stat == null) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
        }
        return stat;
    }

    private static FormationPath path(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        FormationPath path = FormationPath.fromId(StringArgumentType.getString(context, "path"));
        if (!path.isSelectable()) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
        }
        return path;
    }

    private static ResourceLocation abilityId(
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        ResourceLocation id = parseAbility(StringArgumentType.getString(context, "ability"));
        if (id == null) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
        }
        return id;
    }

    private static ResourceLocation parseAbility(String value) {
        ResourceLocation parsed = ResourceLocation.tryParse(value.toLowerCase(Locale.ROOT));
        return parsed == null ? null : parsed;
    }

    private static boolean operator(CommandSourceStack source) {
        return source.hasPermission(2);
    }

    private static Component localizedBoolean(boolean value) {
        return Component.translatable("options." + (value ? "on" : "off"));
    }

    private static String stageKey(String stage) {
        return "rna.riftborne.meta_wear_stage." + stage.toLowerCase(Locale.ROOT);
    }
}
