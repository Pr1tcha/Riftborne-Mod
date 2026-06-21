uniform float RiftTime;
uniform int RiftTier;
uniform vec3 PrimaryColor;
uniform vec3 SecondaryColor;
uniform float Intensity;

in vec3 skyDirection;

out vec4 fragColor;

float hash31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    return mix(
        mix(mix(hash31(i), hash31(i + vec3(1, 0, 0)), f.x),
            mix(hash31(i + vec3(0, 1, 0)), hash31(i + vec3(1, 1, 0)), f.x), f.y),
        mix(mix(hash31(i + vec3(0, 0, 1)), hash31(i + vec3(1, 0, 1)), f.x),
            mix(hash31(i + vec3(0, 1, 1)), hash31(i + vec3(1, 1, 1)), f.x), f.y),
        f.z
    );
}

float fbm(vec3 p) {
    float value = 0.0;
    float amplitude = 0.5;
    mat3 rotation = mat3(
         0.00,  0.80,  0.60,
        -0.80,  0.36, -0.48,
        -0.60, -0.48,  0.64
    );

    for (int i = 0; i < 5; i++) {
        value += noise3(p) * amplitude;
        p = rotation * p * 2.03 + 7.17;
        amplitude *= 0.5;
    }
    return value;
}

float stars(vec3 direction, float scale, float threshold) {
    vec3 cell = floor(direction * scale);
    vec3 local = fract(direction * scale) - 0.5;
    float seed = hash31(cell);
    float star = smoothstep(threshold, 1.0, seed);
    float point = exp(-dot(local, local) * 84.0);
    return star * point;
}

float fractureField(vec3 direction, float time, float scale) {
    vec3 p = direction * scale;
    float a = fbm(p + vec3(time * 0.018, 0.0, 0.0));
    float b = fbm(p * 1.73 - vec3(0.0, time * 0.013, 0.0));
    float ridge = abs(a - b);
    return 1.0 - smoothstep(0.025, 0.105, ridge);
}

vec3 rotateAxis(vec3 p, vec3 axis, float angle) {
    axis = normalize(axis);
    float c = cos(angle);
    float s = sin(angle);
    return p * c + cross(axis, p) * s + axis * dot(axis, p) * (1.0 - c);
}

vec3 surfaceShardPattern(vec3 direction, float time) {
    float haze = smoothstep(0.55, 0.82, fbm(direction * 3.1 + vec3(time * 0.004, 0.0, 0.0)));
    float cracks = fractureField(direction, time * 0.55, 6.5);
    float sparse = smoothstep(0.48, 0.76, fbm(direction * 5.2 - vec3(0.0, time * 0.003, 0.0)));
    return vec3(haze * 0.62, cracks * sparse, 0.0);
}

vec3 shiftedLayerPattern(vec3 direction, float time) {
    vec3 p = rotateAxis(direction, normalize(vec3(0.7, 0.2, -0.5)), 0.52);
    float warp = fbm(p * 4.0 + vec3(time * 0.008, 0.0, 0.0)) - 0.5;
    float coordinate = p.y * 8.5 + warp * 3.2;
    float bands = pow(0.5 + 0.5 * cos(coordinate * 3.14159265), 9.0);
    float displaced = pow(0.5 + 0.5 * cos((coordinate + 0.72 + sin(p.x * 7.0) * 0.22) * 3.14159265), 13.0);
    float layerBody = smoothstep(0.46, 0.76, fbm(p * 5.8 - vec3(time * 0.006, 0.0, 0.0)));
    float gaps = max(bands, displaced * 0.72) * (0.42 + layerBody);
    float haze = smoothstep(0.48, 0.79, fbm(p * 3.4 + vec3(0.0, time * 0.004, 0.0)));
    return vec3(haze, gaps, displaced);
}

