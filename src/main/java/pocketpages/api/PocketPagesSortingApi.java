package pocketpages.api;

import pocketpages.DebugLog;
import pocketpages.inventory.CreativeInventoryPaging;
import pocketpages.inventory.PocketPagesInventoryData;
import pocketpages.inventory.ScrollableInventoryMenu;
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
 * player-owned regions. It therefore works with PocketPages's own screen and
 * with paged player rows shown in other menus, without giving integrations
 * access to the backing list or SavedData.</p>
 */
public final class PocketPagesSortingApi {
    private PocketPagesSortingApi() {
    }

    /**
     * Returns an PocketPages view for this menu, or empty when the menu is
     * not currently managed by PocketPages.
     */
    public static Optional<PocketPagesSortingView> findView(Player player, AbstractContainerMenu menu) {
        if (player == null || menu == null) {
            return Optional.empty();
        }

        List<PocketPagesSortingSlot> storageSlots = new ArrayList<>();
        List<PocketPagesSortingSlot> hotbarSlots = new ArrayList<>();
        List<PocketPagesSortingSlot> armorSlots = new ArrayList<>();
        List<PocketPagesSortingSlot> offhandSlots = new ArrayList<>();
        boolean paged = false;
        int unlocked = PocketPagesInventoryData.getUnlocked(player);

        if (menu instanceof ScrollableInventoryMenu scrollable) {
            paged = true;
            for (int menuSlot = 0; menuSlot < menu.slots.size(); menuSlot++) {
                Slot slot = menu.slots.get(menuSlot);
                int storageSlot = scrollable.getVisibleStorageSlot(slot);
                if (storageSlot >= 0) {
                    storageSlots.add(new PocketPagesSortingSlot(
                            menuSlot, slot.getContainerSlot(), storageSlot, storageSlot < unlocked));
                }
            }
        } else if (CreativeInventoryPaging.isPagedPlayerStorageMenu(menu)) {
            paged = true;
            for (int menuSlot = 0; menuSlot < menu.slots.size(); menuSlot++) {
                Slot slot = menu.slots.get(menuSlot);
                int storageSlot = CreativeInventoryPaging.getPagedStorageSlot(menu, slot);
                if (storageSlot >= 0) {
                    storageSlots.add(new PocketPagesSortingSlot(
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
                hotbarSlots.add(new PocketPagesSortingSlot(menuSlot, slot.getContainerSlot(), -1, true));
            } else if (slot.container == player.getInventory()
                    && slot.getContainerSlot() >= 36 && slot.getContainerSlot() <= 39) {
                armorSlots.add(new PocketPagesSortingSlot(menuSlot, slot.getContainerSlot(), -1, true));
            } else if (slot.container == player.getInventory() && slot.getContainerSlot() == 40) {
                offhandSlots.add(new PocketPagesSortingSlot(menuSlot, slot.getContainerSlot(), -1, true));
            }
        }

        Map<PocketPagesSortingArea, List<PocketPagesSortingSlot>> regions =
                new EnumMap<>(PocketPagesSortingArea.class);
        regions.put(PocketPagesSortingArea.PLAYER_STORAGE, List.copyOf(storageSlots));
        if (!hotbarSlots.isEmpty()) {
            regions.put(PocketPagesSortingArea.PLAYER_HOTBAR, List.copyOf(hotbarSlots));
        }
        if (!armorSlots.isEmpty()) {
            regions.put(PocketPagesSortingArea.PLAYER_ARMOR, List.copyOf(armorSlots));
        }
        if (!offhandSlots.isEmpty()) {
            regions.put(PocketPagesSortingArea.PLAYER_OFFHAND, List.copyOf(offhandSlots));
        }
        DebugLog.debug("[Sorting] view menu={} paged={} storage={} hotbar={} armor={} offhand={} unlocked={}",
                menu.getClass().getSimpleName(), paged, storageSlots.size(), hotbarSlots.size(),
                armorSlots.size(), offhandSlots.size(), unlocked);
        return Optional.of(new View(menu, regions, paged));
    }

    private record View(
            AbstractContainerMenu menu,
            Map<PocketPagesSortingArea, List<PocketPagesSortingSlot>> slotsByArea,
            boolean isPaged
    ) implements PocketPagesSortingView {
        @Override
        public List<PocketPagesSortingRegion> regions() {
            return slotsByArea.entrySet().stream()
                    .map(entry -> new PocketPagesSortingRegion(entry.getKey(), entry.getValue()))
                    .toList();
        }
    }
}
