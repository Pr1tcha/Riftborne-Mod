package com.pr1tcha.Rifts;

public record RiftSpawnProfile(
        RiftType type,
        int searchRadius,
        int attempts,
        int halfWidth,
        int height,
        int halfDepth,
        int minYOffset,
        int maxYOffset
) {
    public static final RiftSpawnProfile NORMAL = new RiftSpawnProfile(
            RiftType.NORMAL_RIFT,
            28,
            64,
            2,
            5,
            2,
            -10,
            8
    );

    public static final RiftSpawnProfile PORTAL = new RiftSpawnProfile(
            RiftType.PORTAL_RIFT,
            36,
            96,
            4,
            9,
            3,
            -12,
            10
    );
}