vec3 nodeRiftPattern(vec3 direction, float time) {
    vec3 p = direction;
    float nodeA = exp(-pow(acos(clamp(dot(p, normalize(vec3(0.64, 0.35, -0.68))), -1.0, 1.0)) * 3.3, 2.0));
    float nodeB = exp(-pow(acos(clamp(dot(p, normalize(vec3(-0.48, -0.18, 0.86))), -1.0, 1.0)) * 4.4, 2.0));
    float nodeC = exp(-pow(acos(clamp(dot(p, normalize(vec3(-0.22, 0.91, 0.34))), -1.0, 1.0)) * 5.1, 2.0));
    float nodes = max(nodeA, max(nodeB * 0.82, nodeC * 0.64));

    float fieldA = fbm(p * 7.2 + vec3(time * 0.008, 0.0, 0.0));
    float fieldB = fbm(p * 12.5 - vec3(0.0, time * 0.011, 0.0));
    float filaments = 1.0 - smoothstep(0.025, 0.095, abs(fieldA - fieldB));
    filaments *= 0.42 + nodes * 1.25;
    float halo = smoothstep(0.03, 0.55, nodes) * (0.55 + fieldA * 0.45);
    return vec3(halo, filaments, nodes);
}

vec3 deepRiftPattern(vec3 direction, float time) {
    vec3 p = rotateAxis(direction, normalize(vec3(0.2, 0.8, 0.3)), -0.34);
    float currentA = fbm(vec3(p.x * 2.2, p.y * 6.5, p.z * 2.2) + vec3(0.0, time * 0.003, 0.0));
    float currentB = fbm(vec3(p.x * 3.7, p.y * 9.0, p.z * 3.7) - vec3(time * 0.002, 0.0, 0.0));
    float currents = smoothstep(0.57, 0.82, currentA * 0.72 + currentB * 0.38);
    float trenches = 1.0 - smoothstep(0.04, 0.16, abs(currentA - currentB));
    float depth = pow(1.0 - abs(p.y), 2.2);
    return vec3(currents * depth, trenches * depth, depth);
}

vec3 limitSlicePattern(vec3 direction, float time) {
    vec3 p = rotateAxis(direction, normalize(vec3(-0.4, 0.7, 0.55)), time * 0.006);
    float planeA = abs(dot(p, normalize(vec3(0.82, 0.22, 0.53))));
    float planeB = abs(dot(p, normalize(vec3(-0.31, 0.91, 0.27))));
    float planeC = abs(dot(p, normalize(vec3(0.18, -0.42, 0.89))));
    float slices = 1.0 - smoothstep(0.012, 0.055, min(planeA, min(planeB, planeC)));

    float cells = fbm(p * 8.5 + vec3(time * 0.018, -time * 0.011, 0.0));
    float cuts = pow(0.5 + 0.5 * cos((p.x + p.y * 0.63 - p.z * 0.38) * 24.0 + cells * 6.0), 18.0);
    float instability = smoothstep(0.56, 0.84, fbm(p * 13.0 - vec3(time * 0.025)));
    return vec3(instability, max(slices, cuts * 0.68), slices * instability);
}

