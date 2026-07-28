package infiniteinvo.inventory;

import infiniteinvo.InfiniteInvo;
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
        if (virtualIndex < container.getContainerSize() && (menu.isUnlocked(virtualIndex) || stack.isEmpty())) {
            container.setItem(virtualIndex, stack);
            setChanged();
        }
    }

    @Override
    public ItemStack remove(int amount) {
        return virtualIndex < container.getContainerSize() && (menu.isUnlocked(virtualIndex) || hasItem())
                ? container.removeItem(virtualIndex, amount)
                : ItemStack.EMPTY;
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
        return virtualIndex < container.getContainerSize() && menu.isUnlocked(virtualIndex) && !stack.is(InfiniteInvo.LOCKED_SLOT.asItem());
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
