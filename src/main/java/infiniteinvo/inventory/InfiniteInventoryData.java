package infiniteinvo.inventory;

import infiniteinvo.Config;
import infiniteinvo.InfiniteInvo;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class InfiniteInventoryData {
    private static final String LEGACY_ROOT = InfiniteInvo.MODID;
    private static final Map<UUID, InfiniteInventoryState> CLIENT_CACHE = new HashMap<>();

    private InfiniteInventoryData() {
    }

    public static int getUnlocked(Player player) {
        if (player.getAbilities().instabuild) {
            return Config.totalExtraSlots();
        }
        return state(player).getUnlockedSlots();
    }

    public static void setUnlocked(Player player, int unlocked) {
        state(player).setUnlockedSlots(unlocked);
        markDirty(player);
    }

    public static boolean unlockOne(Player player) {
        int current = getUnlocked(player);
        if (current >= Config.totalExtraSlots()) {
            return false;
        }

        setUnlocked(player, current + 1);
        return true;
    }

    public static int nextUnlockCost(Player player) {
        return Config.unlockCost(getUnlocked(player));
    }

    public static InfiniteInventoryState state(Player player) {
        InfiniteInventoryState state;
        if (player instanceof ServerPlayer serverPlayer) {
            state = InfiniteInventorySavedData.get(serverPlayer.serverLevel()).getOrCreate(player.getUUID());
            if (player.hasData(InfiniteInvo.LEGACY_INVENTORY_STATE)) {
                state = player.removeData(InfiniteInvo.LEGACY_INVENTORY_STATE).copy();
                InfiniteInventorySavedData.get(serverPlayer.serverLevel()).replace(player.getUUID(), state);
            }
        } else {
            state = CLIENT_CACHE.computeIfAbsent(player.getUUID(), ignored -> new InfiniteInventoryState());
        }
        if (!state.isInitialized() && player.getPersistentData().contains(LEGACY_ROOT)) {
            state.importLegacy(player.getPersistentData().getCompound(LEGACY_ROOT), player.registryAccess());
            player.getPersistentData().remove(LEGACY_ROOT);
            markDirty(player);
        }
        return state;
    }

    public static void markDirty(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            InfiniteInventorySavedData.get(serverPlayer.serverLevel()).setDirty();
        }
    }

    public static void replaceState(ServerPlayer player, InfiniteInventoryState state) {
        InfiniteInventorySavedData.get(player.serverLevel()).replace(player.getUUID(), state);
    }

    /** Inserts only into the non-vanilla portion of the extended inventory. */
    public static int insertOverflow(ServerPlayer player, ItemStack remaining) {
        if (remaining.isEmpty()) {
            return 0;
        }

        InfiniteInventoryState state = state(player);
        int before = remaining.getCount();
        int unlocked = getUnlocked(player);

        for (int slot = 27; slot < unlocked && !remaining.isEmpty(); slot++) {
            ItemStack existing = state.getItem(slot);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, remaining)) {
                int moved = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
                if (moved > 0) {
                    existing.grow(moved);
                    state.setItem(slot, existing);
                    remaining.shrink(moved);
                }
            }
        }

        for (int slot = 27; slot < unlocked && !remaining.isEmpty(); slot++) {
            if (state.getItem(slot).isEmpty()) {
                int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                state.setItem(slot, remaining.copyWithCount(moved));
                remaining.shrink(moved);
            }
        }

        int inserted = before - remaining.getCount();
        if (inserted > 0) {
            markDirty(player);
        }
        return inserted;
    }

    /** Drops client-side menu state when leaving a world to prevent ghost slots in the next save. */
    public static void clearClientCache() {
        CLIENT_CACHE.clear();
    }
}
