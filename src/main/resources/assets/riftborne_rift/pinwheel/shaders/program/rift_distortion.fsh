uniform sampler2D DiffuseSampler0;
uniform vec2 OutSize;
uniform vec4 RiftParams;
uniform float RiftStrength;
uniform float RiftTime;

in vec2 texCoord;

out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(OutSize, vec2(1.0));
    vec4 base = texture(DiffuseSampler0, uv);

    vec2 minBounds = RiftParams.xy;
    vec2 maxBounds = RiftParams.zw;
    vec2 boundsSize = max(maxBounds - minBounds, vec2(0.0001));
    float strength = RiftStrength;
    if (strength <= 0.001 || maxBounds.x <= minBounds.x || maxBounds.y <= minBounds.y) {
        fragColor = base;
        return;
    }

    vec2 local = (uv - minBounds) / boundsSize;
    if (local.x < -0.08 || local.x > 1.08 || local.y < -0.04 || local.y > 1.04) {
        fragColor = base;
        return;
    }

    float verticalFade = smoothstep(0.0, 0.09, local.y) * (1.0 - smoothstep(0.91, 1.0, local.y));
    float body = sin(clamp(local.y, 0.0, 1.0) * 3.14159265);
    float centerNoise = noise(vec2(local.y * 7.0 + RiftTime * 0.12, RiftTime * 0.18)) - 0.5;
    float centerWave = sin(local.y * 18.0 + RiftTime * 1.7) * 0.035;
    float tearCenter = 0.5 + centerNoise * 0.12 + centerWave;
    float tearNoise = noise(vec2(local.y * 24.0 - RiftTime * 0.35, local.x * 6.0 + RiftTime * 0.2));
    float tearWidth = 0.1 + body * 0.26 + (tearNoise - 0.5) * 0.05;
    float edge = abs(local.x - tearCenter);
    float outerMask = 1.0 - smoothstep(tearWidth + 0.04, tearWidth + 0.23, edge);
    float innerCutout = 1.0 - smoothstep(tearWidth * 0.72, tearWidth + 0.035, edge);
    float mask = outerMask * (1.0 - innerCutout) * verticalFade;

    if (mask <= 0.001) {
        fragColor = base;
        return;
    }

    float wave = sin(local.y * 82.0 + RiftTime * 4.0 + tearNoise * 4.4);
    vec2 offset = vec2(wave * 0.0065, (tearNoise - 0.5) * 0.0035) * strength * mask;
    offset += vec2(local.x - tearCenter, 0.0) * (tearNoise - 0.5) * 0.009 * strength * mask;

    vec2 warpedUv = clamp(uv + offset, vec2(0.001), vec2(0.999));
    vec4 warped = texture(DiffuseSampler0, warpedUv);
    vec4 cyan = texture(DiffuseSampler0, clamp(uv + offset * 1.35 + vec2(0.001, -0.0006) * mask * strength, vec2(0.001), vec2(0.999)));
    vec4 violet = texture(DiffuseSampler0, clamp(uv - offset * 0.9 + vec2(-0.001, 0.0008) * mask * strength, vec2(0.001), vec2(0.999)));

    warped.r = violet.r;
    warped.b = cyan.b;
    warped.rgb += vec3(0.035, 0.015, 0.065) * mask * strength;
    fragColor = mix(base, warped, mask * min(strength * 1.35, 1.0));
}
