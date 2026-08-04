uniform sampler2D DiffuseSampler0;
uniform vec2 OutSize;
uniform vec4 BarrierFrame0;
uniform vec4 BarrierFrame1;
uniform float BarrierTime;

in vec2 texCoord;

out vec4 fragColor;

float roundedRectangle(vec2 point) {
    vec2 q = abs(point) - vec2(0.87, 0.82);
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - 0.13;
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(OutSize, vec2(1.0));
    vec4 base = texture(DiffuseSampler0, uv);

    vec2 center = BarrierFrame0.xy;
    vec2 axisX = BarrierFrame0.zw;
    vec2 axisY = BarrierFrame1.xy;
    float strength = BarrierFrame1.z;
    float pulse = BarrierFrame1.w;
    float determinant = axisX.x * axisY.y - axisX.y * axisY.x;
    if (strength <= 0.001 || abs(determinant) <= 0.000001) {
        fragColor = base;
        return;
    }

    vec2 delta = uv - center;
    vec2 local = vec2(
        (delta.x * axisY.y - delta.y * axisY.x) / determinant,
        (-delta.x * axisX.y + delta.y * axisX.x) / determinant
    );
    if (abs(local.x) > 1.18 || abs(local.y) > 1.18) {
        fragColor = base;
        return;
    }

    float distanceField = roundedRectangle(local);
    float body = 1.0 - smoothstep(-0.005, 0.04, distanceField);
    float edge = 1.0 - smoothstep(0.004, 0.026, abs(distanceField));
    float radial = length(local / vec2(1.0, 0.92));
    float impactRing = 1.0 - smoothstep(0.025, 0.095, abs(radial - (0.18 + (1.0 - pulse) * 0.72)));
    impactRing *= body * pulse;

    float diagonalA = abs(sin((local.x * 0.84 + local.y) * 24.0 + BarrierTime * 0.32));
    float diagonalB = abs(sin((local.x * 0.84 - local.y) * 24.0 - BarrierTime * 0.27));
    float lattice = 1.0 - smoothstep(0.035, 0.12, min(diagonalA, diagonalB));
    lattice *= body * (0.035 + pulse * 0.30);

    vec2 screenX = normalize(axisX + vec2(0.000001));
    vec2 screenY = normalize(axisY + vec2(0.000001));
    float wave = sin(local.y * 19.0 + BarrierTime * 2.6)
            + sin(local.x * 13.0 - BarrierTime * 1.9) * 0.55;
    vec2 offset = (screenX * wave + screenY * sin(radial * 17.0 - BarrierTime * 3.1) * 0.42)
            * body * strength * (0.0013 + pulse * 0.0034);

    vec4 warped = texture(DiffuseSampler0, clamp(uv + offset, vec2(0.001), vec2(0.999)));
    vec4 cyan = texture(DiffuseSampler0, clamp(uv + offset * 1.38 + screenX * 0.0012 * body, vec2(0.001), vec2(0.999)));
    vec4 blue = texture(DiffuseSampler0, clamp(uv - offset * 0.82 - screenX * 0.0008 * body, vec2(0.001), vec2(0.999)));
    warped.r = mix(warped.r, blue.b * 0.28, body * strength * 0.18);
    warped.g = mix(warped.g, cyan.g, body * strength * 0.14);
    warped.b = mix(warped.b, cyan.b, body * strength * 0.22);

    float fieldMix = body * min(0.07 + strength * 0.30, 0.42);
    vec3 color = mix(base.rgb, warped.rgb, fieldMix);
    color += vec3(0.035, 0.24, 0.46) * body * strength * 0.032;
    color += vec3(0.14, 0.56, 0.82) * edge * (0.035 + strength * 0.095);
    color += vec3(0.12, 0.50, 0.76) * lattice * strength * 0.055;
    color += vec3(0.58, 0.88, 1.00) * impactRing * (0.16 + strength * 0.38);
    fragColor = vec4(color, base.a);
}
