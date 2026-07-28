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
        PacketDistributor.sendToPlayer(player, new CreativeInventoryPageDataPayload(row, page(state, row)));
    }

    public static void restoreVanillaPage(ServerPlayer player) {
        Integer activeRow = ACTIVE_ROWS.remove(player.getUUID());
        if (activeRow == null) {
            return;
        }

        InfiniteInventoryState state = InfiniteInventoryData.state(player);
        storeMappedPage(player, state, activeRow);
        loadMappedPage(player, state, 0);
        InfiniteInventoryData.markDirty(player);
        PacketDistributor.sendToPlayer(player, new CreativeInventoryPageDataPayload(0, page(state, 0)));
    }

    private static void storeMappedPage(ServerPlayer player, InfiniteInventoryState state, int row) {
        int start = row * 9;
        for (int i = 0; i < PAGE_SIZE && start + i < state.size(); i++) {
            state.setItem(start + i, player.getInventory().getItem(i + 9));
        }
    }

    private static void loadMappedPage(ServerPlayer player, InfiniteInventoryState state, int row) {
        int start = row * 9;
        for (int i = 0; i < PAGE_SIZE; i++) {
            player.getInventory().setItem(i + 9, start + i < state.size() ? state.getItem(start + i).copy() : ItemStack.EMPTY);
        }
        player.inventoryMenu.broadcastChanges();
    }

    private static List<ItemStack> page(InfiniteInventoryState state, int row) {
        int start = row * 9;
        List<ItemStack> stacks = new ArrayList<>(PAGE_SIZE);
        for (int i = 0; i < PAGE_SIZE; i++) {
            stacks.add(start + i < state.size() ? state.getItem(start + i).copy() : ItemStack.EMPTY);
        }
        return stacks;
    }

    public static int maxRow() {
        return maxRow(Config.totalExtraSlots());
    }

    private static int maxRow(int slots) {
        return Math.max(0, (int) Math.ceil(slots / 9.0D) - 3);
    }
}
