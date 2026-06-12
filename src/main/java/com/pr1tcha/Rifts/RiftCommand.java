package com.pr1tcha.Rifts;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.pr1tcha.Rifts.RiftData.RiftBlockEntity;
import com.pr1tcha.Rifts.RiftData.RiftData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class RiftCommand {
    private static final List<String> TIME_UNITS = List.of("sec", "t");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rift")
                .requires(source -> source.hasPermission(2))

                .then(Commands.literal("stage")
                        .then(Commands.literal("get")
                                .executes(context -> getStage(context.getSource()))
                        )
                        .then(Commands.literal("set")
                                .then(Commands.argument("stage", IntegerArgumentType.integer(0, 4))
                                        .executes(context -> setStage(context.getSource(), IntegerArgumentType.getInteger(context, "stage")))
                                )
                        )
                )

                .then(Commands.literal("kill")
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 100))
                                .executes(context -> killRifts(context.getSource(), IntegerArgumentType.getInteger(context, "radius")))
                        )
                        .executes(context -> killRifts(context.getSource(), 10))
                )

                .then(Commands.literal("info")
                        .then(Commands.argument("searchRadius", IntegerArgumentType.integer(1, 100))
                                .executes(context -> getRiftInfo(context.getSource(), IntegerArgumentType.getInteger(context, "searchRadius")))
                        )
                        .executes(context -> getRiftInfo(context.getSource(), 5))
                )

                .then(Commands.literal("spawn")
                        .executes(context -> spawnRift(context.getSource(), BlockPos.containing(context.getSource().getPosition()), 20, 5.0f))

                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> spawnRift(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"), 20, 5.0f))

                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("unit", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(TIME_UNITS, builder))
                                                .executes(context -> {
                                                    BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
                                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                                    String unit = StringArgumentType.getString(context, "unit");
                                                    int lifetimeTicks = toTicks(amount, unit);
                                                    return spawnRift(context.getSource(), pos, lifetimeTicks, 5.0f);
                                                })

                                                .then(Commands.argument("radius", FloatArgumentType.floatArg(1.0f, 50.0f))
                                                        .executes(context -> {
                                                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
                                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                                            String unit = StringArgumentType.getString(context, "unit");
                                                            float radius = FloatArgumentType.getFloat(context, "radius");
                                                            int lifetimeTicks = toTicks(amount, unit);
                                                            return spawnRift(context.getSource(), pos, lifetimeTicks, radius);
                                                        })
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static int toTicks(int amount, String unit) {
        return "sec".equalsIgnoreCase(unit) ? amount * 20 : amount;
    }

    private static int getStage(CommandSourceStack source) {
        int stage = RiftWorldStage.getStage(source.getLevel());
        source.sendSuccess(() -> Component.literal("Riftborne rift stage: " + stage), false);
        return stage;
    }

    private static int setStage(CommandSourceStack source, int stage) {
        MinecraftServer server = source.getServer();
        if (!RiftWorldStage.setStage(source.getLevel(), stage, server)) {
            source.sendFailure(Component.literal("Riftborne rift stage gamerule is not registered yet."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Riftborne rift stage set to " + stage), true);
        return stage;
    }

    private static int spawnRift(CommandSourceStack source, BlockPos pos, int lifetimeTicks, float radius) {
        ServerLevel level = source.getLevel();
        if (lifetimeTicks < 20) {
            lifetimeTicks = 20;
        }

        level.setBlock(pos, ModContent.RIFT_BLOCK.get().defaultBlockState(), 3);

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof RiftBlockEntity rift) {
            RiftData data = rift.getData();
            data.maxLifetimeTicks = lifetimeTicks;
            data.radius = radius;
            data.isCommandSpawned = true;
            rift.setChanged();
        }

        int finalTicks = lifetimeTicks;
        source.sendSuccess(() -> Component.literal(String.format(
                "Rift opened at %s. Lifetime: %d t (%.1f sec), radius: %.1f blocks",
                pos.toShortString(), finalTicks, finalTicks / 20.0f, radius
        )), true);

        return 1;
    }

    private static int killRifts(CommandSourceStack source, int radius) {
        ServerLevel level = source.getLevel();
        BlockPos playerPos = BlockPos.containing(source.getPosition());
        AABB box = new AABB(playerPos).inflate(radius);
        int killedCount = 0;

        for (BlockPos targetPos : BlockPos.betweenClosed(
                BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {

            if (level.getBlockState(targetPos).is(ModContent.RIFT_BLOCK.get())) {
                level.removeBlock(targetPos, false);
                killedCount++;
            }
        }

        int finalKilledCount = killedCount;
        source.sendSuccess(() -> Component.literal(String.format(
                "Removed rifts in %d block radius: %d",
                radius, finalKilledCount
        )), true);

        return killedCount;
    }

    private static int getRiftInfo(CommandSourceStack source, int searchRadius) {
        ServerLevel level = source.getLevel();
        BlockPos playerPos = BlockPos.containing(source.getPosition());
        BlockPos foundPos = null;
        double closestDist = Double.MAX_VALUE;
        AABB box = new AABB(playerPos).inflate(searchRadius);

        for (BlockPos targetPos : BlockPos.betweenClosed(
                BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {

            if (level.getBlockState(targetPos).is(ModContent.RIFT_BLOCK.get())) {
                double dist = playerPos.distSqr(targetPos);
                if (dist < closestDist) {
                    closestDist = dist;
                    foundPos = targetPos.immutable();
                }
            }
        }

        if (foundPos == null) {
            source.sendFailure(Component.literal(String.format("No rift found within %d blocks.", searchRadius)));
            return 0;
        }

        BlockEntity blockEntity = level.getBlockEntity(foundPos);
        if (!(blockEntity instanceof RiftBlockEntity riftTile)) {
            source.sendFailure(Component.literal("Rift block found, but rift data is missing or corrupted."));
            return 0;
        }

        RiftData data = riftTile.getData();
        int leftTicks = Math.max(0, data.maxLifetimeTicks - data.ticksExisted);
        boolean isQuest = data.isQuestRelated || data.isCommandSpawned;
        String statusText = isQuest ? "Scripted" : "Natural";
        ChatFormatting statusColor = isQuest ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GREEN;

        source.sendSuccess(() -> Component.literal("ID: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(data.id.toString()).withStyle(ChatFormatting.GRAY)), false);
        source.sendSuccess(() -> Component.literal("Position: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(data.centerPos.toShortString()).withStyle(ChatFormatting.WHITE)), false);
        source.sendSuccess(() -> Component.literal("Type: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(data.riftType.toString()).withStyle(ChatFormatting.GRAY)), false);
        source.sendSuccess(() -> Component.literal("Stage: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(data.stage.name()).withStyle(ChatFormatting.GOLD)), false);
        source.sendSuccess(() -> Component.literal("Radius: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(data.radius + " blocks").withStyle(ChatFormatting.YELLOW)), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "Lifetime: %d / %d t (left: %.1f sec)",
                data.ticksExisted, data.maxLifetimeTicks, leftTicks / 20.0f
        )).withStyle(ChatFormatting.GREEN), false);
        source.sendSuccess(() -> Component.literal("Rift source: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(statusText).withStyle(statusColor)), false);

        return 1;
    }
}
