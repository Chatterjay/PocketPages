package infiniteinvo.inventory;

import infiniteinvo.Config;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Persistent player attachment. Serialization runs with player saves, not each slot mutation. */
public final class InfiniteInventoryState implements INBTSerializable<CompoundTag> {
    private static final String ITEMS = "Items";
    private static final String UNLOCKED = "UnlockedSlots";
    private final List<ItemStack> items = new ArrayList<>();
    private int unlockedSlots;
    private boolean initialized;

    public InfiniteInventoryState() {
        ensureSize();
    }

    public int size() {
        ensureSize();
        return Config.totalExtraSlots();
    }

    public ItemStack getItem(int slot) {
        ensureSize();
        return slot >= 0 && slot < size() ? items.get(slot) : ItemStack.EMPTY;
    }

    public void setItem(int slot, ItemStack stack) {
        ensureSize();
        if (slot >= 0 && slot < size()) {
            items.set(slot, stack.copy());
        }
    }

    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = getItem(slot);
        if (stack.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) {
            items.set(slot, ItemStack.EMPTY);
        }
        return removed;
    }

    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= size()) {
            return ItemStack.EMPTY;
        }
        ItemStack previous = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        return previous;
    }

    public int getUnlockedSlots() {
        return Math.min(Math.max(Config.startUnlockedSlots(), unlockedSlots), size());
    }

    public void setUnlockedSlots(int unlockedSlots) {
        this.unlockedSlots = Math.max(0, Math.min(unlockedSlots, size()));
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void importLegacy(CompoundTag legacy, HolderLookup.Provider registries) {
        unlockedSlots = legacy.getInt(UNLOCKED);
        ListTag entries = legacy.getList(ITEMS, Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            int slot = entry.getInt("Slot");
            if (slot >= 0) {
                ensureStorage(slot + 1);
                items.set(slot, ItemStack.parseOptional(registries, entry.getCompound("Stack")));
            }
        }
        initialized = true;
    }

    public InfiniteInventoryState copy() {
        InfiniteInventoryState copy = new InfiniteInventoryState();
        copy.unlockedSlots = unlockedSlots;
        copy.initialized = initialized;
        copy.ensureStorage(items.size());
        for (int i = 0; i < items.size(); i++) {
            copy.items.set(i, items.get(i).copy());
        }
        return copy;
    }

    private void ensureSize() {
        ensureStorage(Config.totalExtraSlots());
    }

    private void ensureStorage(int requiredSize) {
        while (items.size() < requiredSize) {
            items.add(ItemStack.EMPTY);
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(UNLOCKED, unlockedSlots);
        ListTag entries = new ListTag();
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty()) {
                CompoundTag entry = new CompoundTag();
                entry.putInt("Slot", slot);
                entry.put("Stack", stack.saveOptional(registries));
                entries.add(entry);
            }
        }
        tag.put(ITEMS, entries);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
        importLegacy(tag, registries);
    }
}
