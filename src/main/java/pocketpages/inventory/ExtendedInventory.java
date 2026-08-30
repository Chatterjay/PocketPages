package pocketpages.inventory;

import pocketpages.Config;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Runtime bridge that gives the vanilla player inventory real extra slots. */
public final class ExtendedInventory {
    private static final int VANILLA_MAIN_SIZE = 36;
    private static final int VANILLA_STORAGE_SLOTS = VANILLA_MAIN_SIZE - 9;
    private static final ThreadLocal<Map<UUID, Integer>> LOADING_DEPTH = ThreadLocal.withInitial(HashMap::new);

    private ExtendedInventory() {
    }

    public static void ensure(Inventory inventory) {
        int required = Config.totalExtraSlots() + 9;
        if (inventory.items.size() >= required) {
            return;
        }
        NonNullList<ItemStack> expanded = NonNullList.withSize(required, ItemStack.EMPTY);
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            expanded.set(slot, inventory.items.get(slot));
        }
        ((ExtendedInventoryAccess) inventory).pocketpages$replaceItems(new TrackedItemList(inventory, expanded));
    }

    public static void initialize(Player player) {
        Inventory inventory = player.getInventory();
        ensure(inventory);
        beginLoading(player);
        try {
            PocketPagesInventoryState state = PocketPagesInventoryData.state(player);
            if (!state.isInitialized()) {
                state.initializeFromInventory(inventory);
                PocketPagesInventoryData.markDirty(player);
                return;
            }
            for (int slot = 0; slot < state.size(); slot++) {
                inventory.items.set(slot + 9, state.getItem(slot));
            }
        } finally {
            finishLoading(player);
        }
    }

    /** Prevents the transient clears performed by Inventory.load() from reaching SavedData. */
    public static void beginLoading(Player player) {
        UUID id = player.getUUID();
        Map<UUID, Integer> depths = LOADING_DEPTH.get();
        depths.put(id, depths.getOrDefault(id, 0) + 1);
    }

    /**
     * Reconciles vanilla's 27 main-storage slots, then restores only the
     * slots that vanilla intentionally does not serialize.
     */
    public static void restoreAfterVanillaLoad(Player player) {
        Inventory inventory = player.getInventory();
        ensure(inventory);
        PocketPagesInventoryState state = PocketPagesInventoryData.state(player);
        if (!state.isInitialized()) {
            state.initializeFromInventory(inventory);
            PocketPagesInventoryData.markDirty(player);
            return;
        }

        int vanillaSlots = Math.min(VANILLA_STORAGE_SLOTS, state.size());
        for (int slot = 0; slot < vanillaSlots; slot++) {
            state.setItemReference(slot, inventory.items.get(slot + 9));
        }
        for (int slot = vanillaSlots; slot < state.size(); slot++) {
            inventory.items.set(slot + 9, state.getItem(slot));
        }
        PocketPagesInventoryData.markDirty(player);
    }

    public static void finishLoading(Player player) {
        UUID id = player.getUUID();
        Map<UUID, Integer> depths = LOADING_DEPTH.get();
        int depth = depths.getOrDefault(id, 0);
        if (depth <= 1) {
            depths.remove(id);
        } else {
            depths.put(id, depth - 1);
        }
    }

    public static void syncSlot(Player player, int inventorySlot) {
        if (!(player instanceof ServerPlayer serverPlayer) || inventorySlot < 9) {
            return;
        }
        PocketPagesInventoryData.syncInventorySlot(serverPlayer, inventorySlot - 9);
    }

    public static void syncAll(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        PocketPagesInventoryState state = PocketPagesInventoryData.state(serverPlayer);
        ensure(player.getInventory());
        for (int slot = 0; slot < state.size(); slot++) {
            state.setItemReference(slot, player.getInventory().items.get(slot + 9));
        }
        PocketPagesInventoryData.markDirty(serverPlayer);
    }

    public static boolean isLoading(Player player) {
        return LOADING_DEPTH.get().getOrDefault(player.getUUID(), 0) > 0;
    }

    static void onItemsMutated(Inventory inventory, int inventorySlot) {
        if (inventorySlot >= 9 && inventorySlot < 9 + Config.totalExtraSlots()
                && !isLoading(inventory.player)) {
            syncSlot(inventory.player, inventorySlot);
        }
    }

}
