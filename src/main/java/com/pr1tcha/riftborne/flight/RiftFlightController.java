package com.pr1tcha.riftborne.flight;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.config.Config;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Riftborne.MODID)
public final class RiftFlightController {
    private static final int GROUND_DISABLE_GRACE_TICKS = 5;

    public static final int INPUT_FORWARD = 1;
    public static final int INPUT_BACKWARD = 1 << 1;
    public static final int INPUT_LEFT = 1 << 2;
    public static final int INPUT_RIGHT = 1 << 3;
    public static final int INPUT_JUMP = 1 << 4;
    public static final int INPUT_CROUCH = 1 << 5;
    public static final int INPUT_SPRINT = 1 << 6;

    private static final int ALL_INPUTS = INPUT_FORWARD
            | INPUT_BACKWARD
            | INPUT_LEFT
            | INPUT_RIGHT
            | INPUT_JUMP
            | INPUT_CROUCH
            | INPUT_SPRINT;

    private static final Map<UUID, FlightState> ACTIVE_FLIGHTS = new HashMap<>();

    private RiftFlightController() {
    }

    public static void setActive(ServerPlayer player, boolean active) {
        UUID playerId = player.getUUID();
        if (!active) {
            stopFlight(player, true);
            return;
        }

        if (!FlightAbility.hasAbility(player) || !canStartFlight(player)) {
            FlightNetwork.sendState(player, false);
            return;
        }

        FlightState state = ACTIVE_FLIGHTS.computeIfAbsent(playerId, id -> new FlightState(player.isNoGravity()));
        state.originalNoGravity = player.isNoGravity();
        state.flightVector = player.getDeltaMovement();
        state.flightBoost = 0.0F;
        state.verticalHover = 0;
        state.inputFlags = 0;
        state.visualPhase = FlightNetwork.VISUAL_INACTIVE;
        state.ticksActive = 0;
        state.wasAirborne = !player.onGround();
        player.setNoGravity(true);
        player.fallDistance = 0.0F;
        FlightNetwork.sendState(player, true);
        FlightNetwork.sendVisualState(player, FlightNetwork.VISUAL_HOVER, 0.0F, state.flightVector.x, state.flightVector.y, state.flightVector.z);
    }

    public static void handleInput(ServerPlayer player, int flags) {
        FlightState state = ACTIVE_FLIGHTS.get(player.getUUID());
        if (state != null) {
            state.inputFlags = flags & ALL_INPUTS;
        }
    }

    public static boolean isActive(Player player) {
        return ACTIVE_FLIGHTS.containsKey(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            tickPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            stopFlight(player, false);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            stopFlight(player, true);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer originalPlayer) {
            stopFlight(originalPlayer, false);
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            FlightNetwork.sendState(player, false);
        }
    }

    private static void tickPlayer(ServerPlayer player) {
        FlightState state = ACTIVE_FLIGHTS.get(player.getUUID());
        if (state == null) {
            return;
        }

        if (!canKeepFlying(player)) {
            stopFlight(player, true);
            return;
        }

        state.ticksActive++;
        if (!player.onGround()) {
            state.wasAirborne = true;
        } else if (state.wasAirborne && state.ticksActive > GROUND_DISABLE_GRACE_TICKS) {
            stopFlight(player, true);
            return;
        }

        player.setNoGravity(true);
        player.getAbilities().flying = false;
        player.fallDistance = 0.0F;

        int input = state.inputFlags;
        boolean forward = has(input, INPUT_FORWARD);
        boolean backward = has(input, INPUT_BACKWARD);
        boolean sprinting = has(input, INPUT_SPRINT);
        FlightSettings settings = FlightAbility.getSettings(player);

        updateBoost(state, settings, forward, backward, sprinting);

        if (state.flightBoost > 0.01F) {
            applyForwardFlight(player, state, settings, input, forward, backward, sprinting);
        } else {
            applyHover(player, state, settings, input);
        }
        updateVisualState(player, state, settings);

        player.hasImpulse = true;
        player.hurtMarked = true;
    }

