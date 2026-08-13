package infiniteinvo.api;

/**
 * A currently visible player-storage slot exposed to an inventory sorter.
 *
 * <p>The menu slot is valid only for the menu's current page. The storage slot
 * is the stable InfiniteInvo index, where {@code 0} is the first normal main
 * inventory slot. Do not cache either value across a page change.</p>
 */
public record InfiniteInvoSortingSlot(int menuSlot, int storageSlot, boolean unlocked) {
    /** Returns whether the slot may receive items during a normal sort. */
    public boolean isSortable() {
        return unlocked;
    }
}
