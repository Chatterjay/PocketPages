package infiniteinvo.inventory;

import infiniteinvo.InfiniteInvo;
import infiniteinvo.DebugLog;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

final class ScrollSlot extends Slot {
    private final ScrollableInventoryMenu menu;
    private int virtualIndex;

    ScrollSlot(ScrollableInventoryMenu menu, Container container, int index, int x, int y) {
        super(container, index, x, y);
        this.menu = menu;
        this.virtualIndex = index;
    }

    void setVirtualIndex(int index) {
        this.virtualIndex = index;
    }

    @Override
    public ItemStack getItem() {
        return virtualIndex < container.getContainerSize() ? container.getItem(virtualIndex) : ItemStack.EMPTY;
    }

    @Override
    public boolean hasItem() {
        return !getItem().isEmpty();
    }

    @Override
    public void set(ItemStack stack) {
        if (virtualIndex < container.getContainerSize() && (menu.isUnlocked(virtualIndex) || stack.isEmpty())
                && (virtualIndex < 27 || InfiniteInventoryData.canInsertIntoVirtualSlot(stack))) {
            DebugLog.debug("[Paging][Server] slot set player={} virtualSlot={} physicalMenuSlot={} old={} new={}",
                    menu.getPlayerName(), virtualIndex, getSlotIndex(), DebugLog.stack(getItem()), DebugLog.stack(stack));
            container.setItem(virtualIndex, stack);
            setChanged();
        } else {
            DebugLog.debug("[Paging][Server] slot set rejected player={} virtualSlot={} value={} unlocked={}",
                    menu.getPlayerName(), virtualIndex, DebugLog.stack(stack), menu.isUnlocked(virtualIndex));
        }
    }

    @Override
    public ItemStack remove(int amount) {
        if (virtualIndex >= container.getContainerSize() || (!menu.isUnlocked(virtualIndex) && !hasItem())) {
            DebugLog.debug("[Paging][Server] slot remove rejected player={} virtualSlot={} amount={}",
                    menu.getPlayerName(), virtualIndex, amount);
            return ItemStack.EMPTY;
        }
        ItemStack before = getItem().copy();
        ItemStack removed = container.removeItem(virtualIndex, amount);
        DebugLog.debug("[Paging][Server] slot remove player={} virtualSlot={} amount={} before={} removed={} after={}",
                menu.getPlayerName(), virtualIndex, amount, DebugLog.stack(before), DebugLog.stack(removed),
                DebugLog.stack(container.getItem(virtualIndex)));
        return removed;
    }

    @Override
    public int getContainerSlot() {
        return virtualIndex;
    }

    @Override
    public int getSlotIndex() {
        return virtualIndex;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return virtualIndex < container.getContainerSize() && menu.isUnlocked(virtualIndex)
                && !stack.is(InfiniteInvo.LOCKED_SLOT.asItem())
                && (virtualIndex < 27 || InfiniteInventoryData.canInsertIntoVirtualSlot(stack));
    }

    @Override
    public boolean mayPickup(Player player) {
        // Recovery-only exception: old items must not be stranded or dropped
        // when a player lowers their unlocked-slot count.
        return virtualIndex < container.getContainerSize()
                && (menu.isUnlocked(virtualIndex) || hasItem())
                && super.mayPickup(player);
    }
}
