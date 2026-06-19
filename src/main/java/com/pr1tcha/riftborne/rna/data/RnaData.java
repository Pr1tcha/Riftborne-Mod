package com.pr1tcha.riftborne.rna.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

public final class RnaData {
    public static final int MAX_STAT = 100;
    public static final int MAX_META_WEAR = 100;

    private boolean hasRNA;
    private int nodeDensity;
    private int connectivity;
    private int throughput;
    private int overloadResistance;
    private FormationPath formationPath = FormationPath.NONE;
    private int metaWear;
    private MetaWearStage metaWearStage = MetaWearStage.STABLE;
    private boolean criticalInstability;
    private boolean rnaCollapsed;
    private long lastMetaWearTick;
    private long lastWarningTick;

    public static RnaData load(CompoundTag tag) {
        RnaData data = new RnaData();
        data.hasRNA = tag.getBoolean("HasRNA");
        data.nodeDensity = clampStat(tag.getInt("NodeDensity"));
        data.connectivity = clampStat(tag.getInt("Connectivity"));
        data.throughput = clampStat(tag.getInt("Throughput"));
        data.overloadResistance = clampStat(tag.getInt("OverloadResistance"));
        data.formationPath = FormationPath.fromId(tag.getString("FormationPath"));
        data.metaWear = Mth.clamp(tag.getInt("MetaWear"), 0, MAX_META_WEAR);
        data.metaWearStage = MetaWearStage.fromWear(data.metaWear);
        data.criticalInstability = tag.getBoolean("CriticalInstability") || data.metaWearStage == MetaWearStage.ARCHITECTURE_BREAK;
        data.rnaCollapsed = tag.getBoolean("RnaCollapsed");
        data.lastMetaWearTick = tag.getLong("LastMetaWearTick");
        data.lastWarningTick = tag.getLong("LastWarningTick");
        return data;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("HasRNA", hasRNA);
        tag.putInt("NodeDensity", nodeDensity);
        tag.putInt("Connectivity", connectivity);
        tag.putInt("Throughput", throughput);
        tag.putInt("OverloadResistance", overloadResistance);
        tag.putString("FormationPath", formationPath.name());
        tag.putInt("MetaWear", metaWear);
        tag.putString("MetaWearStage", metaWearStage.name());
        tag.putBoolean("CriticalInstability", criticalInstability);
        tag.putBoolean("RnaCollapsed", rnaCollapsed);
        tag.putLong("LastMetaWearTick", lastMetaWearTick);
        tag.putLong("LastWarningTick", lastWarningTick);
        return tag;
    }

    public void initialize(FormationPath path) {
        hasRNA = true;
        formationPath = path != null && path.isSelectable() ? path : FormationPath.TRAINING;
        rnaCollapsed = false;
        metaWear = 0;
        metaWearStage = MetaWearStage.STABLE;
        criticalInstability = false;

        switch (formationPath) {
            case TRAINING -> {
                nodeDensity = 25;
                connectivity = 35;
                throughput = 25;
                overloadResistance = 35;
            }
            case STRESS -> {
                nodeDensity = 35;
                connectivity = 22;
                throughput = 35;
                overloadResistance = 22;
            }
            case ARTIFICIAL_BORN -> {
                nodeDensity = 45;
                connectivity = 30;
                throughput = 32;
                overloadResistance = 25;
            }
            default -> {
                nodeDensity = 25;
                connectivity = 25;
                throughput = 25;
                overloadResistance = 25;
            }
        }
    }

    public void reset() {
        hasRNA = false;
        nodeDensity = 0;
        connectivity = 0;
        throughput = 0;
        overloadResistance = 0;
        formationPath = FormationPath.NONE;
        metaWear = 0;
        metaWearStage = MetaWearStage.STABLE;
        criticalInstability = false;
        rnaCollapsed = false;
        lastMetaWearTick = 0L;
        lastWarningTick = 0L;
    }

    public void collapse() {
        hasRNA = false;
        nodeDensity = 0;
        connectivity = 0;
        throughput = 0;
        overloadResistance = 0;
        formationPath = FormationPath.NONE;
        metaWear = 0;
        metaWearStage = MetaWearStage.STABLE;
        criticalInstability = false;
        rnaCollapsed = true;
    }

    public int getStat(RnaStat stat) {
        return switch (stat) {
            case NODE_DENSITY -> nodeDensity;
            case CONNECTIVITY -> connectivity;
            case THROUGHPUT -> throughput;
            case OVERLOAD_RESISTANCE -> overloadResistance;
        };
    }

    public void setStat(RnaStat stat, int value) {
        int clamped = clampStat(value);
        switch (stat) {
            case NODE_DENSITY -> nodeDensity = clamped;
            case CONNECTIVITY -> connectivity = clamped;
            case THROUGHPUT -> throughput = clamped;
            case OVERLOAD_RESISTANCE -> overloadResistance = clamped;
        }
    }

    private static int clampStat(int value) {
        return Mth.clamp(value, 0, MAX_STAT);
    }

    public boolean hasRNA() {
        return hasRNA;
    }

    public int nodeDensity() {
        return nodeDensity;
    }

    public int connectivity() {
        return connectivity;
    }

    public int throughput() {
        return throughput;
    }

    public int overloadResistance() {
        return overloadResistance;
    }

    public FormationPath formationPath() {
        return formationPath;
    }

    public void setFormationPath(FormationPath formationPath) {
        this.formationPath = formationPath;
    }

    public int metaWear() {
        return metaWear;
    }

    public void setMetaWear(int metaWear) {
        this.metaWear = Mth.clamp(metaWear, 0, MAX_META_WEAR);
        this.metaWearStage = MetaWearStage.fromWear(this.metaWear);
        this.criticalInstability = this.metaWearStage == MetaWearStage.ARCHITECTURE_BREAK;
    }

    public MetaWearStage metaWearStage() {
        return metaWearStage;
    }

    public boolean criticalInstability() {
        return criticalInstability;
    }

    public boolean rnaCollapsed() {
        return rnaCollapsed;
    }

    public long lastMetaWearTick() {
        return lastMetaWearTick;
    }

    public void setLastMetaWearTick(long lastMetaWearTick) {
        this.lastMetaWearTick = lastMetaWearTick;
    }

    public long lastWarningTick() {
        return lastWarningTick;
    }

    public void setLastWarningTick(long lastWarningTick) {
        this.lastWarningTick = lastWarningTick;
    }
}
