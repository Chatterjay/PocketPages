package pocketpages.api;

import java.util.List;
import java.util.Optional;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * Read-only description of PocketPages storage currently visible in a menu.
 *
 * <p>Sorting mods must execute moves through the menu's normal slot-click
 * path. They must not replace or reorder {@code Inventory.items} directly.</p>
 */
public interface PocketPagesSortingView {
    /** The menu from which this view was created. */
    AbstractContainerMenu menu();

    /** All currently visible player regions managed by PocketPages. */
    List<PocketPagesSortingRegion> regions();

    /**
     * Returns the visible slots in one player region, including locked slots.
     *
     * <p>Skip entries for which {@link PocketPagesSortingSlot#isSortable()}
     * is false.</p>
     */
    default List<PocketPagesSortingSlot> slots(PocketPagesSortingArea area) {
        return regions().stream()
                .filter(region -> region.area() == area)
                .findFirst()
                .map(PocketPagesSortingRegion::slots)
                .orElse(List.of());
    }

    /**
     * Resolves the player region containing this exact current menu slot.
     */
    default Optional<PocketPagesSortingArea> areaForSlot(Slot slot) {
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
                    .map(PocketPagesSortingRegion::area)
                    .findFirst();
        }
        return Optional.empty();
    }

    /** Visible PocketPages main-storage slots, retained for API compatibility. */
    default List<PocketPagesSortingSlot> visibleStorageSlots() {
        return slots(PocketPagesSortingArea.PLAYER_STORAGE);
    }

    /** Whether these menu slots can be remapped by PocketPages paging. */
    boolean isPaged();
}
