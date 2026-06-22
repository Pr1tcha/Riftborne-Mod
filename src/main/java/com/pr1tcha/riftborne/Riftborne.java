package com.pr1tcha.riftborne;

import com.mojang.logging.LogUtils;
import com.pr1tcha.riftborne.command.RiftborneCommands;
import com.pr1tcha.riftborne.config.Config;
import com.pr1tcha.riftborne.registry.ModContent;
import com.pr1tcha.riftborne.rift.client.RiftBlockEntityRenderer;
import com.pr1tcha.riftborne.rift.client.RiftSkyEffects;
import com.pr1tcha.riftborne.rift.client.RiftSplinterRenderer;
import com.pr1tcha.riftborne.rift.client.VeilRiftDistortion;
import com.pr1tcha.riftborne.rift.RiftWorldStage;
import com.pr1tcha.riftborne.aspects.telekinesis.client.TelekineticBlockRenderer;
import com.pr1tcha.riftborne.aspects.telekinesis.TelekinesisNetwork;
import com.pr1tcha.riftborne.codex.network.CodexNetwork;
import com.pr1tcha.riftborne.codex.client.CodexLaptopRenderer;
import com.pr1tcha.riftborne.codex.client.CodexDockRenderer;
import com.pr1tcha.riftborne.client.model.RiftborneBakedModelFactory;
import com.pr1tcha.riftborne.interspace.RnaFluidClient;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;
import software.bernie.geckolib.util.GeckoLibUtil;

@Mod(Riftborne.MODID)
public class Riftborne {
    public static final String MODID = "riftborne";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Riftborne(IEventBus modEventBus, ModContainer modContainer) {
        GeckoLibUtil.addCustomBakedModelFactory(MODID, new RiftborneBakedModelFactory());
        RiftWorldStage.init();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ModContent::registerEntityAttributes);
        modEventBus.addListener(TelekinesisNetwork::register);
        modEventBus.addListener(CodexNetwork::register);

        NeoForge.EVENT_BUS.register(this);
        ModContent.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Riftborne common setup complete");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        RiftborneCommands.register(event.getServer().getCommands().getDispatcher());
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("Riftborne client setup for {}", Minecraft.getInstance().getUser().getName());
            event.enqueueWork(() -> {
                VeilRiftDistortion.registerIfPresent();
                RnaFluidClient.registerRenderLayers();
            });
        }

        @SubscribeEvent
        public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
            RnaFluidClient.registerExtensions(event);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModContent.RIFT_BE_TYPE.get(), RiftBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(ModContent.CODEX_LAPTOP_BE_TYPE.get(), context -> new CodexLaptopRenderer());
            event.registerBlockEntityRenderer(ModContent.CODEX_DOCK_BE_TYPE.get(), context -> new CodexDockRenderer());
            event.registerEntityRenderer(ModContent.RIFT_SPLINTER.get(), RiftSplinterRenderer::new);
            event.registerEntityRenderer(ModContent.TELEKINETIC_BLOCK.get(), TelekineticBlockRenderer::new);
        }

        @SubscribeEvent
        public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
            RiftSkyEffects.register(event);
        }
    }
}
