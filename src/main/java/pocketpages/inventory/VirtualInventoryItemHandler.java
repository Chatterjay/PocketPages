package pocketpages.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/** Capability view containing only PocketPages's virtual slots. */
public final class VirtualInventoryItemHandler implements IItemHandlerModifiable {
    private final Inventory inventory;

    public VirtualInventoryItemHandler(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public int getSlots() {
        return PocketPagesInventoryItemHandler.getVirtualSlots(inventory);
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return isValidSlot(slot)
                ? PocketPagesInventoryItemHandler.getStackInSlot(inventory, absoluteSlot(slot))
                : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return isValidSlot(slot)
                ? PocketPagesInventoryItemHandler.insertItem(inventory, absoluteSlot(slot), stack, simulate)
                : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return isValidSlot(slot)
                ? PocketPagesInventoryItemHandler.extractItem(inventory, absoluteSlot(slot), amount, simulate)
                : ItemStack.EMPTY;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (isValidSlot(slot)) {
            PocketPagesInventoryItemHandler.setStackInSlot(inventory, absoluteSlot(slot), stack);
        }
    }

    @Override
    public int getSlotLimit(int slot) {
        return isValidSlot(slot)
                ? PocketPagesInventoryItemHandler.getSlotLimit(inventory, absoluteSlot(slot))
                : 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return isValidSlot(slot)
                && PocketPagesInventoryItemHandler.isItemValid(inventory, absoluteSlot(slot), stack);
    }

    private int absoluteSlot(int virtualSlot) {
        return 36 + virtualSlot;
    }

    private boolean isValidSlot(int slot) {
        return slot >= 0 && slot < getSlots()
                && PocketPagesInventoryItemHandler.isOverflowSlot(inventory, absoluteSlot(slot));
    }
}
