package infiniteinvo.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** A lightweight menu view over the persistent player attachment. */
public final class ScrollableInventoryStore implements Container {
    private final Player owner;
    private final InfiniteInventoryState state;

    private ScrollableInventoryStore(Player owner, InfiniteInventoryState state) {
        this.owner = owner;
        this.state = state;
    }

    static ScrollableInventoryStore load(Player player) {
        ScrollableInventoryStore store = new ScrollableInventoryStore(player, InfiniteInventoryData.state(player));
        store.syncFromPlayer(player);
        return store;
    }

    void syncFromPlayer(Player player) {
        boolean changed = false;
        for (int i = 0; i < 27 && i + 9 < player.getInventory().items.size() && i < getContainerSize(); i++) {
            ItemStack nativeStack = player.getInventory().getItem(i + 9);
            if (!ItemStack.matches(state.getItem(i), nativeStack)) {
                state.setItem(i, nativeStack);
                changed = true;
            }
        }
        if (changed) {
            InfiniteInventoryData.markDirty(player);
        }
    }

    /**
     * Reconciles the vanilla main-inventory mirror without changing the active page.
     * This is used while the custom menu is open, when vanilla pickup and automation
     * paths can still mutate inventory slots 9 through 35 directly.
     */
    void syncNativeMirrorFromPlayer(Player player) {
        boolean changed = false;
        for (int stateSlot = 0; stateSlot < 27 && stateSlot < getContainerSize(); stateSlot++) {
            int inventorySlot = stateSlot + 9;
            if (inventorySlot >= player.getInventory().items.size()) {
                break;
            }

            ItemStack nativeStack = player.getInventory().getItem(inventorySlot);
            if (!ItemStack.matches(state.getItem(stateSlot), nativeStack)) {
                state.setItem(stateSlot, nativeStack);
                changed = true;
            }
        }
        if (changed) {
            InfiniteInventoryData.markDirty(player);
        }
    }

    void syncNativeMirrorSlotFromPlayer(Player player, int inventorySlot) {
        int stateSlot = inventorySlot - 9;
        if (stateSlot < 0 || stateSlot >= 27 || stateSlot >= getContainerSize()
                || inventorySlot >= player.getInventory().items.size()) {
            return;
        }

        ItemStack nativeStack = player.getInventory().getItem(inventorySlot);
        if (!ItemStack.matches(state.getItem(stateSlot), nativeStack)) {
            state.setItem(stateSlot, nativeStack);
            InfiniteInventoryData.markDirty(player);
        }
    }

    void syncToPlayer(Player player) {
        for (int i = 0; i < 27 && i + 9 < player.getInventory().items.size() && i < getContainerSize(); i++) {
            ItemStack stateStack = state.getItem(i);
            if (!ItemStack.matches(player.getInventory().getItem(i + 9), stateStack)) {
                player.getInventory().setItem(i + 9, stateStack.copy());
            }
        }
    }

    @Override
    public int getContainerSize() {
        return state.size();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < getContainerSize(); i++) {
            if (!getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return state.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = state.removeItem(slot, amount);
        if (!removed.isEmpty()) {
            InfiniteInventoryData.markDirty(owner);
        }
        syncSlotToPlayer(slot);
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = state.removeItemNoUpdate(slot);
        if (!removed.isEmpty()) {
            InfiniteInventoryData.markDirty(owner);
        }
        syncSlotToPlayer(slot);
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        state.setItem(slot, stack);
        InfiniteInventoryData.markDirty(owner);
        syncSlotToPlayer(slot);
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < getContainerSize(); i++) {
            state.setItem(i, ItemStack.EMPTY);
            syncSlotToPlayer(i);
        }
        InfiniteInventoryData.markDirty(owner);
    }

    private void syncSlotToPlayer(int stateSlot) {
        int inventorySlot = stateSlot + 9;
        if (stateSlot < 0 || stateSlot >= 27 || inventorySlot >= owner.getInventory().items.size()) {
            return;
        }
        ItemStack stateStack = state.getItem(stateSlot);
        if (!ItemStack.matches(owner.getInventory().getItem(inventorySlot), stateStack)) {
            owner.getInventory().setItem(inventorySlot, stateStack.copy());
        }
    }
}
