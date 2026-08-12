package infiniteinvo.inventory;

import java.util.AbstractList;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/** Keeps vanilla container slot ids 0-35 stable after the backing list grows. */
public final class VanillaItemListView extends NonNullList<ItemStack> {
    private static final int VANILLA_MAIN_SIZE = 36;
    private final NonNullList<ItemStack> backing;

    public VanillaItemListView(NonNullList<ItemStack> items) {
        super(new AbstractList<>() {
            @Override
            public ItemStack get(int index) {
                return items.get(index);
            }

            @Override
            public ItemStack set(int index, ItemStack value) {
                return items.set(index, value);
            }

            @Override
            public int size() {
                return VANILLA_MAIN_SIZE;
            }
        }, ItemStack.EMPTY);
        this.backing = items;
    }

    /** Clears the vanilla range without shrinking the extended backing list. */
    @Override
    public void clear() {
        for (int index = 0; index < VANILLA_MAIN_SIZE; index++) {
            set(index, ItemStack.EMPTY);
        }
    }
}
