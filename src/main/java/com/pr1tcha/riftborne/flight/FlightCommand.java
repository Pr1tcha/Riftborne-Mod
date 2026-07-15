package com.pr1tcha.riftborne.flight;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class FlightCommand {
    private static final SuggestionProvider<CommandSourceStack> PRESET_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(FlightSettings.presetNames(), builder);

    private FlightCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> category() {
        return Commands.literal("flight")
                .then(Commands.literal("grant")
                        .executes(context -> grant(context.getSource(), List.of(context.getSource().getPlayerOrException()), FlightSettings.defaults(), "normal"))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> grant(context.getSource(), EntityArgument.getPlayers(context, "targets"), FlightSettings.defaults(), "normal"))
                                .then(Commands.argument("preset", StringArgumentType.word())
                                        .suggests(PRESET_SUGGESTIONS)
                                        .executes(context -> grantPreset(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                StringArgumentType.getString(context, "preset")
                                        ))
                                )
                        )
                )
                .then(Commands.literal("grant_custom")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("speedMultiplier", DoubleArgumentType.doubleArg(0.05D, 10.0D))
                                        .then(Commands.argument("accelerationMultiplier", DoubleArgumentType.doubleArg(0.05D, 10.0D))
                                                .then(Commands.argument("maxSpeed", DoubleArgumentType.doubleArg(0.05D, 10.0D))
                                                        .then(Commands.argument("hover", BoolArgumentType.bool())
                                                                .then(Commands.argument("steeringMultiplier", DoubleArgumentType.doubleArg(0.05D, 5.0D))
                                                                        .then(Commands.argument("brakingMultiplier", DoubleArgumentType.doubleArg(0.05D, 10.0D))
                                                                                .then(Commands.argument("verticalSpeedMultiplier", DoubleArgumentType.doubleArg(0.0D, 5.0D))
                                                                                        .then(Commands.argument("elytraPose", BoolArgumentType.bool())
                                                                                                .executes(context -> grantCustom(context.getSource(), EntityArgument.getPlayers(context, "targets"),
                                                                                                        DoubleArgumentType.getDouble(context, "speedMultiplier"),
                                                                                                        DoubleArgumentType.getDouble(context, "accelerationMultiplier"),
                                                                                                        DoubleArgumentType.getDouble(context, "maxSpeed"),
                                                                                                        BoolArgumentType.getBool(context, "hover"),
                                                                                                        DoubleArgumentType.getDouble(context, "steeringMultiplier"),
                                                                                                        DoubleArgumentType.getDouble(context, "brakingMultiplier"),
                                                                                                        DoubleArgumentType.getDouble(context, "verticalSpeedMultiplier"),
                                                                                                        BoolArgumentType.getBool(context, "elytraPose")))
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
                .then(Commands.literal("revoke")
                        .executes(context -> revoke(context.getSource(), List.of(context.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> revoke(context.getSource(), EntityArgument.getPlayers(context, "targets")))
                        )
                )
                .then(settingsCategory());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> settingsCategory() {
        return Commands.literal("settings")
                .then(Commands.literal("get")
                        .executes(context -> showSettings(context.getSource(), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> showSettings(context.getSource(), EntityArgument.getPlayer(context, "target")))
                        )
                )
                .then(Commands.literal("preset")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("preset", StringArgumentType.word())
                                        .suggests(PRESET_SUGGESTIONS)
                                        .executes(context -> applyPreset(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                StringArgumentType.getString(context, "preset")
                                        ))
                                )
                        )
                )
                .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(doubleSetting("speed", 0.05D, 10.0D, FlightSettings::withSpeedMultiplier))
                                .then(doubleSetting("acceleration", 0.05D, 10.0D, FlightSettings::withAccelerationMultiplier))
                                .then(doubleSetting("max_speed", 0.05D, 10.0D, FlightSettings::withMaxSpeed))
                                .then(doubleSetting("steering", 0.05D, 5.0D, FlightSettings::withSteeringMultiplier))
                                .then(doubleSetting("braking", 0.05D, 10.0D, FlightSettings::withBrakingMultiplier))
                                .then(doubleSetting("vertical", 0.0D, 5.0D, FlightSettings::withVerticalSpeedMultiplier))
                                .then(booleanSetting("hover", FlightSettings::withHoverEnabled))
                                .then(booleanSetting("elytra_pose", FlightSettings::withElytraPoseOnBoost))
                        )
                );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> doubleSetting(String name, double min, double max, DoubleSettingUpdater updater) {
        return Commands.literal(name)
                .then(Commands.argument("value", DoubleArgumentType.doubleArg(min, max))
                        .executes(context -> updateSettings(
                                context.getSource(),
                                EntityArgument.getPlayers(context, "targets"),
                                settings -> updater.update(settings, DoubleArgumentType.getDouble(context, "value"))
                        ))
                );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> booleanSetting(String name, BooleanSettingUpdater updater) {
        return Commands.literal(name)
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> updateSettings(
                                context.getSource(),
                                EntityArgument.getPlayers(context, "targets"),
                                settings -> updater.update(settings, BoolArgumentType.getBool(context, "value"))
                        ))
                );
    }

    private static int grantPreset(CommandSourceStack source, Collection<ServerPlayer> targets, String preset) throws CommandSyntaxException {
        FlightSettings settings = FlightSettings.preset(preset);
        if (settings == null) {
            source.sendFailure(Component.literal("Unknown flight preset: " + preset));
            return 0;
        }
        return grant(source, targets, settings, preset);
    }

    private static int grantCustom(
            CommandSourceStack source,
            Collection<ServerPlayer> targets,
            double speedMultiplier,
            double accelerationMultiplier,
            double maxSpeed,
            boolean hover,
            double steeringMultiplier,
            double brakingMultiplier,
            double verticalSpeedMultiplier,
            boolean elytraPose
    ) throws CommandSyntaxException {
        FlightSettings settings = new FlightSettings(
                speedMultiplier,
                accelerationMultiplier,
                maxSpeed,
                steeringMultiplier,
                brakingMultiplier,
                verticalSpeedMultiplier,
                hover,
                elytraPose
        ).sanitize();
        return grant(source, targets, settings, "custom");
    }

    private static int grant(CommandSourceStack source, Collection<ServerPlayer> targets, FlightSettings settings, String profileName) throws CommandSyntaxException {
        if (targets.isEmpty()) {
            source.sendFailure(Component.literal("No players selected."));
            return 0;
        }

        for (ServerPlayer player : targets) {
            FlightAbility.setAbility(player, true, settings);
        }

        source.sendSuccess(() -> Component.literal("Flight " + profileName + " granted to " + targets.size()
                + " player(s): " + settings.describe()), true);
        return targets.size();
    }

    private static int revoke(CommandSourceStack source, Collection<ServerPlayer> targets) throws CommandSyntaxException {
        if (targets.isEmpty()) {
            source.sendFailure(Component.literal("No players selected."));
            return 0;
        }

        for (ServerPlayer player : targets) {
            FlightAbility.setAbility(player, false);
        }

        source.sendSuccess(() -> Component.literal("Flight revoked from " + targets.size() + " player(s)."), true);
        return targets.size();
    }

    private static int applyPreset(CommandSourceStack source, Collection<ServerPlayer> targets, String preset) throws CommandSyntaxException {
        FlightSettings settings = FlightSettings.preset(preset);
        if (settings == null) {
            source.sendFailure(Component.literal("Unknown flight preset: " + preset));
            return 0;
        }
        return updateSettings(source, targets, ignored -> settings);
    }

    private static int updateSettings(CommandSourceStack source, Collection<ServerPlayer> targets, SettingsUpdater updater) throws CommandSyntaxException {
        if (targets.isEmpty()) {
            source.sendFailure(Component.literal("No players selected."));
            return 0;
        }

        int updated = 0;
        int skipped = 0;
        FlightSettings lastSettings = null;
        for (ServerPlayer player : targets) {
            if (!FlightAbility.hasAbility(player)) {
                skipped++;
                continue;
            }
            FlightSettings settings = updater.update(FlightAbility.getSettings(player)).sanitize();
            FlightAbility.setSettings(player, settings);
            lastSettings = settings;
            updated++;
        }

        if (updated == 0) {
            source.sendFailure(Component.literal("No selected players have flight ability."));
            return 0;
        }

        FlightSettings shownSettings = lastSettings;
        int updatedPlayers = updated;
        int skippedPlayers = skipped;
        source.sendSuccess(() -> Component.literal("Updated flight settings for " + updatedPlayers + " player(s)"
                + (skippedPlayers > 0 ? ", skipped " + skippedPlayers + " without flight" : "")
                + ": " + shownSettings.describe()), true);
        return updated;
    }

    private static int showSettings(CommandSourceStack source, ServerPlayer target) {
        String status = FlightAbility.hasAbility(target) ? "enabled" : "not granted";
        source.sendSuccess(() -> Component.literal("Flight for " + target.getGameProfile().getName()
                + " is " + status + ": " + FlightAbility.getSettings(target).describe()), false);
        return 1;
    }

    @FunctionalInterface
    private interface SettingsUpdater {
        FlightSettings update(FlightSettings settings);
    }

    @FunctionalInterface
    private interface DoubleSettingUpdater {
        FlightSettings update(FlightSettings settings, double value);
    }

    @FunctionalInterface
    private interface BooleanSettingUpdater {
        FlightSettings update(FlightSettings settings, boolean value);
    }
}
