package com.pr1tcha.riftborne.codex.block;

import com.pr1tcha.riftborne.registry.ModContent;
import com.pr1tcha.riftborne.rna.RnaApi;
import com.pr1tcha.riftborne.rna.data.RnaData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class CodexDiagnosticCapsuleBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation IDLE = RawAnimation.begin()
            .thenLoop("animation.codex_diagnostic_capsule.idle");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private String subjectName = "";
    private boolean hasRna;
    private int nodeDensity;
    private int connectivity;
    private int throughput;
    private int overloadResistance;
    private int metaWear;
    private String metaWearStage = "STABLE";
    private String formationPath = "UNKNOWN";

    public CodexDiagnosticCapsuleBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.CODEX_DIAGNOSTIC_CAPSULE_BE_TYPE.get(), pos, state);
    }

    public void capture(ServerPlayer player) {
        RnaData rna = RnaApi.get(player);
        subjectName = player.getGameProfile().getName();
        hasRna = rna.hasRNA();
        nodeDensity = rna.nodeDensity();
        connectivity = rna.connectivity();
        throughput = rna.throughput();
        overloadResistance = rna.overloadResistance();
        metaWear = rna.metaWear();
        metaWearStage = rna.metaWearStage().name();
        formationPath = rna.formationPath().name();
        sync();
    }

    public boolean hasDiagnosticData() {
        return !subjectName.isBlank();
    }

    public String subjectName() {
        return subjectName;
    }

    public boolean hasRna() {
        return hasRna;
    }

    public int nodeDensity() {
        return nodeDensity;
    }

    public int connectivity() {
        return connectivity;
    }

    public int throughput() {
        return throughput;
    }

    public int overloadResistance() {
        return overloadResistance;
    }

    public int metaWear() {
        return metaWear;
    }

    public String metaWearStage() {
        return metaWearStage;
    }

    public String formationPath() {
        return formationPath;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("SubjectName", subjectName);
        tag.putBoolean("HasRna", hasRna);
        tag.putInt("NodeDensity", nodeDensity);
        tag.putInt("Connectivity", connectivity);
        tag.putInt("Throughput", throughput);
        tag.putInt("OverloadResistance", overloadResistance);
        tag.putInt("MetaWear", metaWear);
        tag.putString("MetaWearStage", metaWearStage);
        tag.putString("FormationPath", formationPath);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        subjectName = tag.getString("SubjectName");
        hasRna = tag.getBoolean("HasRna");
        nodeDensity = tag.getInt("NodeDensity");
        connectivity = tag.getInt("Connectivity");
        throughput = tag.getInt("Throughput");
        overloadResistance = tag.getInt("OverloadResistance");
        metaWear = tag.getInt("MetaWear");
        metaWearStage = tag.getString("MetaWearStage").isBlank() ? "STABLE" : tag.getString("MetaWearStage");
        formationPath = tag.getString("FormationPath").isBlank() ? "UNKNOWN" : tag.getString("FormationPath");
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
