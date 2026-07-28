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
        for (int i = 0; i < 27 && i + 9 < player.getInventory().items.size() && i < getContainerSize(); i++) {
            state.setItem(i, player.getInventory().getItem(i + 9));
        }
        InfiniteInventoryData.markDirty(player);
    }

    void syncToPlayer(Player player) {
        for (int i = 0; i < 27 && i + 9 < player.getInventory().items.size() && i < getContainerSize(); i++) {
            player.getInventory().setItem(i + 9, state.getItem(i).copy());
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
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = state.removeItemNoUpdate(slot);
        if (!removed.isEmpty()) {
            InfiniteInventoryData.markDirty(owner);
        }
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        state.setItem(slot, stack);
        InfiniteInventoryData.markDirty(owner);
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
        }
        InfiniteInventoryData.markDirty(owner);
    }
}
