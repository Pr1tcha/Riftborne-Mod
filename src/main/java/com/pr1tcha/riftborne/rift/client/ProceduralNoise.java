package com.pr1tcha.riftborne.rift.client;

import com.pr1tcha.riftborne.Riftborne;
import net.minecraft.util.Mth;

final class ProceduralNoise {
    private ProceduralNoise() {
    }

    static float fbm(long seed, float x, float y, int octaves, float lacunarity, float gain) {
        float value = 0.0F;
        float amplitude = 0.5F;
        float frequency = 1.0F;
        float norm = 0.0F;

        for (int i = 0; i < octaves; i++) {
            value += value(seed + i * 0x9E3779B97F4A7C15L, x * frequency, y * frequency) * amplitude;
            norm += amplitude;
            frequency *= lacunarity;
            amplitude *= gain;
        }

        return norm <= 0.0F ? 0.0F : value / norm;
    }

    static float ridged(long seed, float x, float y, int octaves, float lacunarity, float gain) {
        float value = 0.0F;
        float amplitude = 0.55F;
        float frequency = 1.0F;
        float norm = 0.0F;

        for (int i = 0; i < octaves; i++) {
            float sample = value(seed + i * 0xD1B54A32D192ED03L, x * frequency, y * frequency);
            value += (1.0F - Math.abs(sample)) * 2.0F * amplitude;
            norm += amplitude;
            frequency *= lacunarity;
            amplitude *= gain;
        }

        return norm <= 0.0F ? 0.0F : Mth.clamp(value / norm - 1.0F, -1.0F, 1.0F);
    }

    static Warp warp(long seed, float x, float y, float amount) {
        float wx = fbm(seed + 101L, x + 17.31F, y - 9.47F, 3, 2.05F, 0.5F) * amount;
        float wy = fbm(seed + 211L, x - 5.83F, y + 23.19F, 3, 2.05F, 0.5F) * amount;
        return new Warp(x + wx, y + wy);
    }

    private static float value(long seed, float x, float y) {
        int x0 = Mth.floor(x);
        int y0 = Mth.floor(y);
        int x1 = x0 + 1;
        int y1 = y0 + 1;
        float tx = smooth(x - x0);
        float ty = smooth(y - y0);

        float a = random(seed, x0, y0);
        float b = random(seed, x1, y0);
        float c = random(seed, x0, y1);
        float d = random(seed, x1, y1);
        float low = Mth.lerp(tx, a, b);
        float high = Mth.lerp(tx, c, d);
        return Mth.lerp(ty, low, high);
    }

    private static float smooth(float t) {
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    private static float random(long seed, int x, int y) {
        long value = seed;
        value ^= x * 0x632BE59BD9B4E019L;
        value ^= y * 0x9E3779B97F4A7C15L;
        value ^= value >>> 27;
        value *= 0x3C79AC492BA7B653L;
        value ^= value >>> 33;
        value *= 0x1C69B3F74AC4AE35L;
        value ^= value >>> 27;
        return ((value >>> 40) / (float) 0xFFFFFF) * 2.0F - 1.0F;
    }

    record Warp(float x, float y) {
    }
}