vec3 riftwalkerPattern(vec3 direction, float time) {
    vec3 p = rotateAxis(direction, normalize(vec3(0.62, 0.16, -0.77)), -0.21);

    // Broad, almost black rock plates. This is deliberately low contrast: the
    // fracture network, rather than cloudy noise, must carry the silhouette.
    float plateNoise = fbm(p * 3.6 + vec3(time * 0.00045, 0.0, 0.0));
    float detailNoise = fbm(p * 9.2 - vec3(0.0, time * 0.0007, 0.0));
    float darkPlates = smoothstep(0.26, 0.82, plateNoise * 0.72 + detailNoise * 0.28);

    // Warped directional ridges form a few long lightning trunks. Unlike the
    // previous difference-of-noise field these do not outline every noise cell.
    float warpA = fbm(p * 4.1 + vec3(3.7, -1.2, 5.4)) - 0.5;
    float warpB = fbm(p * 7.7 + vec3(-4.3, 6.1, 1.8)) - 0.5;
    vec3 q = p + vec3(warpA, warpB, warpA - warpB) * 0.19;

    float trunkA = abs(sin(dot(q, normalize(vec3(0.86, 0.31, -0.41))) * 7.2 + warpB * 5.8));
    float trunkB = abs(sin(dot(q, normalize(vec3(-0.37, 0.91, 0.19))) * 8.6 - warpA * 6.4 + 1.7));
    float trunkC = abs(sin(dot(q, normalize(vec3(0.28, 0.46, 0.84))) * 6.4 + (warpA + warpB) * 5.1 - 0.8));
    float trunkDistance = min(trunkA, min(trunkB, trunkC));
    float trunks = 1.0 - smoothstep(0.018, 0.082, trunkDistance);

    // Fine branches live mainly close to a trunk, producing the forked corona
    // visible in the Riftwalker vein texture without filling the whole sky.
    vec3 branchP = rotateAxis(q, normalize(vec3(0.24, -0.78, 0.58)), 0.67);
    float branchWarp = fbm(branchP * 13.0 + vec3(7.0, -2.0, 4.0)) - 0.5;
    float branchA = abs(sin(dot(branchP, normalize(vec3(0.71, -0.22, 0.67))) * 19.0 + branchWarp * 7.0));
    float branchB = abs(sin(dot(branchP, normalize(vec3(-0.18, 0.64, 0.75))) * 23.0 - branchWarp * 8.3));
    float branchDistance = min(branchA, branchB);
    float branches = 1.0 - smoothstep(0.012, 0.052, branchDistance);
    float trunkHalo = 1.0 - smoothstep(0.04, 0.34, trunkDistance);
    branches *= trunkHalo * smoothstep(0.30, 0.68, detailNoise);

    float lightning = max(trunks, branches * 0.78);
    float pulse = 0.82 + 0.18 * sin(time * 1.12 + warpA * 11.0 + warpB * 7.0);
    lightning *= pulse;

    float glow = 1.0 - smoothstep(0.045, 0.27, trunkDistance);
    glow = max(glow * 0.58, branches * 0.48);
    return vec3(darkPlates * 0.16, lightning, glow);
}

vec3 rnaPattern(vec3 direction, float time) {
    vec3 axis = normalize(vec3(0.12, 0.97, -0.21));
    float axisDot = clamp(dot(direction, axis), -1.0, 1.0);
    float polar = acos(axisDot);
    vec3 tangent = normalize(direction - axis * axisDot + vec3(0.0001));
    float angle = atan(dot(tangent, normalize(vec3(0.96, -0.02, 0.28))),
                       dot(tangent, normalize(cross(axis, vec3(0.96, -0.02, 0.28)))));

    float warp = fbm(direction * 4.2 + vec3(time * 0.0018, 0.0, -time * 0.0011)) - 0.5;
    // The angular multiplier must be an integer so both sides of atan's
    // -PI/PI boundary return to the same phase without a visible sphere seam.
    float spiralCoord = polar * 9.4 + angle * 2.0 + warp * 2.7 - time * 0.055;
    float broadArms = pow(0.5 + 0.5 * cos(spiralCoord), 7.0);
    float fineRings = pow(0.5 + 0.5 * cos(polar * 34.0 + warp * 4.0 - time * 0.09), 18.0);
    float disk = 1.0 - smoothstep(0.12, 1.36, polar);
    float vortex = (broadArms * 0.72 + fineRings * 0.46) * disk;

    float core = exp(-polar * polar * 18.0);
    float cloudA = fbm(direction * 3.0 + vec3(0.0, time * 0.0014, 0.0));
    float cloudB = fbm(direction * 7.0 - vec3(time * 0.001, 0.0, 0.0));
    float clouds = smoothstep(0.43, 0.78, cloudA * 0.72 + cloudB * 0.34);
    clouds *= 0.30 + disk * 0.70;

    float beamCells = abs(sin(angle * 17.0 + warp * 5.0));
    float beams = 1.0 - smoothstep(0.018, 0.065, beamCells);
    beams *= smoothstep(0.18, 0.82, disk) * smoothstep(0.32, 0.72, cloudB);
    return vec3(clouds, vortex + beams * 0.54, core);
}

