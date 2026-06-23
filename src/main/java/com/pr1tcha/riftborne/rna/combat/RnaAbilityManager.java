package com.pr1tcha.riftborne.rna.combat;

import com.pr1tcha.riftborne.player.RiftbornePlayerData;
import com.pr1tcha.riftborne.rna.RnaApi;
import com.pr1tcha.riftborne.rna.combat.ability.RnaAbility;
import com.pr1tcha.riftborne.rna.combat.cooldown.RnaCooldown;
import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityCost;
import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityData;
import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityResult;
import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityUseContext;
import com.pr1tcha.riftborne.rna.combat.registry.RnaAbilityRegistry;
import com.pr1tcha.riftborne.rna.combat.scaling.RnaStageModifiers;
import com.pr1tcha.riftborne.rna.data.MetaWearStage;
import com.pr1tcha.riftborne.rna.data.RnaData;
import com.pr1tcha.riftborne.rna.data.RnaStat;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

public final class RnaAbilityManager {
    public static final String LEGACY_TELEKINESIS_TAG = "RiftborneTelekinesis";

    private RnaAbilityManager() {
    }

    public static RnaAbilityData getData(ServerPlayer player) {
        RnaAbilityData data = RiftbornePlayerData.getRnaCombat(player);
        if (migrateLegacyTelekinesis(player, data)) {
            save(player, data);
        }
        return data;
    }

    public static boolean isUnlocked(ServerPlayer player, ResourceLocation abilityId) {
        return getData(player).isUnlocked(abilityId.toString());
    }

    public static boolean grant(ServerPlayer player, ResourceLocation abilityId) {
        if (RnaAbilityRegistry.get(abilityId) == null) {
            return false;
        }
        RnaAbilityData data = getData(player);
        boolean changed = data.unlock(abilityId.toString());
        save(player, data);
        return changed;
    }

    public static boolean revoke(ServerPlayer player, ResourceLocation abilityId) {
        if (RnaAbilityRegistry.get(abilityId) == null) {
            return false;
        }
        RnaAbilityData data = getData(player);
        boolean changed = data.revoke(abilityId.toString());
        save(player, data);
        return changed;
    }

    public static RnaAbilityResult checkUse(
            ServerPlayer player,
            ResourceLocation abilityId,
            RnaAbilityUseContext context
    ) {
        RnaAbility ability = RnaAbilityRegistry.get(abilityId);
        if (ability == null) {
            return RnaAbilityResult.FAIL_UNKNOWN_ABILITY;
        }
        RnaData rna = RnaApi.get(player);
        if (ability.requirement().requiresActiveRna() && !RnaApi.hasActiveRna(player)) {
            return RnaAbilityResult.FAIL_NO_RNA;
        }
        RnaAbilityData data = getData(player);
        if (!data.isUnlocked(abilityId.toString())) {
            return RnaAbilityResult.FAIL_LOCKED;
        }
        long gameTime = player.serverLevel().getGameTime();
        if (data.cooldownUntil(abilityId.toString()) > gameTime) {
            return RnaAbilityResult.FAIL_COOLDOWN;
        }
        RnaStageModifiers modifiers = RnaStageModifiers.forStage(rna.metaWearStage());
        return ability.requirement().check(rna, context, modifiers, ability.heavy());
    }

