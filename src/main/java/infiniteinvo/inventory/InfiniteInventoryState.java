package infiniteinvo.inventory;

import infiniteinvo.Config;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.minecraft.world.entity.player.Inventory;

/** Persistent player attachment. Serialization runs with player saves, not each slot mutation. */
public final class InfiniteInventoryState implements INBTSerializable<CompoundTag> {
    private static final String ITEMS = "Items";
    private static final String UNLOCKED = "UnlockedSlots";
    private static final String PROTOTYPES = "Prototypes";
    private static final String PROTOTYPE_ID = "Prototype";
    private static final String COUNT = "Count";
    private final List<ItemStack> items = new ArrayList<>();
    private int unlockedSlots;
    private boolean initialized;
    private long revision;

    public InfiniteInventoryState() {
        ensureSize();
    }

    public int size() {
        ensureSize();
        return Config.totalExtraSlots();
    }

    /** Includes serialized slots from a previously larger configured inventory. */
    public int storedSize() {
        ensureSize();
        return items.size();
    }

    public ItemStack getItem(int slot) {
        ensureSize();
        return slot >= 0 && slot < size() ? items.get(slot) : ItemStack.EMPTY;
    }

    public ItemStack getStoredItem(int slot) {
        ensureSize();
        return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
    }

    public void clearStoredItem(int slot) {
        if (slot >= 0 && slot < items.size()) {
            if (!items.get(slot).isEmpty()) {
                items.set(slot, ItemStack.EMPTY);
                revision++;
            }
        }
    }

    public void setItem(int slot, ItemStack stack) {
        ensureSize();
        if (slot >= 0 && slot < size()) {
            ItemStack copy = stack.copy();
            if (!ItemStack.matches(items.get(slot), copy)) {
                items.set(slot, copy);
                revision++;
            }
        }
    }

    void setItemReference(int slot, ItemStack stack) {
        ensureSize();
        if (slot >= 0 && slot < size()) {
            if (!ItemStack.matches(items.get(slot), stack)) {
                items.set(slot, stack);
                revision++;
            }
        }
    }

    /** Changes whenever the inventory contents or unlock state changes. */
    public long revision() {
        return revision;
    }

    /** Marks an in-place ItemStack mutation that cannot be detected by a slot write. */
    public void touch() {
        revision++;
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
        revision++;
        return removed;
    }

    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= size()) {
            return ItemStack.EMPTY;
        }
        ItemStack previous = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        if (!previous.isEmpty()) {
            revision++;
        }
        return previous;
    }

    public int getUnlockedSlots() {
        return Math.min(Math.max(Config.startUnlockedSlots(), unlockedSlots), size());
    }

    public void setUnlockedSlots(int unlockedSlots) {
        int normalized = Math.max(0, Math.min(unlockedSlots, size()));
        if (this.unlockedSlots != normalized) {
            this.unlockedSlots = normalized;
            revision++;
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    /** Seeds a new state from the physical slots already loaded by vanilla. */
    public void initializeFromInventory(Inventory inventory) {
        ensureSize();
        for (int slot = 0; slot < size(); slot++) {
            int physicalSlot = slot + 9;
            items.set(slot, physicalSlot < Inventory.INVENTORY_SIZE
                    ? inventory.items.get(physicalSlot)
                    : ItemStack.EMPTY);
        }
        initialized = true;
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
        copy.revision = revision;
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
        ListTag prototypes = new ListTag();
        Map<ItemStackKey, Integer> prototypeIds = new HashMap<>();
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty()) {
                ItemStackKey key = new ItemStackKey(stack, registries);
                int prototypeId = prototypeIds.computeIfAbsent(key, ignored -> {
                    int id = prototypes.size();
                    CompoundTag prototype = new CompoundTag();
                    prototype.putInt("Id", id);
                    prototype.put("Stack", stack.copyWithCount(1).saveOptional(registries));
                    prototypes.add(prototype);
                    return id;
                });
                CompoundTag entry = new CompoundTag();
                entry.putInt("Slot", slot);
                entry.putInt(PROTOTYPE_ID, prototypeId);
                entry.putInt(COUNT, stack.getCount());
                entries.add(entry);
            }
        }
        tag.put(ITEMS, entries);
        tag.put(PROTOTYPES, prototypes);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
        if (!tag.contains(PROTOTYPES, Tag.TAG_LIST)) {
            importLegacy(tag, registries);
            return;
        }
        unlockedSlots = tag.getInt(UNLOCKED);
        ListTag prototypes = tag.getList(PROTOTYPES, Tag.TAG_COMPOUND);
        List<ItemStack> decoded = new ArrayList<>(prototypes.size());
        for (int i = 0; i < prototypes.size(); i++) {
            CompoundTag prototype = prototypes.getCompound(i);
            decoded.add(ItemStack.parseOptional(registries, prototype.getCompound("Stack")));
        }
        ListTag entries = tag.getList(ITEMS, Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            int slot = entry.getInt("Slot");
            int prototypeId = entry.getInt(PROTOTYPE_ID);
            if (slot >= 0 && prototypeId >= 0 && prototypeId < decoded.size()) {
                ItemStack prototype = decoded.get(prototypeId);
                ensureStorage(slot + 1);
                items.set(slot, prototype.isEmpty() ? ItemStack.EMPTY
                        : prototype.copyWithCount(Math.max(1, entry.getInt(COUNT))));
            }
        }
        initialized = true;
    }

    private record ItemStackKey(String value) {
        ItemStackKey(ItemStack stack, HolderLookup.Provider registries) {
            this(stack.copyWithCount(1).saveOptional(registries).toString());
        }
    }
}
