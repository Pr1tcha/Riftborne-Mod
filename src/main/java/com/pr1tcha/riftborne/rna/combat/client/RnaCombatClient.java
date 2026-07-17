package com.pr1tcha.riftborne.rna.combat.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.rna.combat.RnaCombatNetwork;
import com.pr1tcha.riftborne.rna.combat.ability.RnaAbility;
import com.pr1tcha.riftborne.rna.combat.registry.RnaAbilityRegistry;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class RnaCombatClient {
    private static final Map<ResourceLocation, KeyMapping> SKILL_KEYS = new LinkedHashMap<>();
    private static float syncedLoad;
    private static float syncedCapacity = 100.0F;
    private static float barrierIntegrity;
    private static int syncedBand;
    private static int syncedCooldown;
    private static String lastAbility = "";
    private static String lastResult = "";
    private static int feedbackTicks;

    static {
        addKey(RnaAbilityRegistry.BARRIER_ID, GLFW.GLFW_KEY_X);
    }

    private RnaCombatClient() {
    }

    public static void handleSync(RnaCombatNetwork.CombatSyncPayload payload) {
        syncedLoad = payload.load();
        syncedCapacity = payload.overloadCapacity();
        barrierIntegrity = payload.barrierIntegrity();
        syncedBand = payload.bandOrdinal();
        syncedCooldown = payload.cooldownTicks();
        lastAbility = payload.abilityId();
        lastResult = payload.result();
        if (!lastResult.isBlank()) {
            feedbackTicks = 40;
        }
    }

    private static void addKey(ResourceLocation abilityId, int defaultKey) {
        SKILL_KEYS.put(abilityId, new KeyMapping(
                "key.riftborne." + abilityId.getPath(),
                InputConstants.Type.KEYSYM,
                defaultKey,
                "key.categories.riftborne"
        ));
    }

    @EventBusSubscriber(modid = Riftborne.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBusEvents {
        private ModBusEvents() {
        }

        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            SKILL_KEYS.values().forEach(event::register);
        }
    }

    @EventBusSubscriber(modid = Riftborne.MODID, value = Dist.CLIENT)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            BarrierClientState.tick();
            if (minecraft.player == null || minecraft.level == null || minecraft.getConnection() == null) {
                feedbackTicks = 0;
                return;
            }
            if (feedbackTicks > 0) {
                feedbackTicks--;
            }
            if (minecraft.screen != null) {
                return;
            }

            for (Map.Entry<ResourceLocation, KeyMapping> entry : SKILL_KEYS.entrySet()) {
                while (entry.getValue().consumeClick()) {
                    PacketDistributor.sendToServer(new RnaCombatNetwork.ActivateSkillPayload(entry.getKey().toString()));
                }
            }
        }

        @SubscribeEvent
        public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.screen != null) {
                return;
            }
            InteractionHand hand = event.getHand();
            HumanoidArm arm = hand == InteractionHand.MAIN_HAND
                    ? minecraft.player.getMainArm()
                    : minecraft.player.getMainArm().getOpposite();
            if (BarrierClientState.occupiesArm(minecraft.player.getUUID(), arm)) {
                event.setSwingHand(false);
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onRenderGui(RenderGuiEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.options.hideGui) {
                return;
            }

            GuiGraphics graphics = event.getGuiGraphics();
            int x = 8;
            int y = graphics.guiHeight() - 48;
            int width = 112;
            int loadWidth = Math.round(width * Math.max(0.0F, Math.min(100.0F, syncedLoad)) / 100.0F);
            int barColor = switch (syncedBand) {
                case 1 -> 0xFFE6C85C;
                case 2 -> 0xFFFF8A45;
                case 3 -> 0xFFFF3F5F;
                default -> 0xFF5CCFE6;
            };

            graphics.fill(x - 3, y - 4, x + width + 5, y + (barrierIntegrity > 0.0F ? 38 : 25), 0x99040A0D);
            graphics.drawString(
                    minecraft.font,
                    Component.translatable(
                            "hud.riftborne.rna_load_capacity",
                            String.format(Locale.ROOT, "%.0f", syncedCapacity)
                    ),
                    x,
                    y,
                    0xFFBCEFF7,
                    false
            );
            graphics.fill(x, y + 11, x + width, y + 17, 0xFF12191C);
            graphics.fill(x, y + 11, x + loadWidth, y + 17, barColor);
            graphics.drawString(
                    minecraft.font,
                    String.format(Locale.ROOT, "%.0f%%", syncedLoad),
                    x + width + 10,
                    y + 9,
                    0xFFE7F8FB,
                    false
            );

            if (barrierIntegrity > 0.0F) {
                graphics.drawString(
                        minecraft.font,
                        Component.translatable(
                                "hud.riftborne.barrier_integrity",
                                String.format(Locale.ROOT, "%.1f", barrierIntegrity)
                        ),
                        x,
                        y + 22,
                        0xFFBCEFF7,
                        false
                );
            }

            if (feedbackTicks > 0 && !lastAbility.isBlank()) {
                int color = "SUCCESS".equals(lastResult) ? 0xFF8FFFC1 : 0xFFFF7E7E;
                graphics.drawString(
                        minecraft.font,
                        shortAbilityName(lastAbility) + " " + shortResult(lastResult) + cooldownSuffix(),
                        x,
                        y - 12,
                        color,
                        false
                );
            }
        }
    }

    private static String shortAbilityName(String abilityId) {
        ResourceLocation id = ResourceLocation.tryParse(abilityId);
        RnaAbility ability = id == null ? null : RnaAbilityRegistry.get(id);
        if (ability != null) {
            return Component.translatable(ability.titleKey()).getString();
        }
        int slash = abilityId.indexOf(':');
        String path = slash >= 0 ? abilityId.substring(slash + 1) : abilityId;
        return path.replace("rna_", "").replace('_', ' ');
    }

    private static String shortResult(String result) {
        if (result == null || result.isBlank()) {
            return "";
        }
        if ("SUCCESS".equals(result)) {
            return "ok";
        }
        return result.toLowerCase(Locale.ROOT).replace("fail_", "");
    }

    private static String cooldownSuffix() {
        return syncedCooldown > 0 ? " [" + syncedCooldown + "t]" : "";
    }
}
