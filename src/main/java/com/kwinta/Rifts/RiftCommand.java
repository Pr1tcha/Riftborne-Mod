package com.kwinta.Rifts;

import com.kwinta.Rifts.RiftData.RiftBlockEntity;
import com.kwinta.Rifts.RiftData.RiftData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class RiftCommand {
    private static final List<String> TIME_UNITS = List.of("sec", "t");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rift")
                .requires(source -> source.hasPermission(2))

                // 1. Подкоманда /rift kill [радиус]
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
                        .executes(context -> getRiftInfo(context.getSource(), 5)) // По умолчанию радиус поиска равен 5
                )

                // 3. Подкоманда /rift spawn (аргументы идут цепочкой внутрь)
                .then(Commands.literal("spawn")
                        // /rift spawn (под себя, 1 сек, радиус 5)
                        .executes(context -> spawnRift(context.getSource(), BlockPos.containing(context.getSource().getPosition()), 20, 5.0f))

                        // /rift spawn <x> <y> <z>
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> spawnRift(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"), 20, 5.0f))

                                // /rift spawn <x> <y> <z> <amount>
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        // /rift spawn <x> <y> <z> <amount> [sec / t]
                                        .then(Commands.argument("unit", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(TIME_UNITS, builder))
                                                .executes(context -> {
                                                    BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
                                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                                    String unit = StringArgumentType.getString(context, "unit");
                                                    int lifetimeTicks = unit.equals("sec") ? amount * 20 : amount;
                                                    return spawnRift(context.getSource(), pos, lifetimeTicks, 5.0f); // радиус 5 по дефолту
                                                })

                                                // /rift spawn <x> <y> <z> <amount> [sec / t] <radius>
                                                .then(Commands.argument("radius", FloatArgumentType.floatArg(1.0f, 50.0f))
                                                        .executes(context -> {
                                                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
                                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                                            String unit = StringArgumentType.getString(context, "unit");
                                                            float radius = FloatArgumentType.getFloat(context, "radius");
                                                            int lifetimeTicks = unit.equals("sec") ? amount * 20 : amount;
                                                            return spawnRift(context.getSource(), pos, lifetimeTicks, radius);
                                                        })
                                                )
                                        )
                                )
                        )
                )
        );
    }

    // Логика создания разлома с кастомным радиусом
    private static int spawnRift(CommandSourceStack source, BlockPos pos, int lifetimeTicks, float radius) {
        ServerLevel level = source.getLevel();
        if (lifetimeTicks < 20) lifetimeTicks = 20;

        level.setBlock(pos, ModContent.RIFT_BLOCK.get().defaultBlockState(), 3);

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof RiftBlockEntity rift) {
            RiftData data = rift.getData();
            data.maxLifetimeTicks = lifetimeTicks;
            data.radius = radius; // Устанавливаем радиус из команды
            data.isCommandSpawned = true;
            rift.setChanged();
        }

        int finalTicks = lifetimeTicks;
        source.sendSuccess(() -> Component.literal(
                String.format("Разлом открыт на %s. Время жизни: %d t (%.1f sec), Радиус: %.1f блоков",
                        pos.toShortString(), finalTicks, finalTicks / 20.0f, radius)
        ), true);

        return 1;
    }

    // Логика удаления разломов
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
        source.sendSuccess(() -> Component.literal(
                String.format("Удалено разломов в радиусе %d блоков: %d", radius, finalKilledCount)
        ), true);

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
            source.sendFailure(Component.literal(String.format("Поблизости (в радиусе %d б.) не найдено ни одного разлома!", searchRadius)));
            return 0;
        }

        BlockEntity blockEntity = level.getBlockEntity(foundPos);
        if (!(blockEntity instanceof RiftBlockEntity riftTile)) {
            source.sendFailure(Component.literal("Ошибка: Разлом найден, но данные разлома повреждены."));
            return 0;
        }

        RiftData data = riftTile.getData();

        source.sendSuccess(() -> Component.literal("ID: ").withStyle(ChatFormatting.GRAY).append(Component.literal(data.id.toString()).withStyle(ChatFormatting.GRAY)), false);
        source.sendSuccess(() -> Component.literal("Позиция: ").withStyle(ChatFormatting.GRAY).append(Component.literal(data.centerPos.toShortString()).withStyle(ChatFormatting.WHITE)), false);
        source.sendSuccess(() -> Component.literal("Тип: ").withStyle(ChatFormatting.GRAY).append(Component.literal(data.riftType.toString()).withStyle(ChatFormatting.GRAY)), false);
        source.sendSuccess(() -> Component.literal("Стадия: ").withStyle(ChatFormatting.GRAY).append(Component.literal(data.stage.name()).withStyle(ChatFormatting.GOLD)), false);
        source.sendSuccess(() -> Component.literal("Радиус: ").withStyle(ChatFormatting.GRAY).append(Component.literal(data.radius + " блоков").withStyle(ChatFormatting.YELLOW)), false);

        int leftTicks = Math.max(0, data.maxLifetimeTicks - data.ticksExisted);
        source.sendSuccess(() -> Component.literal(
                String.format("Время жизни: %d / %d t (Осталось: %.1f сек)", data.ticksExisted, data.maxLifetimeTicks, leftTicks / 20.0f)
        ).withStyle(ChatFormatting.GREEN), false);

        boolean isQuest = data.isQuestRelated || data.isCommandSpawned;
        String statusText = isQuest ? "Квестовый (команда)" : "Натуральный";
        ChatFormatting statusColor = isQuest ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GREEN;

        source.sendSuccess(() -> Component.literal("Тип разлома: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(statusText).withStyle(statusColor)), false);

        return 1;
    }
}
