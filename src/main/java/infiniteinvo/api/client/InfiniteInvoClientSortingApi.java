package infiniteinvo.api.client;

import infiniteinvo.client.ContainerInventoryPagingController;
import infiniteinvo.client.CreativeInventoryController;
import infiniteinvo.client.ScrollableInventoryScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Client-only paging guard for inventory sorting integrations. */
public final class InfiniteInvoClientSortingApi {
    private InfiniteInvoClientSortingApi() {
    }

    /**
     * Returns true while InfiniteInvo is changing the current menu page.
     * A sorter must defer clicks and menu reads until this returns false.
     */
    public static boolean isPageChangePending(AbstractContainerMenu menu) {
        return ScrollableInventoryScreen.isPageChangePending(menu)
                || ContainerInventoryPagingController.isPageChangePending(menu)
                || CreativeInventoryController.isPageChangePending(menu);
    }
}
