package infiniteinvo.api;

import infiniteinvo.DebugLog;
import infiniteinvo.inventory.CreativeInventoryPaging;
import infiniteinvo.inventory.InfiniteInventoryData;
import infiniteinvo.inventory.ScrollableInventoryMenu;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * Common integration point for client or server inventory-sorting mods.
 *
 * <p>This API deliberately exposes only the visible menu slots and their
 * player-owned regions. It therefore works with InfiniteInvo's own screen and
 * with paged player rows shown in other menus, without giving integrations
 * access to the backing list or SavedData.</p>
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

        List<InfiniteInvoSortingSlot> storageSlots = new ArrayList<>();
        List<InfiniteInvoSortingSlot> hotbarSlots = new ArrayList<>();
        List<InfiniteInvoSortingSlot> armorSlots = new ArrayList<>();
        List<InfiniteInvoSortingSlot> offhandSlots = new ArrayList<>();
        boolean paged = false;
        int unlocked = InfiniteInventoryData.getUnlocked(player);

        if (menu instanceof ScrollableInventoryMenu scrollable) {
            paged = true;
            for (int menuSlot = 0; menuSlot < menu.slots.size(); menuSlot++) {
                Slot slot = menu.slots.get(menuSlot);
                int storageSlot = scrollable.getVisibleStorageSlot(slot);
                if (storageSlot >= 0) {
                    storageSlots.add(new InfiniteInvoSortingSlot(
                            menuSlot, slot.getContainerSlot(), storageSlot, storageSlot < unlocked));
                }
            }
        } else if (CreativeInventoryPaging.isPagedPlayerStorageMenu(menu)) {
            paged = true;
            for (int menuSlot = 0; menuSlot < menu.slots.size(); menuSlot++) {
                Slot slot = menu.slots.get(menuSlot);
                int storageSlot = CreativeInventoryPaging.getPagedStorageSlot(menu, slot);
                if (storageSlot >= 0) {
                    storageSlots.add(new InfiniteInvoSortingSlot(
                            menuSlot, slot.getContainerSlot(), storageSlot, storageSlot < unlocked));
                }
            }
        }

        if (storageSlots.isEmpty()) {
            return Optional.empty();
        }
        for (int menuSlot = 0; menuSlot < menu.slots.size(); menuSlot++) {
            Slot slot = menu.slots.get(menuSlot);
            if (slot.container == player.getInventory()
                    && slot.getContainerSlot() >= 0 && slot.getContainerSlot() < 9) {
                hotbarSlots.add(new InfiniteInvoSortingSlot(menuSlot, slot.getContainerSlot(), -1, true));
            } else if (slot.container == player.getInventory()
                    && slot.getContainerSlot() >= 36 && slot.getContainerSlot() <= 39) {
                armorSlots.add(new InfiniteInvoSortingSlot(menuSlot, slot.getContainerSlot(), -1, true));
            } else if (slot.container == player.getInventory() && slot.getContainerSlot() == 40) {
                offhandSlots.add(new InfiniteInvoSortingSlot(menuSlot, slot.getContainerSlot(), -1, true));
            }
        }

        Map<InfiniteInvoSortingArea, List<InfiniteInvoSortingSlot>> regions =
                new EnumMap<>(InfiniteInvoSortingArea.class);
        regions.put(InfiniteInvoSortingArea.PLAYER_STORAGE, List.copyOf(storageSlots));
        if (!hotbarSlots.isEmpty()) {
            regions.put(InfiniteInvoSortingArea.PLAYER_HOTBAR, List.copyOf(hotbarSlots));
        }
        if (!armorSlots.isEmpty()) {
            regions.put(InfiniteInvoSortingArea.PLAYER_ARMOR, List.copyOf(armorSlots));
        }
        if (!offhandSlots.isEmpty()) {
            regions.put(InfiniteInvoSortingArea.PLAYER_OFFHAND, List.copyOf(offhandSlots));
        }
        DebugLog.debug("[Sorting] view menu={} paged={} storage={} hotbar={} armor={} offhand={} unlocked={}",
                menu.getClass().getSimpleName(), paged, storageSlots.size(), hotbarSlots.size(),
                armorSlots.size(), offhandSlots.size(), unlocked);
        return Optional.of(new View(menu, regions, paged));
    }

    private record View(
            AbstractContainerMenu menu,
            Map<InfiniteInvoSortingArea, List<InfiniteInvoSortingSlot>> slotsByArea,
            boolean isPaged
    ) implements InfiniteInvoSortingView {
        @Override
        public List<InfiniteInvoSortingRegion> regions() {
            return slotsByArea.entrySet().stream()
                    .map(entry -> new InfiniteInvoSortingRegion(entry.getKey(), entry.getValue()))
                    .toList();
        }
    }
}
