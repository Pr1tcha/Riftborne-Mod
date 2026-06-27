package com.pr1tcha.riftborne.rna.combat;

import com.pr1tcha.riftborne.player.RiftbornePlayerData;
import com.pr1tcha.riftborne.config.Config;
import com.pr1tcha.riftborne.rna.RnaApi;
import com.pr1tcha.riftborne.rna.combat.ability.RnaAbility;
import com.pr1tcha.riftborne.rna.combat.cooldown.RnaCooldown;
import com.pr1tcha.riftborne.rna.combat.data.RnaAffinityTag;
import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityCost;
import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityData;
import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityResult;
import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityUseContext;
import com.pr1tcha.riftborne.rna.combat.data.RnaLoadBand;
import com.pr1tcha.riftborne.rna.combat.registry.RnaAbilityRegistry;
import com.pr1tcha.riftborne.rna.combat.scaling.RnaStageModifiers;
import com.pr1tcha.riftborne.rna.data.MetaWearStage;
import com.pr1tcha.riftborne.rna.data.RnaData;
import com.pr1tcha.riftborne.rna.data.RnaStat;
import java.util.Locale;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class RnaAbilityManager {
    public static final String LEGACY_TELEKINESIS_TAG = "RiftborneTelekinesis";

    private RnaAbilityManager() {
    }

    public static RnaAbilityData getData(ServerPlayer player) {
        RnaAbilityData data = RiftbornePlayerData.getRnaCombat(player);
        boolean changed = ensureBasicCombatSkills(player, data);
        if (migrateLegacyTelekinesis(player, data)) {
            changed = true;
        }
        if (changed) {
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
        if (ability.isBasicCombatSkill()
                && !RnaAbilityRegistry.RNA_OVERLOAD_VENT_ID.equals(abilityId)
                && data.currentLoad() >= Config.basicRnaOverloadLockThreshold.get()) {
            return RnaAbilityResult.FAIL_OVERLOAD;
        }
        RnaStageModifiers modifiers = RnaStageModifiers.forStage(rna.metaWearStage());
        return ability.requirement().check(rna, context, modifiers, ability.heavy());
    }

    public static RnaAbilityResult activateBasicSkill(
            ServerPlayer player,
            ResourceLocation abilityId,
            RnaAbilityUseContext context
    ) {
        RnaAbility ability = RnaAbilityRegistry.get(abilityId);
        if (ability == null) {
            return RnaAbilityResult.FAIL_UNKNOWN_ABILITY;
        }
        if (!ability.isBasicCombatSkill()) {
            return RnaAbilityResult.FAIL_CONTEXT;
        }

        RnaAbilityResult check = checkUse(player, abilityId, context);
        if (check != RnaAbilityResult.SUCCESS) {
            return check;
        }
        if (!context.meaningful()) {
            return RnaAbilityResult.FAIL_CONTEXT;
        }

        long gameTime = player.serverLevel().getGameTime();
        RnaAbilityData data = getData(player);
        RnaData rna = RnaApi.get(player);
        RnaStageModifiers stageModifiers = RnaStageModifiers.forStage(rna.metaWearStage());
        RnaLoadBand band = loadBand(data);

        data.setLastUseTick(abilityId.toString(), gameTime);
        data.setLastCombatAction(context.action());
        if (ability.durationTicks() > 0) {
            data.setActiveStateUntil(abilityId.toString(), gameTime + ability.durationTicks());
        }

        if (RnaAbilityRegistry.RNA_OVERLOAD_VENT_ID.equals(abilityId)) {
            data.addLoad(-35.0F);
            data.setInstabilityUntilTick(gameTime + 80L);
        } else {
            data.addLoad(ability.baseLoad() * band.cooldownMultiplier());
        }
        data.setLastLoadTick(gameTime);

        for (RnaAffinityTag affinity : ability.affinityTags()) {
            data.addAffinity(affinity, 1);
        }

        int cooldown = Mth.ceil(ability.cooldownTicks()
                * stageModifiers.cooldownMultiplier()
                * band.cooldownMultiplier());
        if (cooldown > 0) {
            data.setCooldownUntil(abilityId.toString(), gameTime + cooldown);
        }

        applyBasicSkillEffect(player, ability);
        save(player, data);
        RnaCombatNetwork.sendSync(player, abilityId, RnaAbilityResult.SUCCESS);
        return RnaAbilityResult.SUCCESS;
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

    public static void tick(ServerPlayer player) {
        RnaAbilityData data = getData(player);
        long gameTime = player.serverLevel().getGameTime();
        boolean changed = false;

        float beforeLoad = data.currentLoad();
        if (beforeLoad > 0.0F && gameTime > data.lastLoadTick()) {
            float ticksPassed = gameTime - data.lastLoadTick();
            float decay = (float) (Config.basicRnaLoadDecayPerSecond.get() / 20.0D) * ticksPassed;
            data.setCurrentLoad(beforeLoad - decay);
            data.setLastLoadTick(gameTime);
            changed = data.currentLoad() != beforeLoad;
        }

        int activeBefore = data.activeAbilities().size();
        data.expireActiveStates(gameTime);
        changed |= activeBefore != data.activeAbilities().size();

        if (changed) {
            save(player, data);
            if (gameTime % 5L == 0L) {
                RnaCombatNetwork.sendSync(player, null, null);
            }
        }
    }

    public static void setCurrentLoad(ServerPlayer player, float load) {
        RnaAbilityData data = getData(player);
        data.setCurrentLoad(load);
        data.setLastLoadTick(player.serverLevel().getGameTime());
        save(player, data);
    }

    public static RnaLoadBand loadBand(ServerPlayer player) {
        return loadBand(getData(player));
    }

    public static RnaLoadBand loadBand(RnaAbilityData data) {
        return RnaLoadBand.fromLoad(
                data.currentLoad(),
                Config.basicRnaOverloadWarningThreshold.get(),
                Config.basicRnaOverloadLockThreshold.get()
        );
    }

    public static void clearCombatDebugState(ServerPlayer player) {
        RnaAbilityData data = getData(player);
        data.clearCombatState();
        save(player, data);
    }

    public static boolean isActive(ServerPlayer player, ResourceLocation abilityId, long gameTime) {
        return getData(player).activeStateUntil(abilityId.toString()) > gameTime;
    }

    public static void clearActive(ServerPlayer player, ResourceLocation abilityId) {
        RnaAbilityData data = getData(player);
        data.setActiveStateUntil(abilityId.toString(), 0L);
        save(player, data);
        RnaCombatNetwork.sendSync(player, abilityId, null);
    }

    public static void addCombatLoad(ServerPlayer player, float amount) {
        RnaAbilityData data = getData(player);
        data.addLoad(amount);
        data.setLastLoadTick(player.serverLevel().getGameTime());
        save(player, data);
        RnaCombatNetwork.sendSync(player, null, null);
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

    private static boolean ensureBasicCombatSkills(ServerPlayer player, RnaAbilityData data) {
        if (!RnaApi.hasActiveRna(player)) {
            return false;
        }
        boolean changed = false;
        for (ResourceLocation abilityId : RnaAbilityRegistry.basicCombatIds()) {
            changed |= data.unlock(abilityId.toString());
        }
        return changed;
    }

    private static void applyBasicSkillEffect(ServerPlayer player, RnaAbility ability) {
        ResourceLocation id = ability.id();
        if (RnaAbilityRegistry.RNA_FOCUS_ID.equals(id)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, false, true));
            particles(player, 12, 0.35D, 0.35D, 0.35D, 0.02D);
        } else if (RnaAbilityRegistry.RNA_GUARD_ID.equals(id)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 50, 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 45, 0, false, false));
            particles(player, 10, 0.45D, 0.25D, 0.45D, 0.01D);
        } else if (RnaAbilityRegistry.RNA_STRIKE_ID.equals(id)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 70, 0, false, true));
            particles(player, 8, 0.3D, 0.25D, 0.3D, 0.02D);
        } else if (RnaAbilityRegistry.RNA_IMPULSE_STEP_ID.equals(id)) {
            impulseStep(player);
        } else if (RnaAbilityRegistry.RNA_ANCHOR_HOLD_ID.equals(id)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 70, 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 1, false, false));
            particles(player, 14, 0.2D, 0.08D, 0.2D, 0.0D);
        } else if (RnaAbilityRegistry.RNA_RECOVERY_ID.equals(id)) {
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 0, false, true));
            particles(player, 10, 0.35D, 0.25D, 0.35D, 0.03D);
        } else if (RnaAbilityRegistry.RNA_PULSE_PUSH_ID.equals(id)) {
            pulsePush(player);
        } else if (RnaAbilityRegistry.RNA_DEFLECT_ID.equals(id)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 14, 1, false, true));
            particles(player, 8, 0.45D, 0.25D, 0.45D, 0.04D);
        } else if (RnaAbilityRegistry.RNA_REACTION_SPIKE_ID.equals(id)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 90, 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 90, 0, false, true));
            particles(player, 16, 0.45D, 0.35D, 0.45D, 0.04D);
        } else if (RnaAbilityRegistry.RNA_OVERLOAD_VENT_ID.equals(id)) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, false));
            particles(player, 24, 0.55D, 0.45D, 0.55D, 0.03D);
        }
    }

    private static void impulseStep(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        if (flat.lengthSqr() < 0.001D) {
            flat = Vec3.directionFromRotation(0.0F, player.getYRot());
        }
        Vec3 impulse = flat.normalize().scale(1.1D).add(0.0D, player.onGround() ? 0.08D : 0.0D, 0.0D);
        player.setDeltaMovement(player.getDeltaMovement().add(impulse));
        player.hasImpulse = true;
        player.hurtMarked = true;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 4, 0, false, false));
        particles(player, 18, 0.3D, 0.15D, 0.3D, 0.08D);
    }

    private static void pulsePush(ServerPlayer player) {
        Vec3 origin = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        Vec3 look = player.getLookAngle().normalize();
        AABB area = player.getBoundingBox().inflate(4.0D, 2.0D, 4.0D);
        for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class, area, target -> target != player && target.isAlive())) {
            Vec3 offset = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).subtract(origin);
            double distance = Math.max(0.5D, offset.length());
            Vec3 direction = offset.normalize();
            if (look.dot(direction) < 0.25D) {
                continue;
            }
            double strength = Mth.clamp(1.15D - distance * 0.12D, 0.25D, 0.9D);
            target.setDeltaMovement(target.getDeltaMovement().add(direction.scale(strength).add(0.0D, 0.12D, 0.0D)));
            target.hasImpulse = true;
            target.hurtMarked = true;
            target.hurt(player.damageSources().playerAttack(player), 1.0F);
        }
        particles(player, 28, 1.0D, 0.35D, 1.0D, 0.08D);
    }

    private static void particles(ServerPlayer player, int count, double dx, double dy, double dz, double speed) {
        player.serverLevel().sendParticles(
                ParticleTypes.END_ROD,
                player.getX(),
                player.getY() + player.getBbHeight() * 0.55D,
                player.getZ(),
                count,
                dx,
                dy,
                dz,
                speed
        );
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
