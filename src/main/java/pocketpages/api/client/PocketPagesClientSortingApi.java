package pocketpages.api.client;

import pocketpages.client.ContainerInventoryPagingController;
import pocketpages.client.CreativeInventoryController;
import pocketpages.client.ScrollableInventoryScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Client-only paging guard for inventory sorting integrations. */
public final class PocketPagesClientSortingApi {
    private PocketPagesClientSortingApi() {
    }

    /**
     * Returns true while PocketPages is changing the current menu page.
     * A sorter must defer clicks and menu reads until this returns false.
     */
    public static boolean isPageChangePending(AbstractContainerMenu menu) {
        return ScrollableInventoryScreen.isPageChangePending(menu)
                || ContainerInventoryPagingController.isPageChangePending(menu)
                || CreativeInventoryController.isPageChangePending(menu);
    }
}