    private static void updateBoost(FlightState state, FlightSettings settings, boolean forward, boolean backward, boolean sprinting) {
        if (forward) {
            float slowBoostMax = Config.flightSlowBoostMax.get().floatValue();
            float maxBoost = sprinting ? Math.max(slowBoostMax, Config.flightFastBoostMax.get().floatValue()) : slowBoostMax;
            float acceleration = sprinting ? Config.flightFastAcceleration.get().floatValue() : Config.flightSlowAcceleration.get().floatValue();
            acceleration *= settings.accelerationMultiplier();
            state.flightBoost = Math.min(maxBoost, state.flightBoost + acceleration);
        } else {
            float deceleration = backward ? Config.flightBrakeDeceleration.get().floatValue() : Config.flightReleaseDeceleration.get().floatValue();
            deceleration *= settings.brakingMultiplier();
            state.flightBoost = Math.max(0.0F, state.flightBoost - deceleration);
        }

        float slowBoostMax = Config.flightSlowBoostMax.get().floatValue();
        if (!sprinting && state.flightBoost > slowBoostMax) {
            state.flightBoost = Math.max(slowBoostMax, (float) (state.flightBoost - Config.flightBoostSettleDeceleration.get() * settings.brakingMultiplier()));
        }
    }

    private static void applyForwardFlight(ServerPlayer player, FlightState state, FlightSettings settings, int input, boolean forward, boolean backward, boolean sprinting) {
        if (!forward) {
            double damping = backward ? Config.flightBrakeDamping.get() : Config.flightCoastDamping.get();
            damping = applyBrakingMultiplier(damping, settings.brakingMultiplier());
            state.flightVector = state.flightVector.scale(damping);
            double stopSnapSpeed = Config.flightStopSnapSpeed.get();
            if (state.flightVector.lengthSqr() < stopSnapSpeed * stopSnapSpeed) {
                state.flightVector = Vec3.ZERO;
            }
            state.verticalHover = 0;
            player.setDeltaMovement(state.flightVector);
            return;
        }

        Vec3 look = player.getLookAngle().normalize();
        Vec3 desiredDirection = look;

        if (has(input, INPUT_LEFT)) {
            desiredDirection = desiredDirection.add(horizontalSideVector(look, true).scale(Config.flightSideInfluence.get()));
        }
        if (has(input, INPUT_RIGHT)) {
            desiredDirection = desiredDirection.add(horizontalSideVector(look, false).scale(Config.flightSideInfluence.get()));
        }
        if (desiredDirection.lengthSqr() < 1.0E-4D) {
            desiredDirection = look;
        }

        double fastBoostMax = Math.max(Config.flightSlowBoostMax.get(), Config.flightFastBoostMax.get());
        double boost = Mth.clamp(state.flightBoost, 0.0D, fastBoostMax);
        double speed = Config.flightSlowSpeed.get() * Math.min(boost, 1.0D)
                + Config.flightBoostSpeedStep.get() * Math.max(0.0D, boost - 1.0D);
        speed = Math.min(speed * settings.speedMultiplier(), settings.maxSpeed());
        Vec3 desiredVelocity = desiredDirection.normalize().scale(speed);
        Vec3 diff = desiredVelocity.subtract(state.flightVector);
        double maxTurn = sprinting && state.flightBoost > Config.flightSlowBoostMax.get()
                ? Config.flightBoostSteering.get()
                : Config.flightNormalSteering.get();
        maxTurn *= settings.steeringMultiplier();

        if (diff.length() > maxTurn) {
            diff = diff.scale(maxTurn / diff.length());
        }

        state.flightVector = state.flightVector.add(diff);
        state.verticalHover = 0;
        player.setDeltaMovement(state.flightVector);
    }

