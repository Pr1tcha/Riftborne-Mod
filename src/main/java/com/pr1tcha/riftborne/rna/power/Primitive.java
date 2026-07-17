package com.pr1tcha.riftborne.rna.power;

import java.util.Locale;

/**
 * The nine universal primitives of PS V2.5 (5 space + 4 time). These are the base gameplay verbs;
 * their execution level (1-4, = depth PF09) grows from use. Reading and Stabilization are shared:
 * training them in one branch also benefits the other.
 *
 * <p>Fixed enum rather than a datapack registry: primitives are the physics of the world, not
 * content. Extensible content (Ogranki) is datapack-driven and layered on top.
 */
public enum Primitive {
    P1_READING(Branch.SPACE, true),
    P2_ANCHOR(Branch.SPACE, false),
    P3_SHIFT(Branch.SPACE, false),
    P4_SHAPE(Branch.SPACE, false),
    P5_STABILIZE(Branch.SPACE, true),
    V1_TEMPO_READ(Branch.TIME, true),
    V2_PHASE_ENTRY(Branch.TIME, false),
    V3_TEMPO_SHIFT(Branch.TIME, false),
    V4_PHASE_HOLD(Branch.TIME, true);

    private final Branch branch;
    private final boolean shared;

    Primitive(Branch branch, boolean shared) {
        this.branch = branch;
        this.shared = shared;
    }

    public Branch branch() {
        return branch;
    }

    /** Shared primitives (P1/P5/V1/V4) level up in both branches at once. */
    public boolean shared() {
        return shared;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "rna.riftborne.primitive." + id();
    }

    public static Primitive fromId(String id) {
        if (id == null) {
            return null;
        }
        for (Primitive primitive : values()) {
            if (primitive.id().equalsIgnoreCase(id)) {
                return primitive;
            }
        }
        return null;
    }
}
