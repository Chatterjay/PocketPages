package pocketpages.inventory;

import pocketpages.Config;
import pocketpages.DebugLog;
import pocketpages.mixin.AbstractContainerMenuAccess;
import pocketpages.network.CreativeInventoryPageDataPayload;
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
    private static final Map<Slot, Integer> MAPPED_STORAGE_SLOTS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private CreativeInventoryPaging() {
    }

    public static void selectRow(ServerPlayer player, int requestedRow, int sessionId, int requestId,
                                 long knownRevision) {
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

        PocketPagesInventoryData.dropLegacyPlaceholderItems(player);
        PocketPagesInventoryState state = PocketPagesInventoryData.state(player);
        int row = Math.max(0, Math.min(requestedRow, maxRow(state.size())));
        mapMenu(player, player.inventoryMenu, row);
        if (player.containerMenu != player.inventoryMenu) {
            mapMenu(player, player.containerMenu, row);
        }

        ACTIVE_ROWS.put(playerId, row);
        ACTIVE_SESSIONS.put(playerId, sessionId);
        LAST_REQUESTS.put(playerId, requestId);
        int stateId = synchronizeMappedPageState(player.containerMenu);
        sendPage(player, row, sessionId, requestId, knownRevision, stateId, state);
        DebugLog.debug("[Paging] mapped real slots player={} row={} virtualStart={} stateId={} knownRevision={} revision={}",
                player.getName().getString(), row, row * 9, stateId, knownRevision, state.revision());
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
        PocketPagesInventoryData.dropLockedItems(player);
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

    /** Returns whether this menu currently has a real PocketPages page mapped into it. */
    public static boolean isPagedPlayerStorageMenu(AbstractContainerMenu menu) {
        return MENU_MAPPINGS.containsKey(menu);
    }

    /**
     * Returns a stable PocketPages storage index for one mapped menu slot,
     * or {@code -1} when the slot is not part of the current player page.
     */
    public static int getPagedStorageSlot(AbstractContainerMenu menu, Slot slot) {
        MenuMapping mapping = MENU_MAPPINGS.get(menu);
        return mapping == null ? -1 : mapping.storageSlot(slot);
    }

    /** Returns the current extended-inventory index for a mapped slot or {@code -1}. */
    public static int getMappedStorageSlot(Slot slot) {
        Integer mappedSlot = MAPPED_STORAGE_SLOTS.get(slot);
        if (mappedSlot != null) {
            return mappedSlot;
        }
        Slot target = slot instanceof WrappedSlotAccess wrapped ? wrapped.pocketpages$getTargetSlot() : slot;
        return target == slot ? -1 : MAPPED_STORAGE_SLOTS.getOrDefault(target, -1);
    }

    public static void clearAll(ServerPlayer player) {
        ExtendedInventory.ensure(player.getInventory());
        for (int slot = 0; slot < PocketPagesInventoryData.getUnlocked(player); slot++) {
            player.getInventory().items.set(slot + 9, ItemStack.EMPTY);
        }
        PocketPagesInventoryData.markDirty(player);
        refreshClientPage(player);
    }

    public static void refreshClientPage(ServerPlayer player) {
        int row = ACTIVE_ROWS.getOrDefault(player.getUUID(), 0);
        Integer session = ACTIVE_SESSIONS.get(player.getUUID());
        if (session != null) {
            PocketPagesInventoryState state = PocketPagesInventoryData.state(player);
            int stateId = synchronizeMappedPageState(player.containerMenu);
            sendPage(player, row, session, Integer.MAX_VALUE, -1L, stateId, state);
        } else {
            player.inventoryMenu.broadcastChanges();
            if (player.containerMenu != player.inventoryMenu) {
                player.containerMenu.broadcastChanges();
            }
        }
    }

    /** Updates the client-side native cache after a page payload remaps slots. */
    public static void synchronizeClientPageState(AbstractContainerMenu menu, int stateId) {
        MenuMapping mapping = MENU_MAPPINGS.get(menu);
        if (mapping == null) {
            return;
        }
        AbstractContainerMenuAccess access = (AbstractContainerMenuAccess) (Object) menu;
        for (MappedSlot mapped : mapping.slots) {
            ItemStack stack = mapped.slot().getItem().copy();
            if (mapped.slot().index < access.pocketpages$getLastSlots().size()) {
                access.pocketpages$getLastSlots().set(mapped.slot().index, stack.copy());
            }
            if (mapped.slot().index < access.pocketpages$getRemoteSlots().size()) {
                access.pocketpages$getRemoteSlots().set(mapped.slot().index, stack);
            }
        }
        access.pocketpages$setStateId(stateId);
    }

    private static int synchronizeMappedPageState(AbstractContainerMenu menu) {
        MenuMapping mapping = MENU_MAPPINGS.get(menu);
        if (mapping == null) {
            return menu.incrementStateId();
        }
        AbstractContainerMenuAccess access = (AbstractContainerMenuAccess) (Object) menu;
        for (MappedSlot mapped : mapping.slots) {
            ItemStack stack = mapped.slot().getItem().copy();
            if (mapped.slot().index < access.pocketpages$getLastSlots().size()) {
                access.pocketpages$getLastSlots().set(mapped.slot().index, stack.copy());
            }
            if (mapped.slot().index < access.pocketpages$getRemoteSlots().size()) {
                access.pocketpages$getRemoteSlots().set(mapped.slot().index, stack);
            }
        }
        return menu.incrementStateId();
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
        DebugLog.debug("[Paging] mapped menu player={} menu={} row={} storageRange={}..{} exposedRange={}..{}",
                player.getName().getString(), menu.getClass().getSimpleName(), row,
                row * 9, row * 9 + PAGE_SIZE - 1,
                row * 9 + 9, row * 9 + 9 + PAGE_SIZE - 1);
    }

    private static void restorePlayerMenus(Player player) {
        restoreMenu(player.inventoryMenu);
        if (player.containerMenu != player.inventoryMenu) {
            restoreMenu(player.containerMenu);
        }
    }

    private static List<ItemStack> page(PocketPagesInventoryState state, int row) {
        int start = row * 9;
        List<ItemStack> stacks = new ArrayList<>(PAGE_SIZE);
        for (int i = 0; i < PAGE_SIZE; i++) {
            stacks.add(start + i < state.size() ? state.getItem(start + i).copy() : ItemStack.EMPTY);
        }
        return stacks;
    }

    private static void sendPage(ServerPlayer player, int row, int sessionId, int requestId,
                                 long knownRevision, int stateId, PocketPagesInventoryState state) {
        List<ItemStack> stacks = knownRevision == state.revision()
                ? List.of()
                : page(state, row);
        PacketDistributor.sendToPlayer(player, new CreativeInventoryPageDataPayload(
                row, PocketPagesInventoryData.getUnlocked(player), sessionId, requestId,
                state.revision(), stateId, stacks));
    }

    public static int maxRow() {
        return maxRow(Config.totalExtraSlots());
    }

    private static int maxRow(int slots) {
        return Math.max(0, (int) Math.ceil(slots / 9.0D) - 3);
    }

    private record MappedSlot(Slot slot, Container originalContainer, int originalIndex, int offset) {
        void map(Inventory inventory, int row) {
            int virtualIndex = row * 9 + offset;
            MutableSlotAccess mutable = (MutableSlotAccess) slot;
            // Keep the remapped page indistinguishable from a normal player
            // inventory to integrations that inspect Slot.container directly.
            mutable.pocketpages$setContainer(inventory);
            mutable.pocketpages$setContainerSlot(virtualIndex + 9);
            MAPPED_STORAGE_SLOTS.put(slot, virtualIndex);
        }

        void restore() {
            MutableSlotAccess mutable = (MutableSlotAccess) slot;
            mutable.pocketpages$setContainer(originalContainer);
            mutable.pocketpages$setContainerSlot(originalIndex);
            MAPPED_STORAGE_SLOTS.remove(slot);
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
            for (Slot slot : menu.slots) {
                Slot target = slot instanceof WrappedSlotAccess wrapped ? wrapped.pocketpages$getTargetSlot() : slot;
                int index = target.getContainerSlot();
                if (target.container == inventory && index >= 9 && index < 36) {
                    slots.add(new MappedSlot(slot, inventory, index, index - 9));
                }
            }
            return slots.size() == PAGE_SIZE
                    ? new MenuMapping(inventory, List.copyOf(slots))
                    : null;
        }

        void map(int row) {
            slots.forEach(slot -> slot.map(inventory, row));
        }

        void restore() {
            slots.forEach(MappedSlot::restore);
        }

        boolean contains(Slot slot) {
            return identities.containsKey(slot);
        }

        int storageSlot(Slot slot) {
            return contains(slot) ? getMappedStorageSlot(slot) : -1;
        }
    }
}
