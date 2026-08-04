package com.pr1tcha.riftborne.rna.power;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.config.Config;
import com.pr1tcha.riftborne.rna.power.data.AdaptationCycle;
import com.pr1tcha.riftborne.rna.power.data.PhysicalProfile;
import com.pr1tcha.riftborne.rna.power.data.PhysicalStat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Turns ordinary play into adaptation activity.
 *
 * <p>Every source here is deliberately hard to fake. Movement only counts when the player's own body
 * does the work, so vehicles, teleports and running on the spot are worth nothing. Mining is weighed
 * by block hardness rather than block count. Damage only trains Resilience when something external
 * actually hit the player, which rules out self-harm and sitting in a cactus. Recovery cannot be
 * bought with food or sleep at all — it needs a real exertion episode that rises and then subsides.
 */
@EventBusSubscriber(modid = Riftborne.MODID)
public final class AdaptationEvents {
    private static final String MELEE_SUBKEY = "strength_melee";
    private static final int SAMPLE_INTERVAL = 5;
    private static final double MAX_SAMPLE_DISTANCE = 3.0D;

    /** Physical load gained per unit of exertion, and how fast it bleeds off. */
    private static final double LOAD_PER_SPRINT_BLOCK = 0.05D;
    private static final double LOAD_PER_HARDNESS = 0.12D;
    private static final double LOAD_PER_DAMAGE = 0.9D;
    private static final double LOAD_DECAY_BASE = 0.035D;
    private static final double RECOVERY_SETTLED_LOAD = 20.0D;

    /** Guards against the same source being farmed in a tight loop. */
    private static final long DAMAGE_SOURCE_COOLDOWN_TICKS = 60L;

    private static final Map<UUID, Runtime> RUNTIME = new HashMap<>();

    private AdaptationEvents() {
    }

    private static final class Runtime {
        private Vec3 lastPosition;
        private boolean loadEpisodeArmed;
        private final Map<String, Long> lastDamageTick = new HashMap<>();
    }

    private static Runtime runtime(ServerPlayer player) {
        return RUNTIME.computeIfAbsent(player.getUUID(), ignored -> {
            Runtime created = new Runtime();
            created.lastPosition = player.position();
            return created;
        });
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!canAct(player)) {
            return;
        }

        AdaptationService.tickActive(player);
        Runtime runtime = runtime(player);

