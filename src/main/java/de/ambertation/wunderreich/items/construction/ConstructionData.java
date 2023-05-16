package de.ambertation.wunderreich.items.construction;

import de.ambertation.wunderlib.math.Bounds;
import de.ambertation.wunderlib.math.Float3;
import de.ambertation.wunderlib.math.Matrix4;
import de.ambertation.wunderlib.math.Transform;
import de.ambertation.wunderlib.math.sdf.SDF;
import de.ambertation.wunderlib.math.sdf.interfaces.MaterialProvider;
import de.ambertation.wunderlib.math.sdf.interfaces.Transformable;
import de.ambertation.wunderreich.gui.construction.RulerContainer;
import de.ambertation.wunderreich.gui.overlay.TransformWidget;
import de.ambertation.wunderreich.network.ChangedTargetBlockMessage;
import de.ambertation.wunderreich.noise.OpenSimplex2;
import de.ambertation.wunderreich.utils.RandomList;
import de.ambertation.wunderreich.utils.nbt.CachedNBTValue;
import de.ambertation.wunderreich.utils.nbt.NbtTagHelper;

import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jetbrains.annotations.ApiStatus;

public class ConstructionData {
    private static final String SDF_TAG = "sdf";
    private static final String CENTER_TAG = "c";
    private static final String SELECTED_CORNER_TAG = "sc";
    private static final String MATERIAL_TAG = "m";
    private static final String ACTIVE_SLOT_TAG = "a";
    private static final String TRANSFORM_STATE_TAG = "ts";
    private static final int VALID_RADIUS_SQUARE = 64 * 64;
    private static final String CONSTRUCTION_DATA_TAG = "construction";

    public final CachedNBTValue<Float3, CompoundTag> CENTER;
    public final CachedNBTValue<Bounds.Interpolate, ByteTag> SELECTED_CORNER;

    public final CachedNBTValue<SDF, Tag> SDF_DATA;
    public final CachedNBTValue<Integer, IntTag> ACTIVE_SLOT;

    public final CachedNBTValue<RulerContainer, ListTag> MATERIAL_DATA;
    @ApiStatus.Internal
    private static Float3 lastTarget;


    public ConstructionData(CompoundTag baseTag) {
        CENTER = new CachedNBTValue<>(
                baseTag,
                CENTER_TAG,
                Float3.ZERO,
                NbtTagHelper::readPos,
                NbtTagHelper::writePos
        );
        SELECTED_CORNER = new CachedNBTValue<>(
                baseTag,
                SELECTED_CORNER_TAG,
                NbtTagHelper::readInterpolated,
                NbtTagHelper::writeInterpolated
        );
        SDF_DATA = new CachedNBTValue<>(
                baseTag,
                SDF_TAG,
                NbtTagHelper::readSDF,
                NbtTagHelper::writeSDF,
                this::sdfObjectDidChange
        );
        MATERIAL_DATA = new CachedNBTValue<>(
                baseTag,
                MATERIAL_TAG,
                (tag) -> {
                    RulerContainer rc = new RulerContainer();
                    NbtTagHelper.readContainer(tag, rc);
                    return rc;
                },
                NbtTagHelper::writeContainer
        );
        ACTIVE_SLOT = new CachedNBTValue<>(
                baseTag,
                ACTIVE_SLOT_TAG,
                0,
                IntTag::getAsInt,
                IntTag::valueOf,
                this::changedActiveSlot
        );

        updateActiveTransformWidget();
        if (getActiveTransformWidget() != null) {
            if (!baseTag.contains(TRANSFORM_STATE_TAG, Tag.TAG_COMPOUND)) {
                baseTag.put(TRANSFORM_STATE_TAG, new CompoundTag());
            }
            getActiveTransformWidget().setDataTag(baseTag.getCompound(TRANSFORM_STATE_TAG));
            getActiveTransformWidget().readState();
        }
    }

    private TransformWidget activeTransformWidget;

    public void changedActiveSlot(int oldValue, int newValue) {
        if (oldValue != newValue)
            updateActiveTransformWidget();
    }

    public TransformWidget getActiveTransformWidget() {
        return activeTransformWidget;
    }

    public void updateActiveTransformWidget() {
        SDF sdf = getActiveSDF();

        if (sdf instanceof Transformable source) {
            sdf.setRootTransform(Matrix4.ofTranslation(CENTER.get()));
            activeTransformWidget = new TransformWidget(source);
        } else activeTransformWidget = null;
    }

    public static Float3 getCursorPos() {
        return lastTarget;
    }

    public static void setCursorPosOnClient(Float3 newTarget) {
        if (setCursorPosCommon(newTarget)) {
            ChangedTargetBlockMessage.INSTANCE.send(newTarget);
        }
    }

    public static void setCursorPosOnServer(Float3 lastTarget) {
        setCursorPosCommon(lastTarget);
    }

    private static boolean setCursorPosCommon(Float3 newTarget) {
        if (lastTarget == newTarget) return false;

        if (lastTarget == null || newTarget == null || !lastTarget.equals(newTarget)) {
            lastTarget = newTarget;

            return true;
        }

        return false;
    }