void main() {
    vec3 direction = normalize(skyDirection);
    float time = RiftTime;
    float tier = float(RiftTier);

    float vertical = direction.y * 0.5 + 0.5;
    vec3 base = mix(vec3(0.0015, 0.0025, 0.009), vec3(0.006, 0.012, 0.035), pow(vertical, 1.4));

    vec3 pattern;
    if (RiftTier == -1) {
        pattern = rnaPattern(direction, time);
    } else if (RiftTier == 0) {
        pattern = riftwalkerPattern(direction, time);
    } else if (RiftTier == 1) {
        pattern = surfaceShardPattern(direction, time);
    } else if (RiftTier == 2) {
        pattern = shiftedLayerPattern(direction, time);
    } else if (RiftTier == 3) {
        pattern = nodeRiftPattern(direction, time);
    } else if (RiftTier == 4) {
        pattern = deepRiftPattern(direction, time);
    } else {
        pattern = limitSlicePattern(direction, time);
    }

    float smallStars = stars(direction, 410.0, 0.985);
    float brightStars = stars(direction + vec3(0.173, -0.271, 0.091), 155.0, 0.996);
    float twinkle = 0.78 + 0.22 * sin(time * 1.4 + hash31(floor(direction * 155.0)) * 20.0);

    vec3 color = base;
    color += PrimaryColor * pattern.x * 0.28 * Intensity;
    color += mix(PrimaryColor, SecondaryColor, pattern.z) * pattern.y * (0.18 + tier * 0.025) * Intensity;
    color += SecondaryColor * pattern.z * 0.24 * Intensity;

    float starDensity = RiftTier == -1 ? 0.18 : (RiftTier == 0 ? 0.42 : (RiftTier == 4 ? 0.28 : (RiftTier == 5 ? 0.72 : 1.0)));
    color += vec3(0.48, 0.72, 1.0) * smallStars * 0.68 * starDensity;
    color += mix(vec3(0.65, 0.82, 1.0), SecondaryColor, 0.35) * brightStars * twinkle * 1.7 * starDensity;

    if (RiftTier == -1) {
        float core = pow(pattern.z, 1.45);
        color = mix(vec3(0.018, 0.105, 0.19), vec3(0.12, 0.39, 0.62), vertical * 0.72);
        color += PrimaryColor * pattern.x * 0.24 * Intensity;
        color += vec3(0.08, 0.68, 1.00) * pattern.y * 0.44 * Intensity;
        color += vec3(0.56, 0.94, 1.00) * pattern.y * pattern.y * 0.34 * Intensity;
        color += vec3(0.88, 0.99, 1.00) * core * 1.72 * Intensity;
    } else if (RiftTier == 0) {
        float crackCore = pow(pattern.y, 2.65);
        color *= vec3(0.24, 0.18, 0.44);
        color += PrimaryColor * pattern.x * 0.09;
        color += vec3(0.25, 0.018, 0.78) * pattern.z * 0.34 * Intensity;
        color += vec3(0.48, 0.055, 1.00) * pattern.y * 0.52 * Intensity;
        color += vec3(0.94, 0.66, 1.00) * crackCore * 0.96 * Intensity;
    } else if (RiftTier == 3) {
        float pulse = 0.72 + 0.28 * sin(time * 0.78 + pattern.z * 9.0);
        color += SecondaryColor * pattern.z * pattern.z * pulse * 0.34 * Intensity;
    } else if (RiftTier == 4) {
        color *= 0.58;
        color += PrimaryColor * pattern.y * 0.08;
    } else if (RiftTier == 5) {
        float flash = 0.82 + 0.18 * sin(time * 1.7 + pattern.y * 12.0);
        color += mix(PrimaryColor, SecondaryColor, pattern.x) * pattern.y * flash * 0.24;
    }

    color = 1.0 - exp(-color * 1.35);
    fragColor = vec4(color, 1.0);
}
