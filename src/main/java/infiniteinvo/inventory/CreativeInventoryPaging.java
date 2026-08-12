package infiniteinvo.inventory;

import infiniteinvo.Config;
import infiniteinvo.DebugLog;
import infiniteinvo.network.CreativeInventoryPageDataPayload;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Maps visible menu slots directly to their real extended-inventory indices. */
public final class CreativeInventoryPaging {
    public static final int PAGE_SIZE = 27;
    private static final Map<UUID, Integer> ACTIVE_ROWS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> ACTIVE_SESSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_REQUESTS = new ConcurrentHashMap<>();
    private static final Map<AbstractContainerMenu, MenuMapping> MENU_MAPPINGS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private CreativeInventoryPaging() {
    }

    public static void selectRow(ServerPlayer player, int requestedRow, int sessionId, int requestId) {
        ExtendedInventory.ensure(player.getInventory());
        UUID playerId = player.getUUID();
        Integer activeSession = ACTIVE_SESSIONS.get(playerId);
        if (activeSession != null && activeSession != sessionId) {
            restorePlayerMenus(player);
            ACTIVE_ROWS.remove(playerId);
            LAST_REQUESTS.remove(playerId);
        }
        int previousRequest = LAST_REQUESTS.getOrDefault(playerId, -1);
        if (ACTIVE_SESSIONS.containsKey(playerId) && ACTIVE_SESSIONS.get(playerId) == sessionId
                && requestId <= previousRequest) {
            return;
        }

        InfiniteInventoryData.dropLegacyPlaceholderItems(player);
        InfiniteInventoryState state = InfiniteInventoryData.state(player);
        int row = Math.max(0, Math.min(requestedRow, maxRow(state.size())));
        mapMenu(player, player.inventoryMenu, row);
        if (player.containerMenu != player.inventoryMenu) {
            mapMenu(player, player.containerMenu, row);
        }

        ACTIVE_ROWS.put(playerId, row);
        ACTIVE_SESSIONS.put(playerId, sessionId);
        LAST_REQUESTS.put(playerId, requestId);
        sendPage(player, row, sessionId, requestId, state);
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) {
            player.containerMenu.broadcastChanges();
        }
        DebugLog.debug("[Paging] mapped real slots player={} row={} virtualStart={}",
                player.getName().getString(), row, row * 9);
    }

    public static void restoreVanillaPage(ServerPlayer player) {
        Integer sessionId = ACTIVE_SESSIONS.get(player.getUUID());
        if (sessionId != null) {
            restoreVanillaPage(player, sessionId);
        }
    }

    public static void restoreVanillaPage(ServerPlayer player, int sessionId) {
        UUID playerId = player.getUUID();
        Integer activeSession = ACTIVE_SESSIONS.get(playerId);
        if (activeSession == null || activeSession != sessionId) {
            return;
        }
        restorePlayerMenus(player);
        ACTIVE_ROWS.remove(playerId);
        ACTIVE_SESSIONS.remove(playerId);
        LAST_REQUESTS.remove(playerId);
        InfiniteInventoryData.dropLockedItems(player);
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) {
            player.containerMenu.broadcastChanges();
        }
    }

    public static void mapClientMenu(Player player, AbstractContainerMenu menu, int row) {
        mapMenu(player, menu, Math.max(0, Math.min(row, maxRow())));
    }

    public static void restoreMenu(AbstractContainerMenu menu) {
        MenuMapping mapping = MENU_MAPPINGS.remove(menu);
        if (mapping != null) {
            mapping.restore();
        }
    }

    public static boolean isPlayerStorageSlot(AbstractContainerMenu menu, Slot slot, Inventory inventory) {
        MenuMapping mapping = MENU_MAPPINGS.get(menu);
        return mapping != null ? mapping.contains(slot)
                : slot.container == inventory && slot.getContainerSlot() >= 9 && slot.getContainerSlot() < 36;
    }

    public static void clearAll(ServerPlayer player) {
        ExtendedInventory.ensure(player.getInventory());
        for (int slot = 0; slot < InfiniteInventoryData.getUnlocked(player); slot++) {
            player.getInventory().items.set(slot + 9, ItemStack.EMPTY);
        }
        InfiniteInventoryData.markDirty(player);
        refreshClientPage(player);
    }

    public static void refreshClientPage(ServerPlayer player) {
        int row = ACTIVE_ROWS.getOrDefault(player.getUUID(), 0);
        Integer session = ACTIVE_SESSIONS.get(player.getUUID());
        if (session != null) {
            sendPage(player, row, session, Integer.MAX_VALUE, InfiniteInventoryData.state(player));
        }
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) {
            player.containerMenu.broadcastChanges();
        }
    }

    private static void mapMenu(Player player, AbstractContainerMenu menu, int row) {
        MenuMapping mapping = MENU_MAPPINGS.get(menu);
        if (mapping == null) {
            mapping = MenuMapping.create(player, menu);
            if (mapping == null) {
                return;
            }
            MENU_MAPPINGS.put(menu, mapping);
        }
        mapping.map(row);
    }

    private static void restorePlayerMenus(Player player) {
        restoreMenu(player.inventoryMenu);
        if (player.containerMenu != player.inventoryMenu) {
            restoreMenu(player.containerMenu);
        }
    }

    private static List<ItemStack> page(InfiniteInventoryState state, int row) {
        int start = row * 9;
        List<ItemStack> stacks = new ArrayList<>(PAGE_SIZE);
        for (int i = 0; i < PAGE_SIZE; i++) {
            stacks.add(start + i < state.size() ? state.getItem(start + i).copy() : ItemStack.EMPTY);
        }
        return stacks;
    }

    private static void sendPage(ServerPlayer player, int row, int sessionId, int requestId,
                                 InfiniteInventoryState state) {
        PacketDistributor.sendToPlayer(player, new CreativeInventoryPageDataPayload(
                row, InfiniteInventoryData.getUnlocked(player), sessionId, requestId, page(state, row)));
    }

    public static int maxRow() {
        return maxRow(Config.totalExtraSlots());
    }

    private static int maxRow(int slots) {
        return Math.max(0, (int) Math.ceil(slots / 9.0D) - 3);
    }

    private record MappedSlot(Slot slot, Container originalContainer, int originalIndex, int offset,
                              ExtendedInventoryContainer extendedContainer) {
        void map(int row) {
            int virtualIndex = row * 9 + offset;
            MutableSlotAccess mutable = (MutableSlotAccess) slot;
            mutable.infiniteinvo$setContainer(extendedContainer);
            mutable.infiniteinvo$setContainerSlot(virtualIndex);
        }

        void restore() {
            MutableSlotAccess mutable = (MutableSlotAccess) slot;
            mutable.infiniteinvo$setContainer(originalContainer);
            mutable.infiniteinvo$setContainerSlot(originalIndex);
        }
    }

    private static final class MenuMapping {
        private final Inventory inventory;
        private final List<MappedSlot> slots;
        private final Map<Slot, Boolean> identities;

        private MenuMapping(Inventory inventory, List<MappedSlot> slots) {
            this.inventory = inventory;
            this.slots = slots;
            this.identities = new IdentityHashMap<>();
            slots.forEach(mapped -> identities.put(mapped.slot(), Boolean.TRUE));
        }

        static MenuMapping create(Player player, AbstractContainerMenu menu) {
            Inventory inventory = player.getInventory();
            List<MappedSlot> slots = new ArrayList<>(PAGE_SIZE);
            ExtendedInventoryContainer extendedContainer = new ExtendedInventoryContainer(inventory);
            for (Slot slot : menu.slots) {
                Slot target = slot instanceof WrappedSlotAccess wrapped ? wrapped.infiniteinvo$getTargetSlot() : slot;
                int index = target.getContainerSlot();
                if (target.container == inventory && index >= 9 && index < 36) {
                    slots.add(new MappedSlot(slot, inventory, index, index - 9, extendedContainer));
                }
            }
            return slots.size() == PAGE_SIZE
                    ? new MenuMapping(inventory, List.copyOf(slots))
                    : null;
        }

        void map(int row) {
            slots.forEach(slot -> slot.map(row));
        }

        void restore() {
            slots.forEach(MappedSlot::restore);
        }

        boolean contains(Slot slot) {
            return identities.containsKey(slot);
        }
    }
}
