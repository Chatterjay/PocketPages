package pocketpages.inventory;

import pocketpages.PocketPages;
import pocketpages.DebugLog;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

final class ScrollSlot extends Slot {
    private static final int FIRST_STORAGE_INVENTORY_SLOT = 9;

    private final ScrollableInventoryMenu menu;
    private int virtualIndex;

    ScrollSlot(ScrollableInventoryMenu menu, Inventory inventory, int x, int y) {
        // Expose the backing player inventory to generic menu integrations.
        // The virtual index remains the source of truth for paging and locks.
        super(inventory, FIRST_STORAGE_INVENTORY_SLOT, x, y);
        this.menu = menu;
        this.virtualIndex = 0;
    }

    void setVirtualIndex(int index) {
        this.virtualIndex = index;
    }

    @Override
    public ItemStack getItem() {
        return virtualIndex < menu.getStore().getContainerSize()
                ? menu.getStore().getItem(virtualIndex)
                : ItemStack.EMPTY;
    }

    @Override
    public boolean hasItem() {
        return !getItem().isEmpty();
    }

    @Override
    public void set(ItemStack stack) {
        if (virtualIndex < menu.getStore().getContainerSize() && (menu.isUnlocked(virtualIndex) || stack.isEmpty())
                && (virtualIndex < 27 || PocketPagesInventoryData.canInsertIntoVirtualSlot(stack))) {
            DebugLog.debug("[Paging][Server] slot set player={} virtualSlot={} physicalMenuSlot={} old={} new={}",
                    menu.getPlayerName(), virtualIndex, getSlotIndex(), DebugLog.stack(getItem()), DebugLog.stack(stack));
            menu.getStore().setItem(virtualIndex, stack);
            setChanged();
        } else {
            DebugLog.debug("[Paging][Server] slot set rejected player={} virtualSlot={} value={} unlocked={}",
                    menu.getPlayerName(), virtualIndex, DebugLog.stack(stack), menu.isUnlocked(virtualIndex));
        }
    }

    @Override
    public ItemStack remove(int amount) {
        if (virtualIndex >= menu.getStore().getContainerSize() || (!menu.isUnlocked(virtualIndex) && !hasItem())) {
            DebugLog.debug("[Paging][Server] slot remove rejected player={} virtualSlot={} amount={}",
                    menu.getPlayerName(), virtualIndex, amount);
            return ItemStack.EMPTY;
        }
        ItemStack before = getItem().copy();
        ItemStack removed = menu.getStore().removeItem(virtualIndex, amount);
        DebugLog.debug("[Paging][Server] slot remove player={} virtualSlot={} amount={} before={} removed={} after={}",
                menu.getPlayerName(), virtualIndex, amount, DebugLog.stack(before), DebugLog.stack(removed),
                DebugLog.stack(menu.getStore().getItem(virtualIndex)));
        return removed;
    }

    @Override
    public int getContainerSlot() {
        return FIRST_STORAGE_INVENTORY_SLOT + virtualIndex;
    }

    @Override
    public int getSlotIndex() {
        return getContainerSlot();
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return virtualIndex < menu.getStore().getContainerSize() && menu.isUnlocked(virtualIndex)
                && !stack.is(PocketPages.LOCKED_SLOT.asItem())
                && (virtualIndex < 27 || PocketPagesInventoryData.canInsertIntoVirtualSlot(stack));
    }

    @Override
    public boolean mayPickup(Player player) {
        // Recovery-only exception: old items must not be stranded or dropped
        // when a player lowers their unlocked-slot count.
        return virtualIndex < menu.getStore().getContainerSize()
                && (menu.isUnlocked(virtualIndex) || hasItem())
                && super.mayPickup(player);
    }

    int getVirtualIndex() {
        return virtualIndex;
    }
}
