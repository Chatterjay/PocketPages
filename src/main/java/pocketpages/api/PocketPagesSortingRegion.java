package pocketpages.api;

import java.util.List;

/**
 * A coherent, currently visible player inventory region.
 *
 * <p>All listed slots belong to the menu page represented by the containing
 * {@link PocketPagesSortingView}. Do not retain them after a page change.</p>
 */
public record PocketPagesSortingRegion(PocketPagesSortingArea area,
                                        List<PocketPagesSortingSlot> slots) {
    public PocketPagesSortingRegion {
        slots = List.copyOf(slots);
    }
}
