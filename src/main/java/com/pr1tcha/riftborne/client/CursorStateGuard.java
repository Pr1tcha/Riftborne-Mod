package com.pr1tcha.riftborne.client;

import com.pr1tcha.riftborne.Riftborne;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

/** Keeps the native cursor released while any Minecraft screen is open. */
@EventBusSubscriber(modid = Riftborne.MODID, value = Dist.CLIENT)
public final class CursorStateGuard {
    private CursorStateGuard() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null || !minecraft.isWindowActive()) {
            return;
        }

        long window = minecraft.getWindow().getWindow();
        if (GLFW.glfwGetInputMode(window, GLFW.GLFW_CURSOR) != GLFW.GLFW_CURSOR_NORMAL) {
            minecraft.mouseHandler.releaseMouse();
        }
    }
}
