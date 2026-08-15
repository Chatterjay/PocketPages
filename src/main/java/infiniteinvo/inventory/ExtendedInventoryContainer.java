package infiniteinvo.inventory;

import infiniteinvo.InfiniteInvo;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Indexed container view of a player's actual expanded item list. */
public final class ExtendedInventoryContainer extends PlayerExtraInventoryContainer {

    public ExtendedInventoryContainer(Inventory inventory) {
        super(inventory.player);
    }

    @Override
    public int getContainerSize() {
        return InfiniteInventoryData.state(inventory.player).size();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isUnlocked(slot)
                && !stack.is(InfiniteInvo.LOCKED_SLOT.asItem())
                && InfiniteInventoryData.canInsertIntoVirtualSlot(stack);
    }

    public boolean isUnlocked(int slot) {
        return slot >= 0 && slot < InfiniteInventoryData.getUnlocked(inventory.player);
    }

}