    private static void applyHover(ServerPlayer player, FlightState state, FlightSettings settings, int input) {
        int verticalControlTicks = Config.flightVerticalControlTicks.get();
        boolean verticalInput = has(input, INPUT_JUMP) || has(input, INPUT_CROUCH);
        if (has(input, INPUT_JUMP)) {
            state.verticalHover = Math.min(verticalControlTicks, state.verticalHover + 1);
        } else if (has(input, INPUT_CROUCH)) {
            state.verticalHover = Math.max(-verticalControlTicks, state.verticalHover - 1);
        } else if (!settings.hoverEnabled()) {
            state.verticalHover = 0;
        } else if (state.verticalHover != 0) {
            state.verticalHover += state.verticalHover > 0 ? -1 : 1;
        }

        double horizontalDamping = Config.flightHoverHorizontalDamping.get();
        if (!settings.hoverEnabled()) {
            horizontalDamping = applyBrakingMultiplier(horizontalDamping, settings.brakingMultiplier());
        }
        Vec3 damped = state.flightVector.multiply(horizontalDamping, 1.0D, horizontalDamping);
        double stopSnapSpeed = Config.flightStopSnapSpeed.get();
        if (damped.horizontalDistanceSqr() < stopSnapSpeed * stopSnapSpeed) {
            damped = new Vec3(0.0D, damped.y, 0.0D);
        }

        double yMotion;
        if (!settings.hoverEnabled() && !verticalInput) {
            yMotion = -Config.flightNoHoverSinkSpeed.get();
        } else if (state.verticalHover == 0) {
            yMotion = Math.sin(player.tickCount / 10.0D) * Config.flightHoverBobAmplitude.get();
        } else {
            yMotion = (state.verticalHover / (double) verticalControlTicks)
                    * Config.flightVerticalSpeed.get()
                    * settings.verticalSpeedMultiplier();
        }
        double verticalBlend = verticalInput ? Config.flightHoverControlVerticalBlend.get() : Config.flightHoverVerticalBlend.get();
        double blendedY = damped.y + (yMotion - damped.y) * verticalBlend;
        state.flightVector = new Vec3(damped.x, blendedY, damped.z);
        player.setDeltaMovement(state.flightVector);
    }

    private static void updateVisualState(ServerPlayer player, FlightState state, FlightSettings settings) {
        byte phase = getVisualPhase(state, settings);
        if (state.visualPhase != phase || player.tickCount % 2 == 0) {
            state.visualPhase = phase;
            FlightNetwork.sendVisualState(player, phase, state.flightBoost, state.flightVector.x, state.flightVector.y, state.flightVector.z);
        }
    }

    private static byte getVisualPhase(FlightState state, FlightSettings settings) {
        if (settings.elytraPoseOnBoost() && state.flightBoost >= Config.flightElytraPoseBoostThreshold.get()) {
            return FlightNetwork.VISUAL_FLIGHT;
        }
        if (state.flightBoost > 0.05F || state.flightVector.horizontalDistanceSqr() > 0.0025D) {
            return FlightNetwork.VISUAL_LEVITATION;
        }
        return FlightNetwork.VISUAL_HOVER;
    }

    private static Vec3 horizontalSideVector(Vec3 look, boolean left) {
        Vec3 side = left ? look.yRot((float) Math.toRadians(90.0D)) : look.yRot((float) Math.toRadians(-90.0D));
        Vec3 horizontal = new Vec3(side.x, 0.0D, side.z);
        return horizontal.lengthSqr() < 1.0E-4D ? Vec3.ZERO : horizontal.normalize();
    }

    private static double applyBrakingMultiplier(double damping, double brakingMultiplier) {
        return Math.pow(damping, brakingMultiplier);
    }

    private static boolean canStartFlight(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.isPassenger() && !player.isSleeping();
    }

    private static boolean canKeepFlying(ServerPlayer player) {
        return FlightAbility.hasAbility(player) && canStartFlight(player) && !player.isFallFlying() && !player.isSwimming();
    }

    public static void stopFlight(ServerPlayer player, boolean notifyClient) {
        FlightState state = ACTIVE_FLIGHTS.remove(player.getUUID());
        if (state != null && player.isAlive()) {
            player.setNoGravity(state.originalNoGravity);
            player.fallDistance = 0.0F;
            FlightNetwork.sendVisualState(player, FlightNetwork.VISUAL_INACTIVE, 0.0F, 0.0D, 0.0D, 0.0D);
        }
        if (notifyClient) {
            FlightNetwork.sendState(player, false);
        }
    }

    private static boolean has(int input, int flag) {
        return (input & flag) != 0;
    }

    private static final class FlightState {
        private boolean originalNoGravity;
        private int inputFlags;
        private float flightBoost;
        private int verticalHover;
        private byte visualPhase;
        private int ticksActive;
        private boolean wasAirborne;
        private Vec3 flightVector = Vec3.ZERO;

        private FlightState(boolean originalNoGravity) {
            this.originalNoGravity = originalNoGravity;
        }
    }
}
