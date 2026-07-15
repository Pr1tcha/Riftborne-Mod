package com.pr1tcha.riftborne.flight;

import com.pr1tcha.riftborne.Riftborne;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = Riftborne.MODID)
public final class FlightAbility {
    private static final String ABILITY_TAG = "RiftborneFlight";
    private static final String SETTINGS_TAG = "RiftborneFlightSettings";

    private FlightAbility() {
    }

    public static boolean hasAbility(Player player) {
        return player.getPersistentData().getBoolean(ABILITY_TAG);
    }

    public static void setAbility(ServerPlayer player, boolean enabled) {
        setAbility(player, enabled, FlightSettings.defaults());
    }

    public static void setAbility(ServerPlayer player, boolean enabled, FlightSettings settings) {
        player.getPersistentData().putBoolean(ABILITY_TAG, enabled);
        if (enabled) {
            setSettings(player, settings);
        } else {
            player.getPersistentData().remove(SETTINGS_TAG);
            RiftFlightController.stopFlight(player, true);
        }
    }

    public static FlightSettings getSettings(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.contains(SETTINGS_TAG, Tag.TAG_COMPOUND)) {
            return FlightSettings.load(persistentData.getCompound(SETTINGS_TAG));
        }
        return FlightSettings.defaults();
    }

    public static void setSettings(ServerPlayer player, FlightSettings settings) {
        player.getPersistentData().put(SETTINGS_TAG, settings.sanitize().save());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        CompoundTag originalData = event.getOriginal().getPersistentData();
        CompoundTag clonedData = event.getEntity().getPersistentData();
        if (originalData.getBoolean(ABILITY_TAG)) {
            clonedData.putBoolean(ABILITY_TAG, true);
            if (originalData.contains(SETTINGS_TAG, Tag.TAG_COMPOUND)) {
                clonedData.put(SETTINGS_TAG, originalData.getCompound(SETTINGS_TAG).copy());
            }
        }
    }
}
