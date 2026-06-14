package com.pr1tcha.Rifts;

import com.pr1tcha.Rifts.RiftData.RiftData;
import net.minecraft.resources.ResourceLocation;

public enum RiftType {
    NORMAL_RIFT(RiftData.RIFT_TYPE),
    PORTAL_RIFT(RiftData.PORTAL_RIFT_TYPE),
    ARCHIVED_RIFT(RiftData.ARCHIVED_RIFT_TYPE);

    private final ResourceLocation id;

    RiftType(ResourceLocation id) {
        this.id = id;
    }

    public ResourceLocation id() {
        return id;
    }

    public static RiftType fromId(ResourceLocation id) {
        if (RiftData.PORTAL_RIFT_TYPE.equals(id)) {
            return PORTAL_RIFT;
        }
        if (RiftData.ARCHIVED_RIFT_TYPE.equals(id)) {
            return ARCHIVED_RIFT;
        }

        return NORMAL_RIFT;
    }
}
