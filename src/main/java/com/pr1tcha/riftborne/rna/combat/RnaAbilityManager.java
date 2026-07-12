package com.pr1tcha.riftborne.rna.combat;

import com.pr1tcha.riftborne.player.RiftbornePlayerData;
import com.pr1tcha.riftborne.physical.PhysicalTrainingManager;
import com.pr1tcha.riftborne.codex.data.CodexData;
import com.pr1tcha.riftborne.config.Config;
import com.pr1tcha.riftborne.rna.RnaApi;
import com.pr1tcha.riftborne.rna.combat.ability.RnaAbility;
import com.pr1tcha.riftborne.rna.combat.barrier.BarrierGestureMode;
import com.pr1tcha.riftborne.rna.combat.barrier.BarrierPhase;
import com.pr1tcha.riftborne.rna.combat.cooldown.RnaCooldown;
import com.pr1tcha.riftborne.rna.combat.data.RnaAffinityTag;
import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityCost;
import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityData;
import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityResult;
import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityUseContext;
import com.pr1tcha.riftborne.rna.combat.data.RnaLoadBand;
import com.pr1tcha.riftborne.rna.combat.registry.RnaAbilityRegistry;
import com.pr1tcha.riftborne.rna.combat.progression.RnaTechniqueDefinition;
import com.pr1tcha.riftborne.rna.combat.progression.RnaTechniqueRegistry;
import com.pr1tcha.riftborne.rna.combat.progression.RnaTechniqueStage;
import com.pr1tcha.riftborne.rna.combat.progression.RnaTechniqueEvidence;
import com.pr1tcha.riftborne.rna.combat.progression.RnaTechniqueProgression;
import com.pr1tcha.riftborne.rna.combat.progression.RnaAcquisitionMethod;
import com.pr1tcha.riftborne.rna.combat.progression.RnaTechniqueProgress;
import com.pr1tcha.riftborne.rna.combat.scaling.RnaStageModifiers;
import com.pr1tcha.riftborne.rna.data.MetaWearStage;
import com.pr1tcha.riftborne.rna.data.RnaData;
import com.pr1tcha.riftborne.rna.data.RnaStat;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class RnaAbilityManager {
    public static final String LEGACY_TELEKINESIS_TAG = "RiftborneTelekinesis";
    public static final int BARRIER_DEPLOY_TICKS = 5;
    private static final Map<UUID, BarrierDeployment> BARRIER_DEPLOYMENTS = new HashMap<>();

    private RnaAbilityManager() {
    }

    public static RnaAbilityData getData(ServerPlayer player) {
        RnaAbilityData data = RiftbornePlayerData.getRnaCombat(player);
        boolean changed = migratePrototypeCombatUnlocks(data);
        if (migrateLegacyTelekinesis(player, data)) {
            changed = true;
        }
        if (refreshTechniqueProgression(player, data)) {
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
        boolean changed = RnaTechniqueRegistry.get(abilityId) == null
                ? data.unlock(abilityId.toString())
                : data.stabilizeTechnique(abilityId.toString(), player.serverLevel().getGameTime());
        save(player, data);
        return changed;
    }

    public static boolean discoverTechnique(ServerPlayer player, ResourceLocation abilityId) {
        if (RnaTechniqueRegistry.get(abilityId) == null) {
            return false;
        }
        RnaAbilityData data = getData(player);
        boolean changed = data.discoverTechnique(abilityId.toString(), player.serverLevel().getGameTime());
        if (changed) {
            save(player, data);
        }
        return changed;
    }

    public static boolean setTechniqueStage(
            ServerPlayer player,
            ResourceLocation techniqueId,
            RnaTechniqueStage stage
    ) {
        if (RnaTechniqueRegistry.get(techniqueId) == null) {
            return false;
        }
        RnaAbilityData data = getData(player);
        data.setTechniqueStage(techniqueId.toString(), stage, player.serverLevel().getGameTime());
        save(player, data);
        return true;
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
        RnaTechniqueDefinition technique = RnaTechniqueRegistry.get(abilityId);
        boolean provisionalAccess = technique != null
                && data.hasProvisionalTechnique(abilityId.toString())
                && RnaTechniqueProgression.evaluate(rna, data, technique).structuralReady();
        if (!data.isUnlocked(abilityId.toString()) && !provisionalAccess) {
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

        if (RnaAbilityRegistry.BARRIER_ID.equals(abilityId)) {
            if (BARRIER_DEPLOYMENTS.containsKey(player.getUUID())) {
                return RnaAbilityResult.FAIL_CONTEXT;
            }
            beginBarrierDeployment(player, context);
            return RnaAbilityResult.SUCCESS;
        }

        return completeBasicSkillActivation(player, ability, abilityId, context);
    }

    private static RnaAbilityResult completeBasicSkillActivation(
            ServerPlayer player,
            RnaAbility ability,
            ResourceLocation abilityId,
            RnaAbilityUseContext context
    ) {

        long gameTime = player.serverLevel().getGameTime();
        RnaAbilityData data = getData(player);
        RnaData rna = RnaApi.get(player);
        RnaStageModifiers stageModifiers = RnaStageModifiers.forStage(rna.metaWearStage());
        RnaLoadBand band = loadBand(data);
        boolean provisional = !data.isUnlocked(abilityId.toString());

        data.setLastUseTick(abilityId.toString(), gameTime);
        data.setLastCombatAction(context.action());
        if (ability.durationTicks() > 0) {
            int duration = provisional
                    ? Math.max(1, Mth.ceil(ability.durationTicks() * 0.67F))
                    : ability.durationTicks();
            data.setActiveStateUntil(abilityId.toString(), gameTime + duration);
        }

        if (RnaAbilityRegistry.RNA_OVERLOAD_VENT_ID.equals(abilityId)) {
            applyPhysicalLoadDelta(player, data, -35.0F);
            data.setInstabilityUntilTick(gameTime + 80L);
        } else {
            applyPhysicalLoadDelta(
                    player,
                    data,
                    ability.baseLoad() * band.cooldownMultiplier() * (provisional ? 1.35F : 1.0F)
            );
        }
        data.setLastLoadTick(gameTime);

        if (band == RnaLoadBand.OVERHEAT) {
            data.addProgressionEvidence(RnaTechniqueEvidence.HIGH_LOAD_TECHNIQUE_USE, 1);
            addPatternProgressInternal(player, data, abilityId, RnaAcquisitionMethod.FIELD_ADAPTATION, 1);
        }

        if (RnaAbilityRegistry.BARRIER_ID.equals(abilityId)) {
            float integrity = calculateBarrierIntegrity(player, rna, data);
            data.setActiveStrength(abilityId.toString(), provisional ? integrity * 0.7F : integrity);
        }

        for (RnaAffinityTag affinity : ability.affinityTags()) {
            data.addAffinity(affinity, 1);
        }
        int cooldown = Mth.ceil(ability.cooldownTicks()
                * stageModifiers.cooldownMultiplier()
                * band.cooldownMultiplier()
                * (provisional ? 1.25F : 1.0F));
        if (cooldown > 0) {
            data.setCooldownUntil(abilityId.toString(), gameTime + cooldown);
        }

        applyBasicSkillEffect(player, ability);
        save(player, data);
        if (RnaAbilityRegistry.BARRIER_ID.equals(abilityId)) {
            RnaCombatNetwork.broadcastBarrierState(
                    player,
                    data.activeStrength(RnaAbilityRegistry.BARRIER_ID.toString())
            );
        }
        RnaCombatNetwork.sendSync(player, abilityId, RnaAbilityResult.SUCCESS);
        return RnaAbilityResult.SUCCESS;
    }

    private static void beginBarrierDeployment(ServerPlayer player, RnaAbilityUseContext context) {
        long gameTime = player.serverLevel().getGameTime();
        player.stopUsingItem();
        BARRIER_DEPLOYMENTS.put(
                player.getUUID(),
                new BarrierDeployment(gameTime + BARRIER_DEPLOY_TICKS, context.action(), context.source())
        );
        RnaCombatNetwork.broadcastBarrierDeployment(player, BARRIER_DEPLOY_TICKS);
        RnaCombatNetwork.sendSync(player, RnaAbilityRegistry.BARRIER_ID, RnaAbilityResult.SUCCESS);
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

        finishBarrierDeployment(player, gameTime);

        float beforeLoad = data.currentLoad();
        if (beforeLoad > 0.0F && gameTime > data.lastLoadTick()) {
            float ticksPassed = gameTime - data.lastLoadTick();
            float decay = (float) (Config.basicRnaLoadDecayPerSecond.get() / 20.0D) * ticksPassed;
            applyPhysicalLoadDelta(player, data, -decay);
            data.setLastLoadTick(gameTime);
            changed = data.currentLoad() != beforeLoad;
        }

        int activeBefore = data.activeAbilities().size();
        float barrierBefore = data.activeStrength(RnaAbilityRegistry.BARRIER_ID.toString());
        data.expireActiveStates(gameTime);
        boolean activeChanged = activeBefore != data.activeAbilities().size();
        changed |= activeChanged;

        if (changed) {
            save(player, data);
            if (barrierBefore > 0.0F
                    && data.activeStrength(RnaAbilityRegistry.BARRIER_ID.toString()) <= 0.0F) {
                RnaCombatNetwork.broadcastBarrierState(player, 0.0F);
            }
            if (activeChanged || gameTime % 5L == 0L) {
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
        BARRIER_DEPLOYMENTS.remove(player.getUUID());
        RnaAbilityData data = getData(player);
        data.clearCombatState();
        save(player, data);
        RnaCombatNetwork.broadcastBarrierState(player, 0.0F);
    }

    public static boolean isActive(ServerPlayer player, ResourceLocation abilityId, long gameTime) {
        return getData(player).activeStateUntil(abilityId.toString()) > gameTime;
    }

    public static void clearActive(ServerPlayer player, ResourceLocation abilityId) {
        if (RnaAbilityRegistry.BARRIER_ID.equals(abilityId)) {
            BARRIER_DEPLOYMENTS.remove(player.getUUID());
        }
        RnaAbilityData data = getData(player);
        data.setActiveStateUntil(abilityId.toString(), 0L);
        save(player, data);
        if (RnaAbilityRegistry.BARRIER_ID.equals(abilityId)) {
            RnaCombatNetwork.broadcastBarrierState(player, 0.0F);
        }
        RnaCombatNetwork.sendSync(player, abilityId, null);
    }

    public static float activeStrength(ServerPlayer player, ResourceLocation abilityId) {
        return getData(player).activeStrength(abilityId.toString());
    }

    public static BarrierPhase barrierPhase(ServerPlayer player) {
        if (BARRIER_DEPLOYMENTS.containsKey(player.getUUID())) {
            return BarrierPhase.DEPLOYING;
        }
        return isActive(player, RnaAbilityRegistry.BARRIER_ID, player.serverLevel().getGameTime())
                ? BarrierPhase.ACTIVE
                : BarrierPhase.INACTIVE;
    }

    public static int barrierPhaseTicks(ServerPlayer player) {
        BarrierDeployment deployment = BARRIER_DEPLOYMENTS.get(player.getUUID());
        if (deployment == null) {
            return 0;
        }
        return (int) Math.max(0L, deployment.completeAtTick() - player.serverLevel().getGameTime());
    }

    public static BarrierGestureMode barrierGestureMode(ServerPlayer player) {
        RnaTechniqueProgress progress = getData(player).techniqueProgress(RnaAbilityRegistry.BARRIER_ID.toString());
        return progress != null && progress.stage() == RnaTechniqueStage.MASTERED
                ? BarrierGestureMode.LEFT_HANDED
                : BarrierGestureMode.TWO_HANDED;
    }

    public static boolean isBarrierHandOccupied(ServerPlayer player, InteractionHand hand) {
        if (barrierPhase(player) == BarrierPhase.INACTIVE) {
            return false;
        }
        HumanoidArm physicalArm = hand == InteractionHand.MAIN_HAND
                ? player.getMainArm()
                : player.getMainArm().getOpposite();
        return barrierGestureMode(player).occupies(physicalArm);
    }

    public static void cancelBarrierDeployment(ServerPlayer player) {
        if (BARRIER_DEPLOYMENTS.remove(player.getUUID()) != null) {
            RnaCombatNetwork.broadcastBarrierState(player, 0.0F);
        }
    }

    private static void finishBarrierDeployment(ServerPlayer player, long gameTime) {
        BarrierDeployment deployment = BARRIER_DEPLOYMENTS.get(player.getUUID());
        if (deployment == null || gameTime < deployment.completeAtTick()) {
            return;
        }
        BARRIER_DEPLOYMENTS.remove(player.getUUID());
        if (!player.isAlive()) {
            RnaCombatNetwork.broadcastBarrierState(player, 0.0F);
            return;
        }

        RnaAbilityUseContext context = RnaAbilityUseContext.action(
                player,
                null,
                player.blockPosition(),
                deployment.action(),
                deployment.source()
        );
        RnaAbilityResult check = checkUse(player, RnaAbilityRegistry.BARRIER_ID, context);
        if (check != RnaAbilityResult.SUCCESS) {
            RnaCombatNetwork.broadcastBarrierState(player, 0.0F);
            RnaCombatNetwork.sendSync(player, RnaAbilityRegistry.BARRIER_ID, check);
            return;
        }
        completeBasicSkillActivation(
                player,
                RnaAbilityRegistry.get(RnaAbilityRegistry.BARRIER_ID),
                RnaAbilityRegistry.BARRIER_ID,
                context
        );
    }

    public static BarrierImpact absorbBarrierDamage(
            ServerPlayer player,
            float incomingDamage,
            boolean projectileImpact
    ) {
        RnaAbilityData data = getData(player);
        float integrity = data.activeStrength(RnaAbilityRegistry.BARRIER_ID.toString());
        float incoming = Math.max(0.0F, incomingDamage);
        float absorbed = Math.min(integrity, incoming);
        float overflow = Math.max(0.0F, incoming - absorbed);
        float remaining = Math.max(0.0F, integrity - absorbed);

        if (absorbed > 0.0F) {
            data.addProgressionEvidence(RnaTechniqueEvidence.DAMAGE_ABSORBED, Mth.ceil(absorbed));
            if (projectileImpact) {
                data.addProgressionEvidence(RnaTechniqueEvidence.PROJECTILE_INTERCEPTED, 1);
            }
            recordMeaningfulTechniqueUse(
                    data,
                    RnaAbilityRegistry.BARRIER_ID,
                    player.serverLevel().getGameTime()
            );
            addPatternProgressInternal(
                    player,
                    data,
                    RnaAbilityRegistry.BARRIER_ID,
                    RnaAcquisitionMethod.FIELD_ADAPTATION,
                    Math.max(1, Math.min(4, Mth.ceil(absorbed / 4.0F)))
            );
        }

        RnaData rna = RnaApi.get(player);
        float transferLoss = Math.max(0.65F, 1.35F - rna.overloadResistance() * 0.006F);
        applyPhysicalLoadDelta(player, data, Mth.clamp(absorbed * transferLoss, 0.0F, 12.0F));
        data.setLastLoadTick(player.serverLevel().getGameTime());

        boolean overloadBreak = loadBand(data) == RnaLoadBand.BREAKDOWN;
        boolean broken = remaining <= 0.0F || overloadBreak;
        if (broken) {
            data.setActiveStateUntil(RnaAbilityRegistry.BARRIER_ID.toString(), 0L);
            remaining = 0.0F;
        } else {
            data.setActiveStrength(RnaAbilityRegistry.BARRIER_ID.toString(), remaining);
        }

        save(player, data);
        RnaCombatNetwork.sendSync(player, RnaAbilityRegistry.BARRIER_ID, null);
        RnaCombatNetwork.broadcastBarrierState(player, remaining);
        return new BarrierImpact(absorbed, overflow, remaining, broken, overloadBreak);
    }

    public static void addCombatLoad(ServerPlayer player, float amount) {
        RnaAbilityData data = getData(player);
        applyPhysicalLoadDelta(player, data, amount);
        data.setLastLoadTick(player.serverLevel().getGameTime());
        save(player, data);
        RnaCombatNetwork.sendSync(player, null, null);
    }

    public static void addProgressionEvidence(
            ServerPlayer player,
            RnaTechniqueEvidence evidence,
            int amount
    ) {
        if (!RnaApi.hasActiveRna(player) || evidence == null || amount <= 0) {
            return;
        }
        RnaAbilityData data = getData(player);
        data.addProgressionEvidence(evidence, amount);
        save(player, data);
    }

    public static int addTechniquePatternProgress(
            ServerPlayer player,
            ResourceLocation techniqueId,
            RnaAcquisitionMethod method,
            int amount
    ) {
        if (!RnaApi.hasActiveRna(player)
                || RnaTechniqueRegistry.get(techniqueId) == null
                || method == null
                || amount <= 0) {
            return 0;
        }
        RnaAbilityData data = getData(player);
        int accepted = addPatternProgressInternal(player, data, techniqueId, method, amount);
        if (accepted > 0) {
            save(player, data);
        }
        return accepted;
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

    private static float calculateBarrierIntegrity(
            ServerPlayer player,
            RnaData rna,
            RnaAbilityData combat
    ) {
        float structuralCapacity = 6.0F
                + rna.nodeDensity() * 0.12F
                + rna.connectivity() * 0.08F
                + rna.overloadResistance() * 0.05F;
        float healthRatio = player.getMaxHealth() <= 0.0F ? 0.0F : player.getHealth() / player.getMaxHealth();
        float physicalCondition = 0.5F + Mth.clamp(healthRatio, 0.0F, 1.0F) * 0.5F;
        float loadCondition = 1.0F - Mth.clamp(combat.currentLoad() / 100.0F, 0.0F, 1.0F) * 0.45F;
        float wearCondition = switch (rna.metaWearStage()) {
            case STABLE -> 1.0F;
            case STRAIN -> 0.9F;
            case DISTORTION -> 0.75F;
            case REJECTION -> 0.55F;
            case ARCHITECTURE_BREAK -> 0.35F;
        };
        return Math.max(1.0F, structuralCapacity * physicalCondition * loadCondition * wearCondition);
    }

    private static void applyPhysicalLoadDelta(ServerPlayer player, RnaAbilityData data, float rawDelta) {
        float capacityMultiplier = Math.max(0.1F, PhysicalTrainingManager.overloadCapacityMultiplier(player));
        data.addLoad(rawDelta / capacityMultiplier);
    }

    private static void recordMeaningfulTechniqueUse(
            RnaAbilityData data,
            ResourceLocation techniqueId,
            long gameTime
    ) {
        RnaTechniqueDefinition technique = RnaTechniqueRegistry.get(techniqueId);
        if (technique == null) {
            return;
        }
        data.recordTechniqueUse(techniqueId.toString(), gameTime, technique.masteryUses());
        technique.resonanceWeights().forEach(data::addAspectResonance);
    }

    private static int addPatternProgressInternal(
            ServerPlayer player,
            RnaAbilityData data,
            ResourceLocation techniqueId,
            RnaAcquisitionMethod method,
            int amount
    ) {
        int adjusted = Mth.ceil(amount * acquisitionMultiplier(RnaApi.get(player), method));
        int accepted = data.addTechniquePatternProgress(techniqueId.toString(), method, adjusted);
        if (accepted > 0) {
            refreshTechniqueProgression(player, data);
        }
        return accepted;
    }

    private static float acquisitionMultiplier(RnaData rna, RnaAcquisitionMethod method) {
        return switch (method) {
            case TRAINING -> rna.formationPath() == com.pr1tcha.riftborne.rna.data.FormationPath.TRAINING
                    ? 1.15F : 1.0F;
            case FIELD_ADAPTATION -> rna.formationPath() == com.pr1tcha.riftborne.rna.data.FormationPath.STRESS
                    ? 1.15F : 1.0F;
            case ARTIFICIAL_IMPRINT -> rna.formationPath()
                    == com.pr1tcha.riftborne.rna.data.FormationPath.ARTIFICIAL_BORN ? 1.15F : 1.0F;
            case ANOMALOUS_RESEARCH, UNTRACKED -> 1.0F;
        };
    }

    private static boolean refreshTechniqueProgression(ServerPlayer player, RnaAbilityData data) {
        boolean changed = false;
        RnaData rna = RnaApi.get(player);
        for (RnaTechniqueDefinition technique : RnaTechniqueRegistry.all()) {
            RnaTechniqueProgress progress = data.techniqueProgress(technique.id().toString());
            if (progress == null) {
                continue;
            }
            if (progress.hasDiscoveryThreshold()
                    && progress.stage() == RnaTechniqueStage.SEALED
                    && RnaTechniqueProgression.evaluate(rna, data, technique).discoveryReady()
                    && data.discoverTechnique(technique.id().toString(), player.serverLevel().getGameTime())) {
                changed = true;
                CodexData codex = RiftbornePlayerData.getCodex(player);
                codex.addTranslatedRecentData(
                        "codex.riftborne.feed.technique_discovered",
                        CodexData.translationArgument(technique.titleKey())
                );
                RiftbornePlayerData.saveCodex(player, codex);
                player.displayClientMessage(
                        Component.translatable(
                                "message.riftborne.technique.discovered",
                                Component.translatable(technique.titleKey())
                        ),
                        false
                );
            }
            progress = data.techniqueProgress(technique.id().toString());
            if (progress != null
                    && progress.hasStabilizationThreshold()
                    && !progress.stage().usable()
                    && RnaTechniqueProgression.evaluate(rna, data, technique).calibrationReady()
                    && data.stabilizeTechnique(technique.id().toString(), player.serverLevel().getGameTime())) {
                changed = true;
                CodexData codex = RiftbornePlayerData.getCodex(player);
                codex.addTranslatedRecentData(
                        "codex.riftborne.feed.technique_stabilized",
                        CodexData.translationArgument(technique.titleKey())
                );
                RiftbornePlayerData.saveCodex(player, codex);
                player.displayClientMessage(
                        Component.translatable(
                                "message.riftborne.technique.stabilized",
                                Component.translatable(technique.titleKey())
                        ),
                        false
                );
            }
        }
        return changed;
    }

    public record BarrierImpact(
            float absorbedDamage,
            float overflowDamage,
            float remainingIntegrity,
            boolean broken,
            boolean overloadBreak
    ) {
    }

    private record BarrierDeployment(long completeAtTick, String action, String source) {
    }

    private static boolean migratePrototypeCombatUnlocks(RnaAbilityData data) {
        if (data.version() >= RnaAbilityData.CURRENT_VERSION) {
            return false;
        }

        if (data.version() < 2) {
            for (ResourceLocation abilityId : RnaAbilityRegistry.basicCombatIds()) {
                data.revoke(abilityId.toString());
            }
            data.revoke(RnaAbilityRegistry.LEGACY_RNA_GUARD_ID.toString());
        }
        data.migrateUnlockedTechniqueProgress(RnaTechniqueRegistry.ids());
        data.markCurrentVersion();
        return true;
    }

    private static void applyBasicSkillEffect(ServerPlayer player, RnaAbility ability) {
        ResourceLocation id = ability.id();
        if (RnaAbilityRegistry.RNA_FOCUS_ID.equals(id)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, false, true));
            particles(player, 12, 0.35D, 0.35D, 0.35D, 0.02D);
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
