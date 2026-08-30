package pocketpages.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Direct item-handler access to the real extended Inventory.items slots. */
public final class PocketPagesInventoryItemHandler {
    private static final int FIRST_EXTRA_INVENTORY_SLOT = 36;

    private PocketPagesInventoryItemHandler() {
    }

    public static boolean isOverflowSlot(Inventory inventory, int slot) {
        ExtendedInventory.ensure(inventory);
        return slot >= FIRST_EXTRA_INVENTORY_SLOT && slot < inventory.items.size();
    }

    public static int getVirtualSlots(Inventory inventory) {
        ExtendedInventory.ensure(inventory);
        return Math.max(0, inventory.items.size() - FIRST_EXTRA_INVENTORY_SLOT);
    }

    public static ItemStack getStackInSlot(Inventory inventory, int slot) {
        return isOverflowSlot(inventory, slot) && isUnlocked(inventory, slot)
                ? inventory.items.get(slot)
                : ItemStack.EMPTY;
    }

    public static ItemStack insertItem(Inventory inventory, int slot, ItemStack stack, boolean simulate) {
        if (!isOverflowSlot(inventory, slot) || !isUnlocked(inventory, slot) || stack.isEmpty()
                || !PocketPagesInventoryData.canInsertIntoVirtualSlot(stack)) {
            return stack;
        }
        ItemStack existing = inventory.items.get(slot);
        int limit = Math.min(stack.getMaxStackSize(), inventory.getMaxStackSize());
        if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack)) {
            return stack;
        }
        int room = existing.isEmpty() ? limit : limit - existing.getCount();
        int moved = Math.min(room, stack.getCount());
        if (moved <= 0) {
            return stack;
        }
        if (!simulate) {
            inventory.items.set(slot, existing.isEmpty()
                    ? stack.copyWithCount(moved)
                    : existing.copyWithCount(existing.getCount() + moved));
            inventory.setChanged();
        }
        return moved == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - moved);
    }

    public static ItemStack extractItem(Inventory inventory, int slot, int amount, boolean simulate) {
        if (!isOverflowSlot(inventory, slot) || !isUnlocked(inventory, slot) || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack existing = inventory.items.get(slot);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int moved = Math.min(amount, existing.getCount());
        if (simulate) {
            return existing.copyWithCount(moved);
        }
        ItemStack removed = net.minecraft.world.ContainerHelper.removeItem(inventory.items, slot, moved);
        if (!removed.isEmpty()) {
            inventory.setChanged();
        }
        return removed;
    }

    public static void setStackInSlot(Inventory inventory, int slot, ItemStack stack) {
        if (isOverflowSlot(inventory, slot) && isUnlocked(inventory, slot)
                && (stack.isEmpty() || PocketPagesInventoryData.canInsertIntoVirtualSlot(stack))) {
            inventory.items.set(slot, stack);
            inventory.setChanged();
        }
    }

    public static int getSlotLimit(Inventory inventory, int slot) {
        return isOverflowSlot(inventory, slot) && isUnlocked(inventory, slot) ? inventory.getMaxStackSize() : 0;
    }

    public static boolean isItemValid(Inventory inventory, int slot, ItemStack stack) {
        return isOverflowSlot(inventory, slot) && isUnlocked(inventory, slot)
                && PocketPagesInventoryData.canInsertIntoVirtualSlot(stack);
    }

    private static boolean isUnlocked(Inventory inventory, int inventorySlot) {
        return inventorySlot - 9 < PocketPagesInventoryData.getUnlocked(inventory.player);
    }
}
