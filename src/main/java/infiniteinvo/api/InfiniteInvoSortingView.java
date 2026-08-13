package infiniteinvo.api;

import java.util.List;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Read-only description of InfiniteInvo storage currently visible in a menu.
 *
 * <p>Sorting mods must execute moves through the menu's normal slot-click
 * path. They must not replace or reorder {@code Inventory.items} directly.</p>
 */
public interface InfiniteInvoSortingView {
    /** The menu from which this view was created. */
    AbstractContainerMenu menu();

    /**
     * Visible player-storage slots, including locked slots.
     *
     * <p>Skip entries for which {@link InfiniteInvoSortingSlot#isSortable()}
     * is false.</p>
     */
    List<InfiniteInvoSortingSlot> visibleStorageSlots();

    /** Whether these menu slots can be remapped by InfiniteInvo paging. */
    boolean isPaged();
}
