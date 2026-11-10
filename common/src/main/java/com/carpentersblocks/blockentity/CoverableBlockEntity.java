package com.carpentersblocks.blockentity;

import com.carpentersblocks.CarpentersBlocks;
import com.carpentersblocks.block.CoverableBlock;
import com.carpentersblocks.core.attribute.AttributeKey;
import com.carpentersblocks.core.attribute.AttributeLocation;
import com.carpentersblocks.core.attribute.AttributeType;
import com.carpentersblocks.core.attribute.AttributeView;
import com.carpentersblocks.registry.CBContent;
import com.carpentersblocks.util.BlockFlags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds the decorations of a coverable block: cover/dye/overlay/illuminator items and
 * chisel design names, keyed by {@link AttributeKey}.
 * <p>
 * Subclasses exist per loader only to send updates to clients ({@link #syncToClients()}).
 */
public abstract class CoverableBlockEntity extends BlockEntity implements AttributeView, net.minecraft.world.Clearable {
    private static final int MAX_DESIGN_LENGTH = 64;
    /** Bump when the saved layout changes, and migrate in {@link #readAttributes}. */
    private static final int FORMAT_VERSION = 1;
    private static final String TAG_VERSION = "cbVersion";
    private static final String TAG_ITEMS = "cbItems";
    private static final String TAG_DESIGNS = "cbDesigns";
    private static final String TAG_SHAPE = "cbShape";

    private final Map<AttributeKey, ItemStack> items = new LinkedHashMap<>();
    private final Map<AttributeKey, String> designs = new LinkedHashMap<>();
    /** Rebuilt on every change; read by chunk-building threads. */
    private volatile RenderData renderData = RenderData.EMPTY;
    /** Extra shape data for blocks whose shape does not fit in block states (collapsible corners). */
    private int shapeData;

    protected CoverableBlockEntity() {
        super(CBContent.COVERABLE_BLOCK_ENTITY.get());
    }

    // ---- attribute access -------------------------------------------------------------

    @Override
    public boolean has(AttributeKey key) {
        return key.type == AttributeType.DESIGN ? designs.containsKey(key) : items.containsKey(key);
    }

    /** @return a copy of the stored item, or {@link ItemStack#EMPTY}. */
    public ItemStack getItem(AttributeKey key) {
        ItemStack stack = items.get(key);
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }

    public String getDesign(AttributeLocation location) {
        return designs.getOrDefault(AttributeKey.of(AttributeType.DESIGN, location), "");
    }

    public int getShapeData() {
        return shapeData;
    }

    /** Sets the extra shape data and syncs it to clients (server side). */
    public void setShapeData(int data) {
        if (data != shapeData) {
            shapeData = data;
            onAttributesChanged();
        }
    }

    /** Snapshot for rendering. Safe to call from any thread. */
    public RenderData getRenderData() {
        return renderData;
    }

    public Map<AttributeKey, ItemStack> getItems() {
        return Collections.unmodifiableMap(items);
    }

    /** Stores one item of {@code stack} in the slot. Server side only. */
    public void putItem(AttributeKey key, ItemStack stack) {
        if (key.type == AttributeType.DESIGN) {
            throw new IllegalArgumentException("Designs are not items: " + key);
        }
        ItemStack single = stack.copy();
        single.setCount(1);
        items.put(key, single);
        onAttributesChanged();
    }

    public void putDesign(AttributeLocation location, String design) {
        designs.put(AttributeKey.of(AttributeType.DESIGN, location), design);
        onAttributesChanged();
    }

    /** Clears the slots and drops their items next to the block. Server side only. */
    public void removeAndDrop(Iterable<AttributeKey> keys) {
        boolean changed = false;
        for (AttributeKey key : keys) {
            if (key.type == AttributeType.DESIGN) {
                changed |= designs.remove(key) != null;
            } else {
                ItemStack removed = items.remove(key);
                if (removed != null) {
                    drop(removed);
                    changed = true;
                }
            }
        }
        if (changed) {
            onAttributesChanged();
        }
    }

    /** Empties the block without dropping anything ({@code /clone ... move} and similar). */
    @Override
    public void clearContent() {
        items.clear();
        designs.clear();
        renderData = RenderData.of(items, shapeData);
    }

    /** Drops every stored item. Called when the block is removed. */
    public void dropAttributes() {
        if (level == null || level.isClientSide) {
            return;
        }
        for (ItemStack stack : items.values()) {
            drop(stack);
        }
        items.clear();
        designs.clear();
        renderData = RenderData.of(items, shapeData);
    }

    private void drop(ItemStack stack) {
        if (level != null && !level.isClientSide && !stack.isEmpty()) {
            BlockPos pos = getBlockPos();
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack.copy());
        }
    }

    private void onAttributesChanged() {
        renderData = RenderData.of(items, shapeData);
        setChanged();
        if (level != null && !level.isClientSide) {
            updateLight();
            syncToClients();
        }
    }

    // ---- light ------------------------------------------------------------------------

    /** Light from the illuminator (full) or from a glowing host cover such as glowstone. */
    public int computeLight() {
        if (has(AttributeType.ILLUMINATOR, AttributeLocation.HOST)) {
            return 15;
        }
        ItemStack cover = items.get(AttributeKey.of(AttributeType.COVER, AttributeLocation.HOST));
        if (cover != null && cover.getItem() instanceof BlockItem) {
            return ((BlockItem) cover.getItem()).getBlock().defaultBlockState().getLightEmission();
        }
        return 0;
    }

    /**
     * Every visible cover is an opaque full block (glass, leaves and the bare frame are not):
     * the host cover, and any side cover drawn over it.
     */
    public boolean hasOpaqueCover() {
        if (!isOpaque(items.get(AttributeKey.of(AttributeType.COVER, AttributeLocation.HOST)))) {
            return false;
        }
        for (Map.Entry<AttributeKey, ItemStack> entry : items.entrySet()) {
            if (entry.getKey().type == AttributeType.COVER && !isOpaque(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static boolean isOpaque(ItemStack cover) {
        if (cover == null || !(cover.getItem() instanceof BlockItem)) {
            return false;
        }
        BlockState coverState = ((BlockItem) cover.getItem()).getBlock().defaultBlockState();
        try {
            return coverState.canOcclude() && coverState.isSolidRender(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        } catch (RuntimeException e) {
            return false; // a modded block that needs its own world position
        }
    }

    private void updateLight() {
        BlockState state = getBlockState();
        if (!state.hasProperty(CoverableBlock.LIGHT)) {
            return;
        }
        BlockState next = state.setValue(CoverableBlock.LIGHT, computeLight());
        if (next.hasProperty(CoverableBlock.OPAQUE)) {
            next = next.setValue(CoverableBlock.OPAQUE, hasOpaqueCover());
        }
        if (next != state) {
            level.setBlock(getBlockPos(), next, BlockFlags.ALL);
        }
    }

    // ---- saving and syncing -----------------------------------------------------------

    /** Sends the current attributes to clients tracking this block. Server side only. */
    protected abstract void syncToClients();

    /**
     * Called on the client after new data arrived, to redraw the block. Loader subclasses
     * may extend it (Forge refreshes its model data first).
     */
    protected void onClientDataReceived() {
        if (level != null && level.isClientSide) {
            BlockState state = getBlockState();
            // Not "re-render now": that rebuilds the section on the main thread, and this also
            // runs for every block of a chunk that has just arrived.
            level.sendBlockUpdated(getBlockPos(), state, state, BlockFlags.CLIENTS);
        }
    }

    public CompoundTag writeAttributes(CompoundTag tag) {
        return writeAttributes(tag, false);
    }

    /**
     * What clients need to draw the block: item ids only. Item NBT (names, lore, written data)
     * stays on the server, which also keeps chunk packets small.
     */
    public CompoundTag writeClientAttributes(CompoundTag tag) {
        return writeAttributes(tag, true);
    }

    private CompoundTag writeAttributes(CompoundTag tag, boolean forClient) {
        tag.putInt(TAG_VERSION, FORMAT_VERSION);
        CompoundTag itemsTag = new CompoundTag();
        items.forEach((key, stack) -> itemsTag.put(key.id(),
            (forClient ? new ItemStack(stack.getItem()) : stack).save(new CompoundTag())));
        tag.put(TAG_ITEMS, itemsTag);
        CompoundTag designsTag = new CompoundTag();
        designs.forEach((key, design) -> designsTag.putString(key.id(), design));
        tag.put(TAG_DESIGNS, designsTag);
        tag.putInt(TAG_SHAPE, shapeData);
        return tag;
    }

    public void readAttributes(CompoundTag tag) {
        items.clear();
        designs.clear();
        CompoundTag itemsTag = tag.getCompound(TAG_ITEMS);
        for (String id : itemsTag.getAllKeys()) {
            AttributeKey key = AttributeKey.parse(id);
            if (key == null || key.type == AttributeType.DESIGN) {
                CarpentersBlocks.LOGGER.warn("Ignoring unknown attribute {} at {}", id, worldPosition);
                continue;
            }
            ItemStack stack = ItemStack.of(itemsTag.getCompound(id));
            // An item from a removed mod loads as empty; skip it instead of keeping a broken slot.
            // A cover can only be a block that is not one of ours (hand-edited or crafted NBT).
            if (stack.isEmpty() || key.type == AttributeType.COVER && !(stack.getItem() instanceof BlockItem
                && !(((BlockItem) stack.getItem()).getBlock() instanceof CoverableBlock))) {
                continue;
            }
            stack.setCount(1); // a slot holds one item, whatever the data says
            items.put(key, stack);
        }
        shapeData = tag.getInt(TAG_SHAPE);
        CompoundTag designsTag = tag.getCompound(TAG_DESIGNS);
        for (String id : designsTag.getAllKeys()) {
            AttributeKey key = AttributeKey.parse(id);
            if (key != null && key.type == AttributeType.DESIGN) {
                String design = designsTag.getString(id);
                designs.put(key, design.length() > MAX_DESIGN_LENGTH ? design.substring(0, MAX_DESIGN_LENGTH) : design);
            }
        }
        renderData = RenderData.of(items, shapeData);
    }

    @Override
    public void load(BlockState state, CompoundTag tag) {
        super.load(state, tag);
        readAttributes(tag);
        if (level != null && !level.isClientSide) {
            // Data applied to a block already in the world (an item with BlockEntityTag, /data).
            updateLight();
            syncToClients();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        super.save(tag);
        return writeAttributes(tag);
    }

    /** Full data sent with the chunk when a client starts tracking it. */
    @Override
    public CompoundTag getUpdateTag() {
        return writeClientAttributes(super.save(new CompoundTag()));
    }
}
