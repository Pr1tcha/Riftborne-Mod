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
    public static final ModConfigSpec.IntValue metaWearStableDecayInterval;
    public static final ModConfigSpec.IntValue metaWearStrainDecayInterval;
    public static final ModConfigSpec.IntValue metaWearDistortionDecayInterval;
    public static final ModConfigSpec.IntValue metaWearRejectionDecayInterval;
    public static final ModConfigSpec.DoubleValue basicRnaLoadDecayPerSecond;
    public static final ModConfigSpec.IntValue basicRnaOverloadWarningThreshold;
    public static final ModConfigSpec.IntValue basicRnaOverloadLockThreshold;
    public static final ModConfigSpec.IntValue basicRnaFocusCooldown;
    public static final ModConfigSpec.IntValue basicRnaImpulseStepCooldown;
    public static final ModConfigSpec.IntValue basicRnaOverloadVentCooldown;
    public static final ModConfigSpec.DoubleValue codexScanDistance;
    public static final ModConfigSpec.IntValue adaptationCycleTicks;
    public static final ModConfigSpec.DoubleValue adaptationActivityLow;
    public static final ModConfigSpec.DoubleValue adaptationActivityHigh;
    public static final ModConfigSpec.DoubleValue adaptationGrowthLow;
    public static final ModConfigSpec.DoubleValue adaptationGrowthHigh;
    public static final ModConfigSpec.DoubleValue adaptationGrowthFull;
    public static final ModConfigSpec.DoubleValue adaptationRequiredPractice;
    public static final ModConfigSpec.DoubleValue adaptationConnectivityC2;
    public static final ModConfigSpec.IntValue adaptationRecoveryEpisodeLimit;
    public static final ModConfigSpec.DoubleValue adaptationRecoveryLoadThreshold;
    public static final ModConfigSpec.DoubleValue adaptationSprintBlocksFull;
    public static final ModConfigSpec.DoubleValue adaptationSwimBlocksFull;
    public static final ModConfigSpec.DoubleValue adaptationMiningHardnessFull;
    public static final ModConfigSpec.DoubleValue adaptationMeleeShareCap;
    public static final ModConfigSpec.IntValue adaptationPrimitiveFullUses;
    public static final ModConfigSpec.IntValue adaptationPrimitiveHalfUses;
    public static final ModConfigSpec.DoubleValue adaptationBandwidthLoadShare;
    public static final ModConfigSpec.DoubleValue adaptationOverloadBandMin;
    public static final ModConfigSpec.DoubleValue adaptationOverloadBandMax;

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

        BUILDER.comment("Codex settings").push("codex");

        codexScanDistance = BUILDER
                .comment("Maximum Pocket Codex scan distance in blocks")
                .defineInRange("scanDistance", 12.0D, 2.0D, 64.0D);

        BUILDER.pop();

        BUILDER.comment("RNA meta-wear settings").push("rna");

        metaWearStableDecayInterval = BUILDER
                .comment("Ticks required to passively remove one meta-wear point in STABLE")
                .defineInRange("stableDecayInterval", 200, 20, 72000);

        metaWearStrainDecayInterval = BUILDER
                .comment("Ticks required to passively remove one meta-wear point in STRAIN")
                .defineInRange("strainDecayInterval", 400, 20, 72000);

        metaWearDistortionDecayInterval = BUILDER
                .comment("Ticks required to passively remove one meta-wear point in DISTORTION")
                .defineInRange("distortionDecayInterval", 800, 20, 72000);

        metaWearRejectionDecayInterval = BUILDER
                .comment("Ticks required to passively remove one meta-wear point in REJECTION")
                .defineInRange("rejectionDecayInterval", 2400, 20, 72000);

        BUILDER.pop();

        BUILDER.comment("Basic pre-aspect RNA combat settings").push("basic_rna_combat");

        basicRnaLoadDecayPerSecond = BUILDER
                .comment("Resonant combat load removed per second while the player is not adding new load")
                .defineInRange("loadDecayPerSecond", 4.0D, 0.0D, 100.0D);

        basicRnaOverloadWarningThreshold = BUILDER
                .comment("Combat load where overload warnings and unstable skill behavior may begin")
                .defineInRange("overloadWarningThreshold", 75, 1, 100);

        basicRnaOverloadLockThreshold = BUILDER
                .comment("Combat load where new basic RNA skills are blocked")
                .defineInRange("overloadLockThreshold", 90, 1, 100);

        basicRnaFocusCooldown = BUILDER
                .comment("Cooldown for rna_focus in ticks")
                .defineInRange("rnaFocusCooldown", 80, 0, 72000);

        basicRnaImpulseStepCooldown = BUILDER
                .comment("Cooldown for rna_impulse_step in ticks")
                .defineInRange("rnaImpulseStepCooldown", 60, 0, 72000);

        basicRnaOverloadVentCooldown = BUILDER
                .comment("Cooldown for rna_overload_vent in ticks")
                .defineInRange("rnaOverloadVentCooldown", 400, 0, 72000);

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

        BUILDER.comment("Base adaptation of the organism and RNA (progression v0.1)").push("adaptation");

        adaptationCycleTicks = BUILDER
                .comment("Active player ticks in one adaptation cycle (24000 = 20 real minutes)")
                .defineInRange("cycleTicks", 24000, 1200, 1728000);

        adaptationActivityLow = BUILDER
                .comment("Activity score at which a cycle starts producing growth")
                .defineInRange("activityLowThreshold", 40.0D, 0.0D, 100.0D);

        adaptationActivityHigh = BUILDER
                .comment("Activity score for the higher growth step")
                .defineInRange("activityHighThreshold", 70.0D, 0.0D, 100.0D);

        adaptationGrowthLow = BUILDER
                .comment("Physical growth per cycle at the low activity step")
                .defineInRange("growthLow", 0.10D, 0.0D, 10.0D);

        adaptationGrowthHigh = BUILDER
                .comment("Physical growth per cycle at the high activity step")
                .defineInRange("growthHigh", 0.25D, 0.0D, 10.0D);

        adaptationGrowthFull = BUILDER
                .comment("Physical growth per cycle at a full activity score")
                .defineInRange("growthFull", 0.50D, 0.0D, 10.0D);

        adaptationRequiredPractice = BUILDER
                .comment("Quality RNA practice needed in a cycle for the full per-cycle growth cap")
                .defineInRange("requiredPractice", 20.0D, 1.0D, 1000.0D);

        adaptationConnectivityC2 = BUILDER
                .comment("Hidden connectivity progress required for C1 -> C2")
                .defineInRange("connectivityC2Threshold", 60.0D, 1.0D, 10000.0D);

        adaptationRecoveryEpisodeLimit = BUILDER
                .comment("Maximum scoring Recovery episodes per cycle")
                .defineInRange("recoveryEpisodeLimit", 3, 0, 64);

        adaptationRecoveryLoadThreshold = BUILDER
                .comment("Physical load a Recovery episode must reach before it can count")
                .defineInRange("recoveryLoadThreshold", 60.0D, 1.0D, 100.0D);

        adaptationSprintBlocksFull = BUILDER
                .comment("Sprinted blocks for a full Endurance activity score")
                .defineInRange("sprintBlocksFull", 1200.0D, 1.0D, 100000.0D);

        adaptationSwimBlocksFull = BUILDER
                .comment("Swum blocks for a full Endurance activity score")
                .defineInRange("swimBlocksFull", 700.0D, 1.0D, 100000.0D);

        adaptationMiningHardnessFull = BUILDER
                .comment("Total block hardness for a full Strength activity score (~150 stone)")
                .defineInRange("miningHardnessFull", 225.0D, 1.0D, 100000.0D);

        adaptationMeleeShareCap = BUILDER
                .comment("Share of the Strength cycle norm that melee alone may fill")
                .defineInRange("meleeShareCap", 0.25D, 0.0D, 1.0D);

        adaptationPrimitiveFullUses = BUILDER
                .comment("Uses of one primitive per cycle that still give full training value")
                .defineInRange("primitiveFullUses", 8, 1, 512);

        adaptationPrimitiveHalfUses = BUILDER
                .comment("Uses of one primitive per cycle after which training value drops to zero")
                .defineInRange("primitiveHalfUses", 16, 1, 1024);

        adaptationBandwidthLoadShare = BUILDER
                .comment("Share of throughput a cast must reach to train Bandwidth")
                .defineInRange("bandwidthLoadShare", 0.40D, 0.0D, 1.0D);

        adaptationOverloadBandMin = BUILDER
                .comment("Lower bound of the load band that trains Overload Resistance")
                .defineInRange("overloadBandMin", 0.70D, 0.0D, 1.0D);

        adaptationOverloadBandMax = BUILDER
                .comment("Upper bound of the load band that trains Overload Resistance")
                .defineInRange("overloadBandMax", 1.00D, 0.0D, 2.0D);

        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // Config values are read directly from their ModConfigSpec entries.
    }
}
