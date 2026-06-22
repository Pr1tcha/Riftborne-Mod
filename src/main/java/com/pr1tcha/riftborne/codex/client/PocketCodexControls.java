package com.pr1tcha.riftborne.codex.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.codex.network.CodexNetwork;
import com.pr1tcha.riftborne.registry.ModContent;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class PocketCodexControls {
    private static final KeyMapping PREVIOUS = mapping("previous", GLFW.GLFW_KEY_LEFT_BRACKET);
    private static final KeyMapping NEXT = mapping("next", GLFW.GLFW_KEY_RIGHT_BRACKET);
    private static final KeyMapping ACTION = mapping("action", GLFW.GLFW_KEY_G);

    private PocketCodexControls() {
    }

    private static KeyMapping mapping(String name, int key) {
        return new KeyMapping(
                "key.riftborne.pocket_codex." + name,
                InputConstants.Type.KEYSYM,
                key,
                "key.categories.riftborne"
        );
    }

    @EventBusSubscriber(modid = Riftborne.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModEvents {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(PREVIOUS);
            event.register(NEXT);
            event.register(ACTION);
        }
    }

    @EventBusSubscriber(modid = Riftborne.MODID, value = Dist.CLIENT)
    public static final class GameEvents {
        @SubscribeEvent
        public static void clientTick(ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.screen != null || minecraft.getConnection() == null) {
                return;
            }
            if (!minecraft.player.getMainHandItem().is(ModContent.POCKET_CODEX.get())
                    && !minecraft.player.getOffhandItem().is(ModContent.POCKET_CODEX.get())) {
                return;
            }
            while (PREVIOUS.consumeClick()) {
                PacketDistributor.sendToServer(new CodexNetwork.PocketCyclePayload(-1));
            }
            while (NEXT.consumeClick()) {
                PacketDistributor.sendToServer(new CodexNetwork.PocketCyclePayload(1));
            }
            while (ACTION.consumeClick()) {
                PacketDistributor.sendToServer(new CodexNetwork.PocketActionPayload());
            }
        }
    }
}
