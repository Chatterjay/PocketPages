package infiniteinvo.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Reports direct vanilla writes to the expanded main-inventory list. */
final class TrackedItemList extends NonNullList<ItemStack> {
    private final Inventory inventory;

    TrackedItemList(Inventory inventory, NonNullList<ItemStack> source) {
        // ExtendedInventory supplies a fixed-size NonNullList so accidental
        // structural writes cannot shrink the real player inventory.
        super(source, ItemStack.EMPTY);
        this.inventory = inventory;
    }

    @Override
    public ItemStack set(int index, ItemStack stack) {
        ItemStack previous = super.set(index, stack);
        ExtendedInventory.onItemsMutated(inventory, index);
        return previous;
    }

    /** Inventory.load() clears its lists before restoring individual slots. */
    @Override
    public void clear() {
        for (int index = 0; index < size(); index++) {
            super.set(index, ItemStack.EMPTY);
        }
    }

}
