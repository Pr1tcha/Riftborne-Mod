package com.pr1tcha.riftborne.physical;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

public final class PhysicalTrainingData {
    public static final int CURRENT_VERSION = 1;
    public static final float DEFAULT_FORM = 40.0F;
    public static final float MAINTENANCE_THRESHOLD = 60.0F;
    public static final float COMPLETE_THRESHOLD = 100.0F;
    public static final float DAILY_GROWTH = 0.5F;
    public static final float DAILY_DECAY = 0.5F;
    public static final int GRACE_DAYS = 2;

    private final Map<PhysicalStat, Float> form = new EnumMap<>(PhysicalStat.class);
    private final Map<PhysicalStat, Float> dailyProgress = new EnumMap<>(PhysicalStat.class);
    private final Map<PhysicalStat, Integer> missedDays = new EnumMap<>(PhysicalStat.class);
    private long activeDay = Long.MIN_VALUE;

    public PhysicalTrainingData() {
        for (PhysicalStat stat : PhysicalStat.values()) {
            form.put(stat, DEFAULT_FORM);
            dailyProgress.put(stat, 0.0F);
            missedDays.put(stat, 0);
        }
    }

    public static PhysicalTrainingData load(CompoundTag tag) {
        PhysicalTrainingData data = new PhysicalTrainingData();
        CompoundTag forms = tag.getCompound("Form");
        CompoundTag progress = tag.getCompound("DailyProgress");
        CompoundTag missed = tag.getCompound("MissedDays");
        for (PhysicalStat stat : PhysicalStat.values()) {
            String key = stat.id();
            if (forms.contains(key)) {
                data.form.put(stat, Mth.clamp(forms.getFloat(key), 0.0F, 100.0F));
            }
            if (progress.contains(key)) {
                data.dailyProgress.put(stat, Mth.clamp(progress.getFloat(key), 0.0F, 100.0F));
            }
            if (missed.contains(key)) {
                data.missedDays.put(stat, Math.max(0, missed.getInt(key)));
            }
        }
        if (tag.contains("ActiveDay")) {
            data.activeDay = tag.getLong("ActiveDay");
        }
        return data;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        CompoundTag forms = new CompoundTag();
        CompoundTag progress = new CompoundTag();
        CompoundTag missed = new CompoundTag();
        for (PhysicalStat stat : PhysicalStat.values()) {
            String key = stat.id();
            forms.putFloat(key, form(stat));
            progress.putFloat(key, dailyProgress(stat));
            missed.putInt(key, missedDays(stat));
        }
        tag.put("Form", forms);
        tag.put("DailyProgress", progress);
        tag.put("MissedDays", missed);
        tag.putLong("ActiveDay", activeDay);
        tag.putInt("Version", CURRENT_VERSION);
        return tag;
    }

    public float form(PhysicalStat stat) {
        return form.getOrDefault(stat, DEFAULT_FORM);
    }

    public float dailyProgress(PhysicalStat stat) {
        return dailyProgress.getOrDefault(stat, 0.0F);
    }

    public int missedDays(PhysicalStat stat) {
        return missedDays.getOrDefault(stat, 0);
    }

    public long activeDay() {
        return activeDay;
    }

    public void beginDay(long day, boolean evaluatePrevious) {
        if (activeDay == day) {
            return;
        }
        if (evaluatePrevious && activeDay != Long.MIN_VALUE) {
            evaluateDay();
        } else {
            resetDailyProgress();
        }
        activeDay = day;
    }

    public float addProgress(PhysicalStat stat, float amount) {
        if (stat == null || amount <= 0.0F) {
            return dailyProgress(stat == null ? PhysicalStat.ENDURANCE : stat);
        }
        float next = Mth.clamp(dailyProgress(stat) + amount, 0.0F, COMPLETE_THRESHOLD);
        dailyProgress.put(stat, next);
        return next;
    }

    public float overallForm() {
        float total = 0.0F;
        for (PhysicalStat stat : PhysicalStat.values()) {
            total += form(stat);
        }
        return total / PhysicalStat.values().length;
    }

    private void evaluateDay() {
        for (PhysicalStat stat : PhysicalStat.values()) {
            float progress = dailyProgress(stat);
            if (progress >= COMPLETE_THRESHOLD) {
                form.put(stat, Mth.clamp(form(stat) + DAILY_GROWTH, 0.0F, 100.0F));
                missedDays.put(stat, 0);
            } else if (progress >= MAINTENANCE_THRESHOLD) {
                missedDays.put(stat, 0);
            } else {
                int missed = missedDays(stat) + 1;
                missedDays.put(stat, missed);
                if (missed > GRACE_DAYS) {
                    form.put(stat, Mth.clamp(form(stat) - DAILY_DECAY, 0.0F, 100.0F));
                }
            }
        }
        resetDailyProgress();
    }

    private void resetDailyProgress() {
        for (PhysicalStat stat : PhysicalStat.values()) {
            dailyProgress.put(stat, 0.0F);
        }
    }
}
