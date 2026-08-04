package com.pr1tcha.riftborne.rna.power.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.rna.power.network.PowerNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client wiring for the power system: two keys (cast the armed primitive, open the radial picker),
 * the per-tick feedback timer, and the HUD hook.
 */
public final class PowerClient {
    private static final KeyMapping CAST = new KeyMapping(
            "key.riftborne.power_cast",
            InputConstants.Type.KEYSYM,
            org.lwjgl.glfw.GLFW.GLFW_KEY_R,
            "key.categories.riftborne"
    );
    private static final KeyMapping SELECT = new KeyMapping(
            "key.riftborne.power_select",
            InputConstants.Type.KEYSYM,
            org.lwjgl.glfw.GLFW.GLFW_KEY_C,
            "key.categories.riftborne"
    );

    private PowerClient() {
    }

    @EventBusSubscriber(modid = Riftborne.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBusEvents {
        private ModBusEvents() {
        }

        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(CAST);
            event.register(SELECT);
        }
    }

    @EventBusSubscriber(modid = Riftborne.MODID, value = Dist.CLIENT)
    public static final class GameBusEvents {
        private GameBusEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.level == null) {
                PowerClientState.clear();
                return;
            }
            PowerClientState.tick();
            if (minecraft.screen != null) {
                return;
            }

            while (SELECT.consumeClick()) {
                minecraft.setScreen(new PowerSelectorScreen());
            }
            while (CAST.consumeClick()) {
                PacketDistributor.sendToServer(
                        new PowerNetwork.CastRequestPayload(PowerClientState.selected().id()));
            }
        }

        @SubscribeEvent
        public static void onRenderGui(RenderGuiEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.options.hideGui) {
                return;
            }
            PowerHud.render(event.getGuiGraphics(), minecraft);
        }
    }
}
