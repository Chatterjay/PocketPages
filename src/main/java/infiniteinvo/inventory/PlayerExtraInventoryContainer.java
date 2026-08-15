package infiniteinvo.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Shared container view over the player's extended Inventory.items entries. */
abstract class PlayerExtraInventoryContainer implements Container {
    private static final int FIRST_EXTRA_SLOT = 9;

    protected final Inventory inventory;
    private final Player owner;

    protected PlayerExtraInventoryContainer(Player owner) {
        this.owner = owner;
        this.inventory = owner.getInventory();
        ExtendedInventory.ensure(inventory);
    }

    protected final boolean hasSlot(int slot) {
        int inventorySlot = slot + FIRST_EXTRA_SLOT;
        return slot >= 0 && slot < getContainerSize() && inventorySlot < inventory.items.size();
    }

    protected final int inventorySlot(int slot) {
        return slot + FIRST_EXTRA_SLOT;
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
        return hasSlot(slot) ? inventory.items.get(inventorySlot(slot)) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!hasSlot(slot) || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = ContainerHelper.removeItem(inventory.items, inventorySlot(slot), amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!hasSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = inventory.items.set(inventorySlot(slot), ItemStack.EMPTY);
        setChanged();
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (hasSlot(slot)) {
            inventory.items.set(inventorySlot(slot), stack);
        }
    }

    @Override
    public void setChanged() {
        inventory.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return player == owner;
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            if (hasSlot(slot)) {
                inventory.items.set(inventorySlot(slot), ItemStack.EMPTY);
            }
        }
        setChanged();
    }
}
