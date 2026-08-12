package infiniteinvo.inventory;

import infiniteinvo.InfiniteInvo;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Indexed container view of a player's actual expanded item list. */
public final class ExtendedInventoryContainer implements Container {
    private final Inventory inventory;

    public ExtendedInventoryContainer(Inventory inventory) {
        this.inventory = inventory;
        ExtendedInventory.ensure(inventory);
    }

    @Override
    public int getContainerSize() {
        return InfiniteInventoryData.state(inventory.player).size();
    }

    @Override
    public boolean isEmpty() {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            if (!getItem(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        int physical = slot + 9;
        return slot >= 0 && slot < getContainerSize() ? inventory.items.get(physical) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= getContainerSize() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = ContainerHelper.removeItem(inventory.items, slot + 9, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= getContainerSize()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = inventory.items.set(slot + 9, ItemStack.EMPTY);
        setChanged();
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < getContainerSize()) {
            inventory.items.set(slot + 9, stack);
        }
    }

    @Override
    public void setChanged() {
        inventory.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return player == inventory.player;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= 0 && slot < InfiniteInventoryData.getUnlocked(inventory.player)
                && !stack.is(InfiniteInvo.LOCKED_SLOT.asItem())
                && InfiniteInventoryData.canInsertIntoVirtualSlot(stack);
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            inventory.items.set(slot + 9, ItemStack.EMPTY);
        }
        setChanged();
    }
}
