package infiniteinvo.api;

/**
 * A currently visible player-storage slot exposed to an inventory sorter.
 *
 * <p>The menu slot is valid only for the menu's current page. The backing
 * player inventory slot is the physical player-menu slot id. The logical
 * storage slot is InfiniteInvo's stable main-storage index and is {@code -1}
 * outside the main storage. Do not cache either value across a page change.</p>
 */
public record InfiniteInvoSortingSlot(int menuSlot, int backingInventorySlot,
                                      int storageSlot, boolean unlocked) {
    /**
     * Legacy constructor for integrations that only need the logical storage
     * index. Main-storage entries map to {@code PlayerInventory.items[9 + n]}.
     */
    public InfiniteInvoSortingSlot(int menuSlot, int storageSlot, boolean unlocked) {
        this(menuSlot, storageSlot + 9, storageSlot, unlocked);
    }

    /** Returns whether the slot may receive items during a normal sort. */
    public boolean isSortable() {
        return unlocked;
    }
}
