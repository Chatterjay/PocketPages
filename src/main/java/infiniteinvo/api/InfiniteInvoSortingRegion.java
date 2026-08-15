package infiniteinvo.api;

import java.util.List;

/**
 * A coherent, currently visible player inventory region.
 *
 * <p>All listed slots belong to the menu page represented by the containing
 * {@link InfiniteInvoSortingView}. Do not retain them after a page change.</p>
 */
public record InfiniteInvoSortingRegion(InfiniteInvoSortingArea area,
                                        List<InfiniteInvoSortingSlot> slots) {
    public InfiniteInvoSortingRegion {
        slots = List.copyOf(slots);
    }
}
