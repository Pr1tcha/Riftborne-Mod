package com.pr1tcha.riftborne.codex.block;

import com.pr1tcha.riftborne.registry.ModContent;
import com.pr1tcha.riftborne.codex.data.CodexData;
import com.pr1tcha.riftborne.player.RiftbornePlayerData;
import com.pr1tcha.riftborne.rna.RnaApi;
import com.pr1tcha.riftborne.rna.data.RnaData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.UUID;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class CodexDiagnosticCapsuleBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final int SCAN_DURATION_TICKS = 80;
    private static final int RELEASE_COOLDOWN_TICKS = 60;
    private static final RawAnimation IDLE = RawAnimation.begin()
            .thenLoop("animation.codex_diagnostic_capsule.idle");
    private static final RawAnimation SCANNING = RawAnimation.begin()
            .thenLoop("animation.codex_diagnostic_capsule.scanning");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private UUID occupantId;
    private UUID releasedOccupantId;
    private int scanTicks;
    private int releaseCooldownTicks;
    private String subjectName = "";
    private boolean hasRna;
    private int nodeDensity;
    private int connectivity;
    private int throughput;
    private int overloadResistance;
    private int metaWear;
    private String metaWearStage = "STABLE";
    private String formationPath = "UNKNOWN";
    private String techniqueNotice = "";

    public CodexDiagnosticCapsuleBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.CODEX_DIAGNOSTIC_CAPSULE_BE_TYPE.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CodexDiagnosticCapsuleBlockEntity capsule
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        capsule.tickReleaseCooldown();

        if (!capsule.isScanning()) {
            for (ServerPlayer player : serverLevel.getEntitiesOfClass(
                    ServerPlayer.class,
                    scanBox(pos),
                    player -> !player.isSpectator()
            )) {
                if (capsule.isReleaseCoolingDown(player)) {
                    continue;
                }
                capsule.startScan(player);
                break;
            }
            return;
        }

        ServerPlayer occupant = null;
        if (capsule.occupantId != null && serverLevel.getPlayerByUUID(capsule.occupantId) instanceof ServerPlayer player) {
            occupant = player;
        }
        if (occupant == null || !occupant.isAlive() || !scanBox(pos).inflate(0.45D).intersects(occupant.getBoundingBox())) {
            capsule.cancelScan();
            return;
        }

        capsule.holdOccupant(occupant);
        capsule.scanTicks--;
        if (capsule.scanTicks <= 0) {
            capsule.finishScan(occupant);
        }
    }

    public void startScan(ServerPlayer player) {
        if (isScanning()) {
            return;
        }
        occupantId = player.getUUID();
        scanTicks = SCAN_DURATION_TICKS;
        holdOccupant(player);
        player.displayClientMessage(
                Component.translatable("message.riftborne.codex_diagnostic_capsule.scanning"),
                true
        );
        sync();
    }

    public boolean isScanning() {
        return scanTicks > 0;
    }

    private void finishScan(ServerPlayer player) {
        capture(player);
        techniqueNotice = "";
        CodexData codex = RiftbornePlayerData.getCodex(player);
        codex.addTranslatedRecentData("codex.riftborne.feed.diagnostic_snapshot", subjectName);
        RiftbornePlayerData.saveCodex(player, codex);
        occupantId = null;
        scanTicks = 0;
        releasedOccupantId = player.getUUID();
        releaseCooldownTicks = RELEASE_COOLDOWN_TICKS;
        releaseOccupant(player);
        player.displayClientMessage(
                Component.translatable("message.riftborne.codex_diagnostic_capsule.captured"),
                true
        );
        sync();
    }

    private void cancelScan() {
        occupantId = null;
        scanTicks = 0;
        sync();
    }

    private void tickReleaseCooldown() {
        if (releaseCooldownTicks <= 0) {
            releasedOccupantId = null;
            return;
        }
        releaseCooldownTicks--;
        if (releaseCooldownTicks <= 0) {
            releasedOccupantId = null;
        }
    }

    private boolean isReleaseCoolingDown(ServerPlayer player) {
        return releaseCooldownTicks > 0 && player.getUUID().equals(releasedOccupantId);
    }

    private void holdOccupant(ServerPlayer player) {
        Vec3 center = Vec3.atBottomCenterOf(worldPosition).add(0.0D, 0.13D, 0.0D);
        player.teleportTo(center.x(), center.y(), center.z());
        player.setDeltaMovement(Vec3.ZERO);
    }

    private void releaseOccupant(ServerPlayer player) {
        Vec3 base = Vec3.atBottomCenterOf(worldPosition).add(0.0D, 0.13D, 0.0D);
        Direction facing = getBlockState().hasProperty(CodexDiagnosticCapsuleBlock.FACING)
                ? getBlockState().getValue(CodexDiagnosticCapsuleBlock.FACING)
                : Direction.NORTH;
        Vec3 exit = base.add(facing.getStepX() * 0.75D, 0.0D, facing.getStepZ() * 0.75D);
        Vec3[] exits = {
                exit,
                base.add(facing.getClockWise().getStepX() * 0.75D, 0.0D, facing.getClockWise().getStepZ() * 0.75D),
                base.add(facing.getCounterClockWise().getStepX() * 0.75D, 0.0D, facing.getCounterClockWise().getStepZ() * 0.75D),
                base.add(facing.getOpposite().getStepX() * 0.75D, 0.0D, facing.getOpposite().getStepZ() * 0.75D)
        };
        for (Vec3 candidate : exits) {
            if (canMovePlayerTo(player, candidate)) {
                exit = candidate;
                break;
            }
        }
        player.teleportTo(exit.x(), exit.y(), exit.z());
        player.setDeltaMovement(Vec3.ZERO);
    }

    private boolean canMovePlayerTo(ServerPlayer player, Vec3 target) {
        AABB movedBox = player.getBoundingBox().move(target.subtract(player.position()));
        return player.level().noCollision(player, movedBox);
    }

    private void capture(ServerPlayer player) {
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

    public int scanProgress() {
        return isScanning() ? SCAN_DURATION_TICKS - scanTicks : 0;
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

    public String techniqueNotice() {
        return techniqueNotice;
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
        tag.putString("TechniqueNotice", techniqueNotice);
        tag.putInt("ScanTicks", isScanning() ? scanTicks : 0);
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
        techniqueNotice = tag.getString("TechniqueNotice");
        scanTicks = tag.getInt("ScanTicks");
        if (scanTicks <= 0) {
            occupantId = null;
        }
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
        controllers.add(new AnimationController<>(this, "capsule", 0, state ->
                state.setAndContinue(isScanning() ? SCANNING : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    private static AABB scanBox(BlockPos pos) {
        return new AABB(
                pos.getX() + 0.18D,
                pos.getY() + 0.05D,
                pos.getZ() + 0.18D,
                pos.getX() + 0.82D,
                pos.getY() + 1.95D,
                pos.getZ() + 0.82D
        );
    }
}