    public void sdfObjectDidChange(SDF old, SDF fresh) {
//        if (old == fresh) return;
//
//        if (old != null) old.removeChangeListener(this::sdfContentDidChange);
//        if (fresh != null) fresh.addChangeListener(this::sdfContentDidChange);
    }

    public static ConstructionData getConstructionData(ItemStack itemStack) {
        if (itemStack.getItem() instanceof Ruler) {
            CompoundTag tag = itemStack.getOrCreateTag();
            return getConstructionData(tag);
        }
        return null;
    }

    public static ConstructionData getConstructionData(CompoundTag tag) {
        if (tag == null) return null;

        if (!tag.contains(CONSTRUCTION_DATA_TAG)) {
            tag.put(CONSTRUCTION_DATA_TAG, new CompoundTag());
        }
        return new ConstructionData(tag.getCompound(CONSTRUCTION_DATA_TAG));
    }

    public SDF getRootSDF() {
        SDF s = SDF_DATA.get();
        return s;
    }

    public SDF getActiveSDF() {
        SDF s = SDF_DATA.get();
        if (s == null) return null;

        s = s.getChildWithGraphIndex(ACTIVE_SLOT.get());
        return s;
    }

    public Transform updateActiveTransformOnClient(BiFunction<SDF, Transform, Transform> updater) {
        SDF s = getActiveSDF();
        if (s instanceof Transformable tSDF) {
            Transform newTransform = updater.apply(s, tSDF.getLocalTransform());
            tSDF.setLocalTransform(newTransform);
            SDF_DATA.set(s.getRoot());
            return newTransform;
        }
        return null;
    }

    public Bounds.Interpolate getSelectedCorner() {
        return SELECTED_CORNER.get();
    }

    public void setSelectedCorner(Bounds.Interpolate bb) {
        SELECTED_CORNER.set(bb);
    }

    public Bounds getBoundingBoxInWorldSpace() {
        return getBoundingBoxInWorldSpace(getRootSDF());
    }

    public Bounds getActiveBoundingBoxInWorldSpace() {
        return getBoundingBoxInWorldSpace(getActiveSDF());
    }

    public Bounds getBoundingBoxInWorldSpace(SDF sdf) {
        if (sdf != null) {
            Float3 offset = CENTER.get();
            Bounds box = sdf.getBoundingBox();
            if (offset != null) box = box.move(offset);
            return box;
        }
        return Bounds.EMPTY;
    }

    public static Bounds getNewBoundsForSelectedCorner(
            Bounds bounds,
            Bounds.Interpolate selectedCorner,
            Float3 newCornerPos
    ) {
        if (Objects.equals(selectedCorner.idx, Bounds.Interpolate.CENTER.idx)) {
            System.out.println("New Center:" + newCornerPos);
            return bounds.moveToCenter(newCornerPos);
        }

        Bounds.Interpolate oppositeCorner = selectedCorner.opposite();
        return Bounds.of(bounds.get(oppositeCorner), newCornerPos);
    }

    public double distToCenterSquare(Float3 worldPos) {
        Bounds bb = getBoundingBoxInWorldSpace();
        if (bb == null) return Double.MAX_VALUE;
        return bb.getCenter().distSquare(worldPos);
    }

    public boolean inReach(Float3 worldPos) {
        return distToCenterSquare(worldPos) < VALID_RADIUS_SQUARE;
    }

    public void realize(MinecraftServer server, ServerPlayer player) {
        SDF sdf = getRootSDF();
        if (sdf != null) {
            sdf.setRootTransform(Matrix4.ofTranslation(CENTER.get()));


            final Function<Float3, Float>
                    noise = (cPos) -> (1 + OpenSimplex2.noise3_ImproveXZ(
                    20688,
                    cPos.x * 0.15,
                    cPos.y * 0.2,
                    cPos.z * 0.15
            )) / 2;
            //noise = (p) -> RandomList.random();

            final RandomList<ItemStack>[] materials = new RandomList[RulerContainer.MAX_CATEGORIES];
            for (int i = 0; i < materials.length; i++)
                materials[i] = new RandomList<>(RulerContainer.ITEMS_PER_CATEGORY);

            RulerContainer materialContainer = MATERIAL_DATA.get();
            for (int page = 0; page < RulerContainer.MAX_CATEGORIES; page++) {
                for (int pageIndex = 0; pageIndex < RulerContainer.ITEMS_PER_CATEGORY; pageIndex++) {
                    final int slot = materialContainer.getCategorySlotIndex(page, pageIndex)
                            + RulerContainer.CATEGORIES_SLOT_START;
                    ItemStack stack = materialContainer.getItem(slot);
                    if (stack == null || stack.isEmpty()) continue;
                    if (!(stack.getItem() instanceof BlockItem)) continue;


                    materials[page].add(stack, stack.getCount());
                }
            }

            sdf.evaluate((p, ed) -> {
                int mIdx = 0;
                if (ed.source() instanceof MaterialProvider mp) mIdx = mp.getMaterialIndex();
                ItemStack stack = materials[mIdx % materials.length].getRandomAt(p, noise);
                player.level().setBlock(
                        p.toBlockPos(),
                        ((BlockItem) (stack.getItem())).getBlock().defaultBlockState(),
                        2
                );
            }, null);
        }
    }
}
