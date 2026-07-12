package com.pr1tcha.riftborne.rna.combat.training;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public final class RnaTrainingSession {
    private final ResourceLocation techniqueId;
    private ResourceLocation anchorDimension;
    private BlockPos anchorPos;
    private RnaTrainingPhase phase;
    private int phaseSuccesses;
    private int totalFailures;
    private final long startedAtTick;
    private long lastResultTick;

    public RnaTrainingSession(ResourceLocation techniqueId, long startedAtTick) {
        this(techniqueId, startedAtTick, RnaTrainingPhase.FORMATION, null, null);
    }

    public RnaTrainingSession(
            ResourceLocation techniqueId,
            long startedAtTick,
            RnaTrainingPhase phase,
            ResourceLocation anchorDimension,
            BlockPos anchorPos
    ) {
        if (techniqueId == null) {
            throw new IllegalArgumentException("Training technique cannot be null");
        }
        this.techniqueId = techniqueId;
        this.phase = phase == null ? RnaTrainingPhase.FORMATION : phase;
        this.anchorDimension = anchorDimension;
        this.anchorPos = anchorPos == null ? null : anchorPos.immutable();
        this.startedAtTick = Math.max(0L, startedAtTick);
    }

    public static RnaTrainingSession load(CompoundTag tag) {
        ResourceLocation techniqueId = ResourceLocation.tryParse(tag.getString("Technique"));
        if (techniqueId == null) {
            return null;
        }
        ResourceLocation anchorDimension = ResourceLocation.tryParse(tag.getString("AnchorDimension"));
        BlockPos anchorPos = tag.contains("AnchorPos") ? BlockPos.of(tag.getLong("AnchorPos")) : null;
        RnaTrainingSession session = new RnaTrainingSession(
                techniqueId,
                tag.getLong("StartedAtTick"),
                RnaTrainingPhase.fromId(tag.getString("Phase")),
                anchorDimension,
                anchorPos
        );
        session.phaseSuccesses = Math.max(0, tag.getInt("PhaseSuccesses"));
        session.totalFailures = Math.max(0, tag.getInt("TotalFailures"));
        session.lastResultTick = Math.max(0L, tag.getLong("LastResultTick"));
        return session;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Technique", techniqueId.toString());
        tag.putString("Phase", phase.id());
        if (anchorDimension != null && anchorPos != null) {
            tag.putString("AnchorDimension", anchorDimension.toString());
            tag.putLong("AnchorPos", anchorPos.asLong());
        }
        tag.putInt("PhaseSuccesses", phaseSuccesses);
        tag.putInt("TotalFailures", totalFailures);
        tag.putLong("StartedAtTick", startedAtTick);
        tag.putLong("LastResultTick", lastResultTick);
        return tag;
    }

    public SuccessResult recordSuccess(long gameTime) {
        RnaTrainingPhase completedPhase = phase;
        phaseSuccesses++;
        lastResultTick = Math.max(0L, gameTime);
        boolean phaseCompleted = phaseSuccesses >= phase.requiredSuccesses();
        boolean sessionCompleted = false;
        if (phaseCompleted) {
            RnaTrainingPhase next = phase.next();
            if (next == null) {
                sessionCompleted = true;
            } else {
                phase = next;
                phaseSuccesses = 0;
            }
        }
        int patternGain = phaseCompleted ? completedPhase.completionPatternGain() : 0;
        return new SuccessResult(patternGain, completedPhase, phaseCompleted, sessionCompleted);
    }

    public void recordFailure(long gameTime) {
        totalFailures++;
        lastResultTick = Math.max(0L, gameTime);
    }

    public ResourceLocation techniqueId() {
        return techniqueId;
    }

    public boolean anchored() {
        return anchorDimension != null && anchorPos != null;
    }

    public ResourceLocation anchorDimension() {
        return anchorDimension;
    }

    public BlockPos anchorPos() {
        return anchorPos;
    }

    public RnaTrainingPhase phase() {
        return phase;
    }

    public int phaseSuccesses() {
        return phaseSuccesses;
    }

    public int totalFailures() {
        return totalFailures;
    }

    public long startedAtTick() {
        return startedAtTick;
    }

    public long lastResultTick() {
        return lastResultTick;
    }

    public record SuccessResult(
            int patternGain,
            RnaTrainingPhase completedPhase,
            boolean phaseCompleted,
            boolean sessionCompleted
    ) {
    }
}
