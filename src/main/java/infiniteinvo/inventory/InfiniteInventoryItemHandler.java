package infiniteinvo.inventory;

import infiniteinvo.Config;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Shared virtual-slot implementation for NeoForge player inventory handlers. */
public final class InfiniteInventoryItemHandler {
    private static final int VANILLA_MAIN_STORAGE_SLOTS = 27;

    private InfiniteInventoryItemHandler() {
    }

    public static int getSlots(Inventory inventory) {
        return Config.exposeVirtualSlotsToAutomation()
                ? inventory.items.size() + getVirtualSlots(inventory)
                : inventory.items.size();
    }

    public static boolean isOverflowSlot(Inventory inventory, int slot) {
        return slot >= inventory.items.size() && slot < inventory.items.size() + getVirtualSlots(inventory);
    }

    public static boolean isExposedOverflowSlot(Inventory inventory, int slot) {
        return Config.exposeVirtualSlotsToAutomation() && isOverflowSlot(inventory, slot);
    }

    public static int getVirtualSlots(Inventory inventory) {
        return Math.max(0, InfiniteInventoryData.state(inventory.player).size() - VANILLA_MAIN_STORAGE_SLOTS);
    }

    public static ItemStack getStackInSlot(Inventory inventory, int slot) {
        int stateSlot = stateSlot(inventory, slot);
        return stateSlot < 0 || !isUnlocked(inventory.player, stateSlot)
                ? ItemStack.EMPTY
                : InfiniteInventoryData.state(inventory.player).getItem(stateSlot).copy();
    }

    public static ItemStack insertItem(Inventory inventory, int slot, ItemStack stack, boolean simulate) {
        int stateSlot = stateSlot(inventory, slot);
        if (stateSlot < 0 || stack.isEmpty() || !isUnlocked(inventory.player, stateSlot)
                || !InfiniteInventoryData.canInsertIntoVirtualSlot(stack)) {
            return stack;
        }

        InfiniteInventoryState state = InfiniteInventoryData.state(inventory.player);
        ItemStack existing = state.getItem(stateSlot);
        int limit = Math.min(stack.getMaxStackSize(), inventory.getMaxStackSize());
        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(existing, stack) || existing.getCount() >= limit) {
                return stack;
            }

            int moved = Math.min(limit - existing.getCount(), stack.getCount());
            if (!simulate) {
                state.setItem(stateSlot, existing.copyWithCount(existing.getCount() + moved));
                InfiniteInventoryData.markDirty(inventory.player);
            }
            return stack.getCount() == moved ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - moved);
        }

        int moved = Math.min(limit, stack.getCount());
        if (!simulate) {
            state.setItem(stateSlot, stack.copyWithCount(moved));
            InfiniteInventoryData.markDirty(inventory.player);
        }
        return stack.getCount() == moved ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - moved);
    }

    public static ItemStack extractItem(Inventory inventory, int slot, int amount, boolean simulate) {
        int stateSlot = stateSlot(inventory, slot);
        if (stateSlot < 0 || amount <= 0 || !isUnlocked(inventory.player, stateSlot)) {
            return ItemStack.EMPTY;
        }

        InfiniteInventoryState state = InfiniteInventoryData.state(inventory.player);
        ItemStack existing = state.getItem(stateSlot);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int moved = Math.min(amount, existing.getCount());
        ItemStack extracted = existing.copyWithCount(moved);
        if (!simulate) {
            state.setItem(stateSlot, existing.getCount() == moved
                    ? ItemStack.EMPTY
                    : existing.copyWithCount(existing.getCount() - moved));
            InfiniteInventoryData.markDirty(inventory.player);
        }
        return extracted;
    }

    public static void setStackInSlot(Inventory inventory, int slot, ItemStack stack) {
        int stateSlot = stateSlot(inventory, slot);
        if (stateSlot < 0 || !isUnlocked(inventory.player, stateSlot)
                || (!stack.isEmpty() && !InfiniteInventoryData.canInsertIntoVirtualSlot(stack))) {
            return;
        }

        InfiniteInventoryData.state(inventory.player).setItem(stateSlot, stack);
        InfiniteInventoryData.markDirty(inventory.player);
    }

    public static int getSlotLimit(Inventory inventory, int slot) {
        return isOverflowSlot(inventory, slot) && isUnlocked(inventory.player, stateSlot(inventory, slot))
                ? inventory.getMaxStackSize()
                : 0;
    }

    public static boolean isItemValid(Inventory inventory, int slot, ItemStack stack) {
        int stateSlot = stateSlot(inventory, slot);
        return stateSlot >= 0 && isUnlocked(inventory.player, stateSlot)
                && InfiniteInventoryData.canInsertIntoVirtualSlot(stack);
    }

    private static int stateSlot(Inventory inventory, int slot) {
        if (!isOverflowSlot(inventory, slot)) {
            return -1;
        }
        return VANILLA_MAIN_STORAGE_SLOTS + slot - inventory.items.size();
    }

    private static boolean isUnlocked(Player player, int stateSlot) {
        return stateSlot < InfiniteInventoryData.getUnlocked(player);
    }
}
