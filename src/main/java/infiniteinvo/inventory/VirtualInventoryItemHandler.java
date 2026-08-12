package infiniteinvo.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/** Capability view containing only InfiniteInvo's virtual slots. */
public final class VirtualInventoryItemHandler implements IItemHandlerModifiable {
    private final Inventory inventory;

    public VirtualInventoryItemHandler(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public int getSlots() {
        return InfiniteInventoryItemHandler.getVirtualSlots(inventory);
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return isValidSlot(slot)
                ? InfiniteInventoryItemHandler.getStackInSlot(inventory, absoluteSlot(slot))
                : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return isValidSlot(slot)
                ? InfiniteInventoryItemHandler.insertItem(inventory, absoluteSlot(slot), stack, simulate)
                : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return isValidSlot(slot)
                ? InfiniteInventoryItemHandler.extractItem(inventory, absoluteSlot(slot), amount, simulate)
                : ItemStack.EMPTY;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (isValidSlot(slot)) {
            InfiniteInventoryItemHandler.setStackInSlot(inventory, absoluteSlot(slot), stack);
        }
    }

    @Override
    public int getSlotLimit(int slot) {
        return isValidSlot(slot)
                ? InfiniteInventoryItemHandler.getSlotLimit(inventory, absoluteSlot(slot))
                : 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return isValidSlot(slot)
                && InfiniteInventoryItemHandler.isItemValid(inventory, absoluteSlot(slot), stack);
    }

    private int absoluteSlot(int virtualSlot) {
        return 36 + virtualSlot;
    }

    private boolean isValidSlot(int slot) {
        return slot >= 0 && slot < getSlots()
                && InfiniteInventoryItemHandler.isOverflowSlot(inventory, absoluteSlot(slot));
    }
}
