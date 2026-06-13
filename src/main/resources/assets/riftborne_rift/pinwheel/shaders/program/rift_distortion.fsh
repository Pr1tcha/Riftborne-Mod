uniform sampler2D DiffuseSampler0;
uniform vec2 OutSize;
uniform vec4 RiftParams;
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
    vec2 uv = texCoord;
    vec4 base = texture(DiffuseSampler0, uv);

    vec2 center = RiftParams.xy;
    float radius = RiftParams.z;
    float strength = RiftParams.w;
    if (radius <= 0.001 || strength <= 0.001) {
        fragColor = base;
        return;
    }

    float aspect = OutSize.x / max(OutSize.y, 1.0);
    vec2 delta = uv - center;
    vec2 scaled = vec2(delta.x * aspect, delta.y);
    float dist = length(scaled);
    float inner = smoothstep(radius * 0.18, radius * 0.72, dist);
    float outer = 1.0 - smoothstep(radius * 0.72, radius * 1.28, dist);
    float mask = inner * outer;

    float tear = noise(vec2(delta.y * 34.0 + RiftTime * 0.55, delta.x * 18.0 - RiftTime * 0.3));
    float wave = sin(delta.y * 72.0 + RiftTime * 4.0 + tear * 3.2);
    vec2 radial = dist > 0.0001 ? scaled / dist : vec2(0.0, 1.0);
    vec2 tangent = vec2(-radial.y / aspect, radial.x);
    vec2 offset = tangent * wave * 0.0085 * strength * mask;
    offset += vec2(delta.x, delta.y) * (tear - 0.5) * 0.012 * strength * mask;

    vec2 warpedUv = clamp(uv + offset, vec2(0.001), vec2(0.999));
    vec4 warped = texture(DiffuseSampler0, warpedUv);
    vec4 cyan = texture(DiffuseSampler0, clamp(uv + offset * 1.35 + vec2(0.0015, -0.0008) * mask * strength, vec2(0.001), vec2(0.999)));
    vec4 violet = texture(DiffuseSampler0, clamp(uv - offset * 0.9 + vec2(-0.0012, 0.0011) * mask * strength, vec2(0.001), vec2(0.999)));

    warped.r = violet.r;
    warped.b = cyan.b;
    warped.rgb += vec3(0.035, 0.02, 0.06) * mask * strength;
    fragColor = mix(base, warped, mask * min(strength * 1.25, 1.0));
}
