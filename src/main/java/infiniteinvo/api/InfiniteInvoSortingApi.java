package infiniteinvo.api;

import infiniteinvo.inventory.CreativeInventoryPaging;
import infiniteinvo.inventory.InfiniteInventoryData;
import infiniteinvo.inventory.ScrollableInventoryMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * Common integration point for client or server inventory-sorting mods.
 *
 * <p>This API deliberately exposes only the visible menu slots. It therefore
 * works with InfiniteInvo's own screen and with the paged player rows shown in
 * other menus, without giving integrations access to the backing inventory
 * list or SavedData.</p>
 */
public final class InfiniteInvoSortingApi {
    private InfiniteInvoSortingApi() {
    }

    /**
     * Returns an InfiniteInvo view for this menu, or empty when the menu is
     * not currently managed by InfiniteInvo.
     */
    public static Optional<InfiniteInvoSortingView> findView(Player player, AbstractContainerMenu menu) {
        if (player == null || menu == null) {
            return Optional.empty();
        }

        List<InfiniteInvoSortingSlot> slots = new ArrayList<>();
        boolean paged = false;
        int unlocked = InfiniteInventoryData.getUnlocked(player);

        if (menu instanceof ScrollableInventoryMenu scrollable) {
            paged = true;
            int capacity = scrollable.getStore().getContainerSize();
            for (int menuSlot = 0; menuSlot < menu.slots.size(); menuSlot++) {
                Slot slot = menu.slots.get(menuSlot);
                if (slot.container != scrollable.getStore()) {
                    continue;
                }
                int storageSlot = slot.getContainerSlot();
                if (storageSlot >= 0 && storageSlot < capacity) {
                    slots.add(new InfiniteInvoSortingSlot(menuSlot, storageSlot, storageSlot < unlocked));
                }
            }
        } else if (CreativeInventoryPaging.isPagedPlayerStorageMenu(menu)) {
            paged = true;
            for (int menuSlot = 0; menuSlot < menu.slots.size(); menuSlot++) {
                int storageSlot = CreativeInventoryPaging.getPagedStorageSlot(menu, menu.slots.get(menuSlot));
                if (storageSlot >= 0) {
                    slots.add(new InfiniteInvoSortingSlot(menuSlot, storageSlot, storageSlot < unlocked));
                }
            }
        }

        return slots.isEmpty()
                ? Optional.empty()
                : Optional.of(new View(menu, List.copyOf(slots), paged));
    }

    private record View(
            AbstractContainerMenu menu,
            List<InfiniteInvoSortingSlot> visibleStorageSlots,
            boolean isPaged
    ) implements InfiniteInvoSortingView {
    }
}
