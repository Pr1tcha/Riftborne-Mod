package com.pr1tcha.riftborne.riftwalker.item;

import com.pr1tcha.riftborne.riftwalker.client.RiftwalkerArmorRenderer;
import java.util.function.Consumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class RiftwalkerArmorItem extends ArmorItem implements GeoItem {
    private static final String LIGHTS_KEY = "RiftwalkerMaskLights";
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public RiftwalkerArmorItem(Type type, Properties properties) {
        super(ArmorMaterials.NETHERITE, type, properties);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private RiftwalkerArmorRenderer renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(
                    T livingEntity,
                    ItemStack itemStack,
                    EquipmentSlot equipmentSlot,
                    HumanoidModel<T> original
            ) {
                if (renderer == null) {
                    renderer = new RiftwalkerArmorRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // The suit follows the vanilla humanoid rig. Cloth motion is deliberately
        // restrained so the silhouette remains Minecraft-like.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    public static boolean maskLightsEnabled(ItemStack stack) {
        if (!(stack.getItem() instanceof RiftwalkerArmorItem armor) || armor.getType() != Type.HELMET) {
            return false;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return !tag.contains(LIGHTS_KEY) || tag.getBoolean(LIGHTS_KEY);
    }

    public static boolean toggleMaskLights(ItemStack stack) {
        boolean enabled = !maskLightsEnabled(stack);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(LIGHTS_KEY, enabled);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return enabled;
    }
}