    public static RnaAbilityResult completeSuccessfulUse(
            ServerPlayer player,
            ResourceLocation abilityId,
            RnaAbilityUseContext context
    ) {
        RnaAbilityResult check = checkUse(player, abilityId, context);
        if (check != RnaAbilityResult.SUCCESS) {
            return check;
        }
        if (!context.meaningful()) {
            return RnaAbilityResult.FAIL_CONTEXT;
        }

        RnaAbility ability = RnaAbilityRegistry.get(abilityId);
        RnaData before = RnaApi.get(player);
        MetaWearStage stageBefore = before.metaWearStage();
        RnaStageModifiers modifiers = RnaStageModifiers.forStage(stageBefore);
        RnaAbilityCost cost = ability.costFor(context.action());
        int adjustedWear = cost.baseMetaWear() <= 0
                ? 0
                : Math.max(1, Mth.ceil(cost.baseMetaWear() * modifiers.metaWearMultiplier()));

        if (adjustedWear > 0 && !RnaApi.addMetaWear(player, adjustedWear, context.source())) {
            return RnaAbilityResult.FAIL_NO_RNA;
        }

        long gameTime = player.serverLevel().getGameTime();
        RnaAbilityData data = getData(player);
        data.setLastUseTick(abilityId.toString(), gameTime);
        int cooldown = Mth.ceil(ability.cooldownTicks() * modifiers.cooldownMultiplier());
        if (cooldown > 0) {
            data.setCooldownUntil(abilityId.toString(), gameTime + cooldown);
        }

        if (RnaApi.hasActiveRna(player)) {
            applyGrowth(player, data, ability, cost, stageBefore, gameTime, context.source());
        }
        save(player, data);
        return RnaAbilityResult.SUCCESS;
    }

    public static long remainingCooldown(ServerPlayer player, ResourceLocation abilityId) {
        long until = getData(player).cooldownUntil(abilityId.toString());
        return new RnaCooldown(until).remaining(player.serverLevel().getGameTime());
    }

    public static void clearCooldown(ServerPlayer player, ResourceLocation abilityId) {
        RnaAbilityData data = getData(player);
        data.clearCooldown(abilityId.toString());
        save(player, data);
    }

    public static void clearCooldowns(ServerPlayer player) {
        RnaAbilityData data = getData(player);
        data.clearCooldowns();
        save(player, data);
    }

    private static void applyGrowth(
            ServerPlayer player,
            RnaAbilityData data,
            RnaAbility ability,
            RnaAbilityCost cost,
            MetaWearStage stageBefore,
            long gameTime,
            String source
    ) {
        if (cost.growthStat() != null && cost.growthAmount() > 0
                && canGrow(data, ability.id(), cost.growthStat(), gameTime)) {
            int grown = RnaApi.addStatGrowth(player, cost.growthStat(), cost.growthAmount(), source);
            if (grown > 0) {
                setGrowthCooldown(data, ability.id(), cost.growthStat(), gameTime + cost.growthCooldownTicks());
            }
        }

        if (stageBefore.ordinal() >= MetaWearStage.STRAIN.ordinal()
                && stageBefore != MetaWearStage.ARCHITECTURE_BREAK
                && canGrow(data, ability.id(), RnaStat.OVERLOAD_RESISTANCE, gameTime)) {
            int grown = RnaApi.addStatGrowth(player, RnaStat.OVERLOAD_RESISTANCE, 1, source);
            if (grown > 0) {
                setGrowthCooldown(data, ability.id(), RnaStat.OVERLOAD_RESISTANCE, gameTime + 600);
            }
        }
    }

    private static boolean canGrow(
            RnaAbilityData data,
            ResourceLocation abilityId,
            RnaStat stat,
            long gameTime
    ) {
        return data.growthCooldownUntil(
                abilityId.toString(),
                stat
        ) <= gameTime;
    }

    private static void setGrowthCooldown(
            RnaAbilityData data,
            ResourceLocation abilityId,
            RnaStat stat,
            long tick
    ) {
        data.setGrowthCooldownUntil(
                abilityId.toString(),
                stat,
                tick
        );
    }

    private static boolean migrateLegacyTelekinesis(ServerPlayer player, RnaAbilityData data) {
        if (!player.getPersistentData().getBoolean(LEGACY_TELEKINESIS_TAG)) {
            return false;
        }
        player.getPersistentData().remove(LEGACY_TELEKINESIS_TAG);
        return data.unlock(RnaAbilityRegistry.TELEKINESIS_ID.toString());
    }

    private static void save(ServerPlayer player, RnaAbilityData data) {
        RiftbornePlayerData.saveRnaCombat(player, data);
    }

    public static String describeResult(RnaAbilityResult result) {
        return result.name().toLowerCase(Locale.ROOT);
    }
}
