package infiniteinvo.inventory;

import infiniteinvo.Config;
import infiniteinvo.network.CreativeInventoryPageDataPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Temporarily maps an extended inventory page onto the vanilla player inventory
 * while the creative inventory tab is open. This keeps vanilla creative slot
 * clicks authoritative instead of emulating them in a client overlay.
 */
public final class CreativeInventoryPaging {
    public static final int PAGE_SIZE = 27;
    private static final Map<UUID, Integer> ACTIVE_ROWS = new ConcurrentHashMap<>();
    private static final ThreadLocal<Integer> INTERNAL_MAPPING_WRITE_DEPTH = ThreadLocal.withInitial(() -> 0);

    private CreativeInventoryPaging() {
    }

    public static void selectRow(ServerPlayer player, int requestedRow) {
        InfiniteInventoryState state = InfiniteInventoryData.state(player);
        int previousRow = ACTIVE_ROWS.getOrDefault(player.getUUID(), 0);
        storeMappedPage(player, state, previousRow);

        int row = Math.max(0, Math.min(requestedRow, maxRow(state.size())));
        loadMappedPage(player, state, row);
        ACTIVE_ROWS.put(player.getUUID(), row);
        InfiniteInventoryData.markDirty(player);
        sendPage(player, row, state);
    }

    public static void restoreVanillaPage(ServerPlayer player) {
        Integer activeRow = ACTIVE_ROWS.remove(player.getUUID());
        if (activeRow == null) {
            return;
        }

        InfiniteInventoryState state = InfiniteInventoryData.state(player);
        storeMappedPage(player, state, activeRow);
        InfiniteInventoryData.dropLockedItems(player);
        // The client needs this page's final state before the physical slots are restored to page zero.
        sendPage(player, activeRow, state);
        loadMappedPage(player, state, 0);
        InfiniteInventoryData.markDirty(player);
        sendPage(player, 0, state);
    }

    public static void clearAll(ServerPlayer player) {
        InfiniteInventoryState state = InfiniteInventoryData.state(player);
        for (int slot = 0; slot < InfiniteInventoryData.getUnlocked(player); slot++) {
            state.setItem(slot, ItemStack.EMPTY);
        }
        InfiniteInventoryData.markDirty(player);
        int row = ACTIVE_ROWS.getOrDefault(player.getUUID(), 0);
        sendPage(player, row, state);
    }

    /** Drops locked stacks from the physical page currently mapped onto the vanilla inventory. */
    public static void dropMappedLockedItems(ServerPlayer player) {
        Integer row = ACTIVE_ROWS.get(player.getUUID());
        if (row == null) {
            return;
        }

        InfiniteInventoryState state = InfiniteInventoryData.state(player);
        int unlocked = InfiniteInventoryData.getUnlocked(player);
        int start = row * 9;
        boolean changed = false;

        for (int index = 0; index < PAGE_SIZE; index++) {
            int virtualSlot = start + index;
            if (virtualSlot < unlocked || virtualSlot >= state.size()) {
                continue;
            }

            ItemStack stack = player.getInventory().getItem(index + 9);
            if (stack.isEmpty()) {
                continue;
            }

            InfiniteInventoryData.dropAtPlayer(player, stack);
            final int inventorySlot = index + 9;
            runInternalMappingWrite(() -> player.getInventory().setItem(inventorySlot, ItemStack.EMPTY));
            state.setItem(virtualSlot, ItemStack.EMPTY);
            changed = true;
        }

        if (changed) {
            InfiniteInventoryData.markDirty(player);
            player.inventoryMenu.broadcastChanges();
            player.containerMenu.broadcastChanges();
            sendPage(player, row, state);
        }
    }

    /** Returns whether a native storage slot is writable while an extended page is mapped onto it. */
    public static boolean isMappedSlotUnlocked(ServerPlayer player, int inventorySlot) {
        Integer row = ACTIVE_ROWS.get(player.getUUID());
        if (row == null || inventorySlot < 9 || inventorySlot >= 36) {
            return true;
        }

        int virtualSlot = row * 9 + inventorySlot - 9;
        return virtualSlot < InfiniteInventoryData.getUnlocked(player);
    }

    public static boolean isMappedSlotLocked(ServerPlayer player, int inventorySlot) {
        return !isInternalMappingWrite() && !isMappedSlotUnlocked(player, inventorySlot);
    }

    public static boolean hasMappedLockedSlots(ServerPlayer player) {
        Integer row = ACTIVE_ROWS.get(player.getUUID());
        return row != null && row * 9 + PAGE_SIZE > InfiniteInventoryData.getUnlocked(player);
    }

    private static void storeMappedPage(ServerPlayer player, InfiniteInventoryState state, int row) {
        int start = row * 9;
        for (int i = 0; i < PAGE_SIZE && start + i < state.size(); i++) {
            state.setItem(start + i, player.getInventory().getItem(i + 9));
        }
    }

    private static void loadMappedPage(ServerPlayer player, InfiniteInventoryState state, int row) {
        runInternalMappingWrite(() -> {
            int start = row * 9;
            for (int i = 0; i < PAGE_SIZE; i++) {
                player.getInventory().setItem(i + 9, start + i < state.size() ? state.getItem(start + i).copy() : ItemStack.EMPTY);
            }
        });
        player.inventoryMenu.broadcastChanges();
    }

    private static boolean isInternalMappingWrite() {
        return INTERNAL_MAPPING_WRITE_DEPTH.get() > 0;
    }

    private static void runInternalMappingWrite(Runnable action) {
        INTERNAL_MAPPING_WRITE_DEPTH.set(INTERNAL_MAPPING_WRITE_DEPTH.get() + 1);
        try {
            action.run();
        } finally {
            int remaining = INTERNAL_MAPPING_WRITE_DEPTH.get() - 1;
            if (remaining == 0) {
                INTERNAL_MAPPING_WRITE_DEPTH.remove();
            } else {
                INTERNAL_MAPPING_WRITE_DEPTH.set(remaining);
            }
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

    private static void sendPage(ServerPlayer player, int row, InfiniteInventoryState state) {
        PacketDistributor.sendToPlayer(player, new CreativeInventoryPageDataPayload(
                row, InfiniteInventoryData.getUnlocked(player), page(state, row)));
    }

    public static int maxRow() {
        return maxRow(Config.totalExtraSlots());
    }

    private static int maxRow(int slots) {
        return Math.max(0, (int) Math.ceil(slots / 9.0D) - 3);
    }
}
