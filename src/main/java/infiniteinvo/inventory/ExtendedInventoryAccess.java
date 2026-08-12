package infiniteinvo.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

public interface ExtendedInventoryAccess {
    void infiniteinvo$replaceItems(NonNullList<ItemStack> items);
}
