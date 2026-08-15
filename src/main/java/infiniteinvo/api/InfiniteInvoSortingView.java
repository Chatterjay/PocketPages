package infiniteinvo.api;

import java.util.List;
import java.util.Optional;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * Read-only description of InfiniteInvo storage currently visible in a menu.
 *
 * <p>Sorting mods must execute moves through the menu's normal slot-click
 * path. They must not replace or reorder {@code Inventory.items} directly.</p>
 */
public interface InfiniteInvoSortingView {
    /** The menu from which this view was created. */
    AbstractContainerMenu menu();

    /** All currently visible player regions managed by InfiniteInvo. */
    List<InfiniteInvoSortingRegion> regions();

    /**
     * Returns the visible slots in one player region, including locked slots.
     *
     * <p>Skip entries for which {@link InfiniteInvoSortingSlot#isSortable()}
     * is false.</p>
     */
    default List<InfiniteInvoSortingSlot> slots(InfiniteInvoSortingArea area) {
        return regions().stream()
                .filter(region -> region.area() == area)
                .findFirst()
                .map(InfiniteInvoSortingRegion::slots)
                .orElse(List.of());
    }

    /**
     * Resolves the player region containing this exact current menu slot.
     */
    default Optional<InfiniteInvoSortingArea> areaForSlot(Slot slot) {
        if (slot == null) {
            return Optional.empty();
        }
        for (int menuSlot = 0; menuSlot < menu().slots.size(); menuSlot++) {
            if (menu().slots.get(menuSlot) != slot) {
                continue;
            }
            final int currentMenuSlot = menuSlot;
            return regions().stream()
                    .filter(region -> region.slots().stream()
                            .anyMatch(regionSlot -> regionSlot.menuSlot() == currentMenuSlot))
                    .map(InfiniteInvoSortingRegion::area)
                    .findFirst();
        }
        return Optional.empty();
    }

    /** Visible InfiniteInvo main-storage slots, retained for API compatibility. */
    default List<InfiniteInvoSortingSlot> visibleStorageSlots() {
        return slots(InfiniteInvoSortingArea.PLAYER_STORAGE);
    }

    /** Whether these menu slots can be remapped by InfiniteInvo paging. */
    boolean isPaged();
}
