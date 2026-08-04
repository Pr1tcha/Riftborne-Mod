package com.pr1tcha.riftborne.rna.power.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.pr1tcha.riftborne.rna.power.Primitive;
import com.pr1tcha.riftborne.rna.power.AdaptationService;
import com.pr1tcha.riftborne.rna.power.PowerApi;
import com.pr1tcha.riftborne.rna.power.PowerPractice;
import com.pr1tcha.riftborne.rna.power.PowerCast;
import com.pr1tcha.riftborne.rna.power.PowerCrystallization;
import com.pr1tcha.riftborne.rna.power.PowerGates;
import com.pr1tcha.riftborne.rna.power.PowerRules;
import com.pr1tcha.riftborne.rna.power.data.RNAProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Operator/testing command for the PS V2.5 power system while there is no in-world input yet.
 * {@code /riftborne power activate|reset|get|cast <primitive>}.
 */
public final class PowerCommand {
    private PowerCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> category() {
        LiteralArgumentBuilder<CommandSourceStack> cast = Commands.literal("cast");
        for (Primitive primitive : Primitive.values()) {
            cast.then(Commands.literal(primitive.id())
                    .executes(ctx -> cast(ctx.getSource(), primitive)));
        }
        return Commands.literal("power")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("activate")
                        .executes(ctx -> activate(ctx.getSource(), "training"))
                        .then(Commands.argument("path", StringArgumentType.word())
                                .executes(ctx -> activate(ctx.getSource(), StringArgumentType.getString(ctx, "path")))))
                .then(Commands.literal("reset")
                        .executes(ctx -> reset(ctx.getSource())))
                .then(Commands.literal("get")
                        .executes(ctx -> get(ctx.getSource())))
                .then(Commands.literal("connectivity")
                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 3))
                                .executes(ctx -> setConnectivity(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "value")))))
                .then(Commands.literal("progress")
                        .executes(ctx -> progress(ctx.getSource())))
                .then(Commands.literal("crystallize")
                        .executes(ctx -> crystallize(ctx.getSource())))
                .then(Commands.literal("adaptation")
                        .executes(ctx -> adaptation(ctx.getSource()))
                        .then(Commands.literal("complete")
                                .executes(ctx -> completeCycle(ctx.getSource()))))
                .then(cast);
    }

    private static int activate(CommandSourceStack source, String path) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PowerApi.activate(player, path);
        RNAProfile profile = PowerApi.get(player);
        source.sendSuccess(() -> Component.literal("RNA activated: path=" + profile.formationPath()
                + " C" + profile.connectivity() + String.format(" thr=%.1f", profile.throughput())).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int reset(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        PowerApi.reset(source.getPlayerOrException());
        source.sendSuccess(() -> Component.literal("RNA profile reset.").withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int get(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        RNAProfile p = PowerApi.get(player);
        source.sendSuccess(() -> Component.literal(String.format(
                "RNA[%s] path=%s C%d thr=%.1f nd=%.1f or=%.1f wear=%.1f window=%.1f",
                p.active() ? "active" : "dormant", p.formationPath(), p.connectivity(),
                p.throughput(), p.nodeDensity(), p.overloadRes(), p.metaWear(),
                PowerRules.admissibilityWindow(p))).withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "gates: phys=%.0f mental=%d psych=%d genetic=%.2f | caps: nd/or≤%d C≤%d",
                PowerGates.phys(player), PowerGates.mental(player), PowerGates.psych(player),
                PowerGates.genetic(player), PowerGates.physicalStatCap(player),
                PowerGates.connectivityCap(player))).withStyle(ChatFormatting.DARK_AQUA), false);
        return 1;
    }


    /** Full readout of the current adaptation cycle: what has been earned but not yet applied. */
    private static int adaptation(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        com.pr1tcha.riftborne.rna.power.data.PhysicalProfile phys = AdaptationService.physical(player);
        com.pr1tcha.riftborne.rna.power.data.AdaptationCycle cycle = AdaptationService.cycle(player);
        RNAProfile rna = PowerApi.get(player);

        source.sendSuccess(() -> Component.literal(String.format(
                "physical: str=%.1f end=%.1f res=%.1f rec=%.1f load=%.1f",
                phys.strength(), phys.endurance(), phys.resilience(), phys.recovery(),
                phys.physicalLoad())).withStyle(ChatFormatting.GREEN), false);

        source.sendSuccess(() -> Component.literal(String.format(
                "cycle: %d/%d ticks | episodes=%d",
                cycle.activeTicks(), com.pr1tcha.riftborne.config.Config.adaptationCycleTicks.get(),
                cycle.recoveryEpisodes())).withStyle(ChatFormatting.GRAY), false);

        StringBuilder activity = new StringBuilder("activity:");
        for (com.pr1tcha.riftborne.rna.power.data.PhysicalStat stat
                : com.pr1tcha.riftborne.rna.power.data.PhysicalStat.values()) {
            activity.append(String.format(" %s=%.1f(+%.2f)", stat.id(), cycle.activity(stat),
                    AdaptationService.basePhysicalGrowth(cycle.activity(stat))
                            * AdaptationService.diminishing(phys.get(stat))));
        }
        source.sendSuccess(() -> Component.literal(activity.toString()).withStyle(ChatFormatting.DARK_GREEN), false);

        StringBuilder practice = new StringBuilder("practice:");
        for (com.pr1tcha.riftborne.rna.power.data.RnaTrack track
                : com.pr1tcha.riftborne.rna.power.data.RnaTrack.values()) {
            practice.append(String.format(" %s=%.1f", track.id(), cycle.practice(track)));
        }
        practice.append(String.format(" | connectivityThisCycle=%.2f progress=%.1f",
                PowerPractice.connectivityPractice(cycle), rna.connectivityProgress()));
        source.sendSuccess(() -> Component.literal(practice.toString()).withStyle(ChatFormatting.AQUA), false);

        source.sendSuccess(() -> Component.literal("primitiveUses: " + cycle.primitiveUses())
                .withStyle(ChatFormatting.DARK_AQUA), false);
        return 1;
    }

    /** Close the cycle immediately so a full cycle can be verified without waiting 20 minutes. */
    private static int completeCycle(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        AdaptationService.completeCycle(player, AdaptationService.cycle(player));
        source.sendSuccess(() -> Component.literal("Adaptation cycle closed and applied.")
                .withStyle(ChatFormatting.YELLOW), false);
        return adaptation(source);
    }

    private static int setConnectivity(CommandSourceStack source, int value) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        RNAProfile p = PowerApi.get(player);
        PowerApi.set(player, p.withStats(p.throughput(), value, p.nodeDensity(), p.overloadRes()));
        source.sendSuccess(() -> Component.literal("Connectivity set to C" + value).withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int progress(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var prog = PowerApi.getProgress(player);
        source.sendSuccess(() -> Component.literal("practice=" + prog.practiceByAxis()
                + " total=" + prog.totalPractice()
                + " facet=" + (prog.facet().isEmpty() ? "none"
                        : prog.facet().get().signature() + prog.facet().get().dominantAxes()))
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static int crystallize(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean formed = PowerCrystallization.tryCrystallize(player);
        source.sendSuccess(() -> Component.literal(formed ? "Facet crystallized."
                : "Cannot crystallize (need active RNA, no facet, practice≥"
                        + PowerCrystallization.MIN_PRACTICE + ").").withStyle(ChatFormatting.GRAY), false);
        return formed ? 1 : 0;
    }

    private static int cast(CommandSourceStack source, Primitive primitive) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PowerCast.Outcome outcome = PowerCast.cast(player, primitive);
        ChatFormatting color = outcome.result() == PowerCast.Result.OK ? ChatFormatting.GREEN
                : outcome.result() == PowerCast.Result.COMPENSATION ? ChatFormatting.RED : ChatFormatting.YELLOW;
        source.sendSuccess(() -> Component.literal(String.format(
                "%s → %s | load=%.1f window=%.1f%s",
                primitive.id(), outcome.result(), outcome.load(), outcome.window(),
                outcome.overload() ? " OVERLOAD" : "")).withStyle(color), false);
        return outcome.result() == PowerCast.Result.OK ? 1 : 0;
    }
}
