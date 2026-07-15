package com.pr1tcha.riftborne.flight;

import com.pr1tcha.riftborne.config.Config;
import java.util.List;
import java.util.Locale;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

public record FlightSettings(
        double speedMultiplier,
        double accelerationMultiplier,
        double maxSpeed,
        double steeringMultiplier,
        double brakingMultiplier,
        double verticalSpeedMultiplier,
        boolean hoverEnabled,
        boolean elytraPoseOnBoost
) {
    private static final String SPEED_MULTIPLIER = "SpeedMultiplier";
    private static final String ACCELERATION_MULTIPLIER = "AccelerationMultiplier";
    private static final String MAX_SPEED = "MaxSpeed";
    private static final String STEERING_MULTIPLIER = "SteeringMultiplier";
    private static final String BRAKING_MULTIPLIER = "BrakingMultiplier";
    private static final String VERTICAL_SPEED_MULTIPLIER = "VerticalSpeedMultiplier";
    private static final String HOVER_ENABLED = "HoverEnabled";
    private static final String ELYTRA_POSE_ON_BOOST = "ElytraPoseOnBoost";

    private static final List<String> PRESETS = List.of("slow", "normal", "fast", "overdrive", "precise", "hoverless");

    public static FlightSettings defaults() {
        return new FlightSettings(
                1.0D,
                1.0D,
                configuredMaxSpeed(),
                1.0D,
                1.0D,
                1.0D,
                Config.flightHoverEnabled.get(),
                Config.flightElytraPoseOnBoost.get()
        );
    }

    public static FlightSettings preset(String preset) {
        double baseMaxSpeed = configuredMaxSpeed();
        return switch (preset.toLowerCase(Locale.ROOT)) {
            case "slow" -> new FlightSettings(0.75D, 0.75D, baseMaxSpeed * 0.75D, 1.2D, 1.15D, 0.85D, true, false);
            case "normal" -> defaults();
            case "fast" -> new FlightSettings(1.25D, 1.2D, baseMaxSpeed * 1.4D, 0.9D, 1.1D, 1.0D, true, true);
            case "overdrive" -> new FlightSettings(1.6D, 1.45D, baseMaxSpeed * 1.9D, 0.75D, 1.2D, 1.05D, true, true);
            case "precise" -> new FlightSettings(0.9D, 0.85D, baseMaxSpeed * 0.9D, 1.35D, 1.4D, 0.8D, true, false);
            case "hoverless" -> new FlightSettings(1.0D, 1.0D, baseMaxSpeed, 1.0D, 1.0D, 1.0D, false, true);
            default -> null;
        };
    }

    public static List<String> presetNames() {
        return PRESETS;
    }

    public static FlightSettings load(CompoundTag tag) {
        FlightSettings defaults = defaults();
        return new FlightSettings(
                readDouble(tag, SPEED_MULTIPLIER, defaults.speedMultiplier()),
                readDouble(tag, ACCELERATION_MULTIPLIER, defaults.accelerationMultiplier()),
                readDouble(tag, MAX_SPEED, defaults.maxSpeed()),
                readDouble(tag, STEERING_MULTIPLIER, defaults.steeringMultiplier()),
                readDouble(tag, BRAKING_MULTIPLIER, defaults.brakingMultiplier()),
                readDouble(tag, VERTICAL_SPEED_MULTIPLIER, defaults.verticalSpeedMultiplier()),
                tag.contains(HOVER_ENABLED) ? tag.getBoolean(HOVER_ENABLED) : defaults.hoverEnabled(),
                tag.contains(ELYTRA_POSE_ON_BOOST) ? tag.getBoolean(ELYTRA_POSE_ON_BOOST) : defaults.elytraPoseOnBoost()
        ).sanitize();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble(SPEED_MULTIPLIER, this.speedMultiplier);
        tag.putDouble(ACCELERATION_MULTIPLIER, this.accelerationMultiplier);
        tag.putDouble(MAX_SPEED, this.maxSpeed);
        tag.putDouble(STEERING_MULTIPLIER, this.steeringMultiplier);
        tag.putDouble(BRAKING_MULTIPLIER, this.brakingMultiplier);
        tag.putDouble(VERTICAL_SPEED_MULTIPLIER, this.verticalSpeedMultiplier);
        tag.putBoolean(HOVER_ENABLED, this.hoverEnabled);
        tag.putBoolean(ELYTRA_POSE_ON_BOOST, this.elytraPoseOnBoost);
        return tag;
    }

    public FlightSettings sanitize() {
        return new FlightSettings(
                Mth.clamp(this.speedMultiplier, 0.05D, 10.0D),
                Mth.clamp(this.accelerationMultiplier, 0.05D, 10.0D),
                Mth.clamp(this.maxSpeed, 0.05D, 10.0D),
                Mth.clamp(this.steeringMultiplier, 0.05D, 5.0D),
                Mth.clamp(this.brakingMultiplier, 0.05D, 10.0D),
                Mth.clamp(this.verticalSpeedMultiplier, 0.0D, 5.0D),
                this.hoverEnabled,
                this.elytraPoseOnBoost
        );
    }

    public FlightSettings withSpeedMultiplier(double value) {
        return new FlightSettings(value, this.accelerationMultiplier, this.maxSpeed, this.steeringMultiplier,
                this.brakingMultiplier, this.verticalSpeedMultiplier, this.hoverEnabled, this.elytraPoseOnBoost).sanitize();
    }

    public FlightSettings withAccelerationMultiplier(double value) {
        return new FlightSettings(this.speedMultiplier, value, this.maxSpeed, this.steeringMultiplier,
                this.brakingMultiplier, this.verticalSpeedMultiplier, this.hoverEnabled, this.elytraPoseOnBoost).sanitize();
    }

    public FlightSettings withMaxSpeed(double value) {
        return new FlightSettings(this.speedMultiplier, this.accelerationMultiplier, value, this.steeringMultiplier,
                this.brakingMultiplier, this.verticalSpeedMultiplier, this.hoverEnabled, this.elytraPoseOnBoost).sanitize();
    }

    public FlightSettings withSteeringMultiplier(double value) {
        return new FlightSettings(this.speedMultiplier, this.accelerationMultiplier, this.maxSpeed, value,
                this.brakingMultiplier, this.verticalSpeedMultiplier, this.hoverEnabled, this.elytraPoseOnBoost).sanitize();
    }

    public FlightSettings withBrakingMultiplier(double value) {
        return new FlightSettings(this.speedMultiplier, this.accelerationMultiplier, this.maxSpeed, this.steeringMultiplier,
                value, this.verticalSpeedMultiplier, this.hoverEnabled, this.elytraPoseOnBoost).sanitize();
    }

    public FlightSettings withVerticalSpeedMultiplier(double value) {
        return new FlightSettings(this.speedMultiplier, this.accelerationMultiplier, this.maxSpeed, this.steeringMultiplier,
                this.brakingMultiplier, value, this.hoverEnabled, this.elytraPoseOnBoost).sanitize();
    }

    public FlightSettings withHoverEnabled(boolean value) {
        return new FlightSettings(this.speedMultiplier, this.accelerationMultiplier, this.maxSpeed, this.steeringMultiplier,
                this.brakingMultiplier, this.verticalSpeedMultiplier, value, this.elytraPoseOnBoost);
    }

    public FlightSettings withElytraPoseOnBoost(boolean value) {
        return new FlightSettings(this.speedMultiplier, this.accelerationMultiplier, this.maxSpeed, this.steeringMultiplier,
                this.brakingMultiplier, this.verticalSpeedMultiplier, this.hoverEnabled, value);
    }

    public String describe() {
        return String.format(Locale.ROOT,
                "speed=%.2fx, acceleration=%.2fx, maxSpeed=%.2f, steering=%.2fx, braking=%.2fx, vertical=%.2fx, hover=%s, elytraPose=%s",
                this.speedMultiplier,
                this.accelerationMultiplier,
                this.maxSpeed,
                this.steeringMultiplier,
                this.brakingMultiplier,
                this.verticalSpeedMultiplier,
                this.hoverEnabled,
                this.elytraPoseOnBoost
        );
    }

    private static double configuredMaxSpeed() {
        double fastBoostMax = Math.max(Config.flightSlowBoostMax.get(), Config.flightFastBoostMax.get());
        return Config.flightSlowSpeed.get() * Math.min(fastBoostMax, 1.0D)
                + Config.flightBoostSpeedStep.get() * Math.max(0.0D, fastBoostMax - 1.0D);
    }

    private static double readDouble(CompoundTag tag, String key, double fallback) {
        return tag.contains(key) ? tag.getDouble(key) : fallback;
    }
}
