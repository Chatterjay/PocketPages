package pocketpages.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

public interface ExtendedInventoryAccess {
    void pocketpages$replaceItems(NonNullList<ItemStack> items);
}
