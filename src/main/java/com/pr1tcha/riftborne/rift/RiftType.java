package com.pr1tcha.riftborne.rift;

import com.pr1tcha.riftborne.rift.data.RiftData;
import com.pr1tcha.riftborne.Riftborne;
import net.minecraft.resources.ResourceLocation;

public enum RiftType {
    NORMAL_RIFT(RiftData.RIFT_TYPE),
    CONTOUR_RIFT(RiftData.CONTOUR_RIFT_TYPE),
    ARCHIVED_RIFT(RiftData.ARCHIVED_RIFT_TYPE);

    private final ResourceLocation id;

    RiftType(ResourceLocation id) {
        this.id = id;
    }

    public ResourceLocation id() {
        return id;
    }

    public static RiftType fromId(ResourceLocation id) {
        if (RiftData.isContourRift(id)) {
            return CONTOUR_RIFT;
        }
        if (RiftData.ARCHIVED_RIFT_TYPE.equals(id)) {
            return ARCHIVED_RIFT;
        }

        return NORMAL_RIFT;
    }
}