        if (player.tickCount % SAMPLE_INTERVAL == 0) {
            trackMovement(player, runtime);
            runtime.lastPosition = player.position();
        }
        decayLoad(player, runtime);
    }

    /** Sleeping, spectating or riding something means the body is not the one doing the work. */
    private static boolean canAct(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.isSleeping();
    }

    private static void trackMovement(ServerPlayer player, Runtime runtime) {
        if (player.isPassenger()) {
            return;
        }
        Vec3 current = player.position();
        Vec3 delta = current.subtract(runtime.lastPosition);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        // A jump beyond the sample window is a teleport or an external shove, not travel.
        if (horizontal <= 0.01D || horizontal > MAX_SAMPLE_DISTANCE) {
            return;
        }

        if (player.isSwimming()) {
            double norm = Config.adaptationSwimBlocksFull.get();
            AdaptationService.addActivity(player, PhysicalStat.ENDURANCE,
                    horizontal / norm * AdaptationCycle.MAX_ACTIVITY);
            addLoad(player, horizontal * LOAD_PER_SPRINT_BLOCK);
        } else if (player.isSprinting() && player.onGround()) {
            double norm = Config.adaptationSprintBlocksFull.get();
            AdaptationService.addActivity(player, PhysicalStat.ENDURANCE,
                    horizontal / norm * AdaptationCycle.MAX_ACTIVITY);
            addLoad(player, horizontal * LOAD_PER_SPRINT_BLOCK);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.isCreative()) {
            return;
        }
        float hardness = event.getState().getDestroySpeed(player.level(), event.getPos());
        // Instantly-broken foliage and the like should be worth essentially nothing.
        if (hardness <= 0.1F) {
            return;
        }
        double norm = Config.adaptationMiningHardnessFull.get();
        AdaptationService.addActivity(player, PhysicalStat.STRENGTH,
                hardness / norm * AdaptationCycle.MAX_ACTIVITY);
        addLoad(player, hardness * LOAD_PER_HARDNESS);
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative()) {
            return;
        }
        if (!event.getTarget().isAlive() || event.getTarget() == player) {
            return;
        }
        // Melee may only ever fill part of the Strength norm, so a punching bag is not a shortcut.
        double cap = Config.adaptationMeleeShareCap.get() * AdaptationCycle.MAX_ACTIVITY;
        AdaptationCycle cycle = AdaptationService.cycle(player);
        if (cycle.raw(MELEE_SUBKEY) >= cap) {
            return;
        }
        double share = AdaptationCycle.MAX_ACTIVITY / 120.0D;
        AdaptationService.setCycle(player, cycle.addRaw(MELEE_SUBKEY, share));
        AdaptationService.addActivity(player, PhysicalStat.STRENGTH, share);
        addLoad(player, 0.4D);
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DamageSource source = event.getSource();
        if (isSelfInflicted(player, source) || !isExternalImpact(source)) {
            return;
        }
        String key = source.getMsgId();
        Runtime runtime = runtime(player);
        long now = player.serverLevel().getGameTime();
        Long last = runtime.lastDamageTick.get(key);
        // The same hazard repeating in a tight loop is a trap being farmed, not adaptation.
        if (last != null && now - last < DAMAGE_SOURCE_COOLDOWN_TICKS) {
            return;
        }
        runtime.lastDamageTick.put(key, now);

        double amount = event.getNewDamage();
        AdaptationService.addActivity(player, PhysicalStat.RESILIENCE, Math.min(6.0D, amount * 0.6D));
        addLoad(player, amount * LOAD_PER_DAMAGE);
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        // A controlled landing from a real height, not a fall that hurts.
        if (event.getDistance() < 4.0F || event.getDistance() > 12.0F) {
            return;
        }
        AdaptationService.addActivity(player, PhysicalStat.RESILIENCE, 1.5D);
        addLoad(player, event.getDistance() * 0.3D);
    }

    private static boolean isSelfInflicted(ServerPlayer player, DamageSource source) {
        return source.getEntity() == player || source.getDirectEntity() == player;
    }

    /** Only real impacts count: something struck the player or drove them into something. */
    private static boolean isExternalImpact(DamageSource source) {
        return source.getEntity() != null || source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION);
    }

    private static void addLoad(ServerPlayer player, double amount) {
        if (amount <= 0.0D) {
            return;
        }
        PhysicalProfile profile = AdaptationService.physical(player);
        double load = Mth.clamp(profile.physicalLoad() + amount, 0.0D, PhysicalProfile.MAX_VALUE);
        AdaptationService.setPhysical(player, profile.withPhysicalLoad(load));
        if (load >= Config.adaptationRecoveryLoadThreshold.get()) {
            runtime(player).loadEpisodeArmed = true;
        }
    }

    /**
     * Load bleeds off continuously, faster for a better-recovered body. Completing the fall from a
     * real exertion peak back down to a settled level is what actually trains Recovery — and only a
     * limited number of times per cycle.
     */
    private static void decayLoad(ServerPlayer player, Runtime runtime) {
        PhysicalProfile profile = AdaptationService.physical(player);
        if (profile.physicalLoad() <= 0.0D) {
            return;
        }
        double rate = LOAD_DECAY_BASE * (0.6D + profile.recovery() / 100.0D);
        double load = Math.max(0.0D, profile.physicalLoad() - rate);
        AdaptationService.setPhysical(player, profile.withPhysicalLoad(load));

        if (!runtime.loadEpisodeArmed || load > RECOVERY_SETTLED_LOAD) {
            return;
        }
        runtime.loadEpisodeArmed = false;
        AdaptationCycle cycle = AdaptationService.cycle(player);
        if (cycle.recoveryEpisodes() >= Config.adaptationRecoveryEpisodeLimit.get()) {
            return;
        }
        AdaptationService.setCycle(player, cycle.withRecoveryEpisodes(cycle.recoveryEpisodes() + 1));
        AdaptationService.addActivity(player, PhysicalStat.RECOVERY,
                AdaptationCycle.MAX_ACTIVITY / Math.max(1, Config.adaptationRecoveryEpisodeLimit.get()));
    }
}
