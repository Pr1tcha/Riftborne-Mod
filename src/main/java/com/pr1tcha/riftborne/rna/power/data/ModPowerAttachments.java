package com.pr1tcha.riftborne.rna.power.data;

import com.pr1tcha.riftborne.Riftborne;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Registers the PS V2.5 power-system data attachments. The whole {@link RNAProfile} lives in one
 * attachment: serialized via Codec, copied on death (the lore cost is not "lose progress on death"),
 * and synced to the owning client for the HUD.
 */
public final class ModPowerAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Riftborne.MODID);

    public static final Supplier<AttachmentType<RNAProfile>> RNA_PROFILE = ATTACHMENT_TYPES.register(
            "rna_profile",
            () -> AttachmentType.builder(RNAProfile::empty)
                    .serialize(RNAProfile.CODEC)
                    .sync(RNAProfile.STREAM_CODEC)
                    .copyOnDeath()
                    .build()
    );

    private ModPowerAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
