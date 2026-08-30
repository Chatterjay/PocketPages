package pocketpages.inventory;

import pocketpages.PocketPages;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Indexed container view of a player's actual expanded item list. */
public final class ExtendedInventoryContainer extends PlayerExtraInventoryContainer {

    public ExtendedInventoryContainer(Inventory inventory) {
        super(inventory.player);
    }

    @Override
    public int getContainerSize() {
        return PocketPagesInventoryData.state(inventory.player).size();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isUnlocked(slot)
                && !stack.is(PocketPages.LOCKED_SLOT.asItem())
                && PocketPagesInventoryData.canInsertIntoVirtualSlot(stack);
    }

    public boolean isUnlocked(int slot) {
        return slot >= 0 && slot < PocketPagesInventoryData.getUnlocked(inventory.player);
    }

}
