package com.pr1tcha.riftborne.config;

import com.pr1tcha.riftborne.Riftborne;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = Riftborne.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue riftReactingRadius;
    public static final ModConfigSpec.IntValue riftCrackingRadius;
    public static final ModConfigSpec.IntValue riftOpeningRadius;
    public static final ModConfigSpec.IntValue riftOpeningDurationTicks;
    public static final ModConfigSpec.DoubleValue flightSlowBoostMax;
    public static final ModConfigSpec.DoubleValue flightFastBoostMax;
    public static final ModConfigSpec.DoubleValue flightSlowAcceleration;
    public static final ModConfigSpec.DoubleValue flightFastAcceleration;
    public static final ModConfigSpec.DoubleValue flightReleaseDeceleration;
    public static final ModConfigSpec.DoubleValue flightBrakeDeceleration;
    public static final ModConfigSpec.DoubleValue flightBoostSettleDeceleration;
    public static final ModConfigSpec.DoubleValue flightSlowSpeed;
    public static final ModConfigSpec.DoubleValue flightBoostSpeedStep;
    public static final ModConfigSpec.DoubleValue flightNormalSteering;
    public static final ModConfigSpec.DoubleValue flightBoostSteering;
    public static final ModConfigSpec.DoubleValue flightSideInfluence;
    public static final ModConfigSpec.DoubleValue flightCoastDamping;
    public static final ModConfigSpec.DoubleValue flightBrakeDamping;
    public static final ModConfigSpec.DoubleValue flightStopSnapSpeed;
    public static final ModConfigSpec.BooleanValue flightHoverEnabled;
    public static final ModConfigSpec.DoubleValue flightHoverHorizontalDamping;
    public static final ModConfigSpec.DoubleValue flightHoverVerticalBlend;
    public static final ModConfigSpec.DoubleValue flightHoverControlVerticalBlend;
    public static final ModConfigSpec.DoubleValue flightHoverBobAmplitude;
    public static final ModConfigSpec.IntValue flightVerticalControlTicks;
    public static final ModConfigSpec.DoubleValue flightVerticalSpeed;
    public static final ModConfigSpec.DoubleValue flightNoHoverSinkSpeed;
    public static final ModConfigSpec.BooleanValue flightElytraPoseOnBoost;
    public static final ModConfigSpec.DoubleValue flightElytraPoseBoostThreshold;

    static {
        BUILDER.comment("Rift system settings").push("rifts");

        riftReactingRadius = BUILDER
                .comment("Distance in blocks where a dormant normal rift starts reacting to players")
                .defineInRange("reactingRadius", 16, 1, 128);

        riftCrackingRadius = BUILDER
                .comment("Distance in blocks where a normal rift grows visible side cracks")
                .defineInRange("crackingRadius", 10, 1, 128);

        riftOpeningRadius = BUILDER
                .comment("Distance in blocks where a normal rift begins its one-way opening burst")
                .defineInRange("openingRadius", 6, 1, 128);

        riftOpeningDurationTicks = BUILDER
                .comment("Duration of the normal rift opening burst before it becomes active")
                .defineInRange("openingDurationTicks", 60, 1, 1200);

        BUILDER.pop();

        BUILDER.comment("Rift flight ability settings").push("flight");

        flightSlowBoostMax = BUILDER
                .comment("Maximum boost level for regular forward flight")
                .defineInRange("slowBoostMax", 1.0D, 0.1D, 10.0D);

        flightFastBoostMax = BUILDER
                .comment("Maximum boost level while sprinting forward")
                .defineInRange("fastBoostMax", 3.0D, 0.1D, 10.0D);

        flightSlowAcceleration = BUILDER
                .comment("Boost gained per tick while holding forward")
                .defineInRange("slowAcceleration", 0.055D, 0.001D, 1.0D);

        flightFastAcceleration = BUILDER
                .comment("Boost gained per tick while sprinting forward")
                .defineInRange("fastAcceleration", 0.18D, 0.001D, 1.0D);

        flightReleaseDeceleration = BUILDER
                .comment("Boost lost per tick after releasing forward")
                .defineInRange("releaseDeceleration", 0.075D, 0.001D, 1.0D);

        flightBrakeDeceleration = BUILDER
                .comment("Boost lost per tick while holding backward")
                .defineInRange("brakeDeceleration", 0.2D, 0.001D, 1.0D);

        flightBoostSettleDeceleration = BUILDER
                .comment("Boost lost per tick when leaving sprint boost but still flying forward")
                .defineInRange("boostSettleDeceleration", 0.075D, 0.001D, 1.0D);

        flightSlowSpeed = BUILDER
                .comment("Velocity scale for regular forward flight")
                .defineInRange("slowSpeed", 0.42D, 0.05D, 3.0D);

        flightBoostSpeedStep = BUILDER
                .comment("Extra velocity scale added per boost level above regular flight")
                .defineInRange("boostSpeedStep", 0.31D, 0.0D, 3.0D);

        flightNormalSteering = BUILDER
                .comment("Maximum velocity steering change per tick during regular flight")
                .defineInRange("normalSteering", 0.105D, 0.001D, 1.0D);

        flightBoostSteering = BUILDER
                .comment("Maximum velocity steering change per tick during sprint boost")
                .defineInRange("boostSteering", 0.075D, 0.001D, 1.0D);

        flightSideInfluence = BUILDER
                .comment("How much left/right input bends the forward flight direction")
                .defineInRange("sideInfluence", 0.42D, 0.0D, 2.0D);

        flightCoastDamping = BUILDER
                .comment("Velocity retained each tick after releasing forward; lower values stop faster")
                .defineInRange("coastDamping", 0.94D, 0.0D, 1.0D);

        flightBrakeDamping = BUILDER
                .comment("Velocity retained each tick while holding backward; lower values brake faster")
                .defineInRange("brakeDamping", 0.82D, 0.0D, 1.0D);

        flightStopSnapSpeed = BUILDER
                .comment("Velocity below this speed snaps to a clean stop")
                .defineInRange("stopSnapSpeed", 0.035D, 0.0D, 0.5D);

        flightHoverEnabled = BUILDER
                .comment("Whether active flight holds the player in place when no forward boost remains")
                .define("hoverEnabled", true);

        flightHoverHorizontalDamping = BUILDER
                .comment("Horizontal velocity retained each tick while hovering")
                .defineInRange("hoverHorizontalDamping", 0.82D, 0.0D, 1.0D);

        flightHoverVerticalBlend = BUILDER
                .comment("How quickly hovering blends toward its idle vertical motion")
                .defineInRange("hoverVerticalBlend", 0.22D, 0.0D, 1.0D);

        flightHoverControlVerticalBlend = BUILDER
                .comment("How quickly jump/crouch hover control blends toward the requested vertical speed")
                .defineInRange("hoverControlVerticalBlend", 0.34D, 0.0D, 1.0D);

        flightHoverBobAmplitude = BUILDER
                .comment("Idle hover bob vertical velocity amplitude")
                .defineInRange("hoverBobAmplitude", 0.01D, 0.0D, 0.2D);

        flightVerticalControlTicks = BUILDER
                .comment("Ticks needed for jump/crouch hover control to reach full vertical speed")
                .defineInRange("verticalControlTicks", 22, 1, 80);

        flightVerticalSpeed = BUILDER
                .comment("Maximum vertical hover speed from jump/crouch input")
                .defineInRange("verticalSpeed", 0.31D, 0.0D, 2.0D);

        flightNoHoverSinkSpeed = BUILDER
                .comment("Downward speed target when hover is disabled and there is no vertical input")
                .defineInRange("noHoverSinkSpeed", 0.08D, 0.0D, 1.0D);

        flightElytraPoseOnBoost = BUILDER
                .comment("Whether sprint boost should render the player in a fall-flying pose")
                .define("elytraPoseOnBoost", true);

        flightElytraPoseBoostThreshold = BUILDER
                .comment("Boost level required before the fall-flying pose is shown")
                .defineInRange("elytraPoseBoostThreshold", 1.2D, 0.0D, 10.0D);

        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // Config values are read directly from their ModConfigSpec entries.
    }
}
