package com.pr1tcha.riftborne.rna;

import com.pr1tcha.riftborne.codex.data.CodexData;
import com.pr1tcha.riftborne.player.RiftbornePlayerData;
import com.pr1tcha.riftborne.rna.data.FormationPath;
import com.pr1tcha.riftborne.rna.data.MetaWearStage;
import com.pr1tcha.riftborne.rna.data.RnaData;
import com.pr1tcha.riftborne.rna.data.RnaStat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public final class RnaApi {
    private RnaApi() {
    }

    public static RnaData get(Player player) {
        return RiftbornePlayerData.getRna(player);
    }

    public static boolean hasActiveRna(Player player) {
        RnaData data = get(player);
        return data.hasRNA() && !data.rnaCollapsed();
    }

    public static void initialize(ServerPlayer player, FormationPath path) {
        RnaData data = get(player);
        data.initialize(path);
        save(player, data);

        CodexData codex = RiftbornePlayerData.getCodex(player);
        codex.addNotification("Профиль РНА инициализирован.");
        codex.addRecentData("Сформирована РНА: " + data.formationPath().name());
        codex.unlock("connectivity");
        RiftbornePlayerData.saveCodex(player, codex);
    }

    public static void reset(ServerPlayer player) {
        RnaData data = get(player);
        data.reset();
        save(player, data);
    }

    public static void setStat(ServerPlayer player, RnaStat stat, int value) {
        RnaData data = get(player);
        data.setStat(stat, value);
        save(player, data);
    }

    public static int addStatGrowth(ServerPlayer player, RnaStat stat, int baseAmount, String source) {
        RnaData data = get(player);
        if (!data.hasRNA() || baseAmount <= 0) {
            return 0;
        }
        double stageMultiplier = switch (data.metaWearStage()) {
            case STABLE -> 1.0D;
            case STRAIN -> 0.9D;
            case DISTORTION -> 0.65D;
            case REJECTION, ARCHITECTURE_BREAK -> 0.0D;
        };
        if (stageMultiplier <= 0.0D) {
            return 0;
        }
        int adjusted = Math.max(1, Mth.floor(
                baseAmount * data.formationPath().growthMultiplier(stat) * stageMultiplier
        ));
        int previous = data.getStat(stat);
        data.setStat(stat, previous + adjusted);
        save(player, data);

        CodexData codex = RiftbornePlayerData.getCodex(player);
        codex.addRecentData(stat.id() + " +" + (data.getStat(stat) - previous) + " [" + source + "]");
        RiftbornePlayerData.saveCodex(player, codex);
        return data.getStat(stat) - previous;
    }

    public static boolean setFormationPath(ServerPlayer player, FormationPath path) {
        if (path == null || !path.isSelectable()) {
            return false;
        }
        RnaData data = get(player);
        data.setFormationPath(path);
        save(player, data);
        return true;
    }

    public static void setMetaWear(ServerPlayer player, int value, String source) {
        RnaData data = get(player);
        if (!data.hasRNA()) {
            return;
        }
        MetaWearStage previous = data.metaWearStage();
        data.setMetaWear(value);
        data.setLastMetaWearTick(player.serverLevel().getGameTime());
        save(player, data);
        onStageChanged(player, data, previous, source);
    }

    public static boolean addMetaWear(ServerPlayer player, int amount, String source) {
        if (amount <= 0) {
            return false;
        }

        RnaData data = get(player);
        if (!data.hasRNA()) {
            return false;
        }
        if (data.criticalInstability()) {
            collapse(player, source);
            return true;
        }

        double resistanceReduction = 1.0D - data.overloadResistance() * 0.003D;
        int adjustedAmount = Math.max(1, Mth.ceil(amount * data.formationPath().metaWearMultiplier() * resistanceReduction));
        MetaWearStage previous = data.metaWearStage();
        data.setMetaWear(data.metaWear() + adjustedAmount);
        data.setLastMetaWearTick(player.serverLevel().getGameTime());
        save(player, data);
        onStageChanged(player, data, previous, source);
        return true;
    }

    public static void reduceMetaWear(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return;
        }
        RnaData data = get(player);
        if (!data.hasRNA() || data.metaWearStage() == MetaWearStage.ARCHITECTURE_BREAK) {
            return;
        }
        MetaWearStage previous = data.metaWearStage();
        data.setMetaWear(data.metaWear() - amount);
        data.setLastMetaWearTick(player.serverLevel().getGameTime());
        save(player, data);
        onStageChanged(player, data, previous, "passive_decay");
    }

    public static void collapse(ServerPlayer player, String source) {
        RnaData data = get(player);
        data.collapse();
        save(player, data);
        player.sendSystemMessage(Component.translatable("message.riftborne.rna.collapsed").withStyle(ChatFormatting.DARK_RED));

        CodexData codex = RiftbornePlayerData.getCodex(player);
        codex.addNotification("РНА разрушена. Синхронный доступ утрачен.");
        codex.addRecentData("Зарегистрирован распад РНА: " + source);
        codex.unlock("meta_wear");
        RiftbornePlayerData.saveCodex(player, codex);
    }

    private static void onStageChanged(ServerPlayer player, RnaData data, MetaWearStage previous, String source) {
        if (data.metaWearStage() == previous) {
            return;
        }
        data.setLastWarningTick(player.serverLevel().getGameTime());
        save(player, data);
        player.sendSystemMessage(data.metaWearStage().transitionMessage().withStyle(stageColor(data.metaWearStage())));

        CodexData codex = RiftbornePlayerData.getCodex(player);
        codex.addNotification(data.metaWearStage().transitionMessage().getString());
        codex.addRecentData("Мета-износ: " + data.metaWear() + "% [" + source + "]");
        if (data.metaWearStage().ordinal() >= MetaWearStage.DISTORTION.ordinal()) {
            codex.unlock("meta_wear");
        }
        RiftbornePlayerData.saveCodex(player, codex);
    }

    private static ChatFormatting stageColor(MetaWearStage stage) {
        return switch (stage) {
            case STABLE -> ChatFormatting.GREEN;
            case STRAIN -> ChatFormatting.YELLOW;
            case DISTORTION -> ChatFormatting.GOLD;
            case REJECTION -> ChatFormatting.RED;
            case ARCHITECTURE_BREAK -> ChatFormatting.DARK_RED;
        };
    }

    private static void save(Player player, RnaData data) {
        RiftbornePlayerData.saveRna(player, data);
    }
}
