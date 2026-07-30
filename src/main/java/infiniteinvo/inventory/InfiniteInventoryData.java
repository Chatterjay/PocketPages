package infiniteinvo.inventory;

import infiniteinvo.Config;
import infiniteinvo.InfiniteInvo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class InfiniteInventoryData {
    private static final String LEGACY_ROOT = InfiniteInvo.MODID;
    private static final Map<UUID, InfiniteInventoryState> CLIENT_CACHE = new HashMap<>();

    private InfiniteInventoryData() {
    }

    public static int getUnlocked(Player player) {
        if (player.getAbilities().instabuild || !Config.requiresExperienceToUnlock()) {
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

    public static boolean canAffordNextUnlock(Player player) {
        if (player.getAbilities().instabuild || !Config.requiresExperienceToUnlock()) {
            return true;
        }
        return player.totalExperience >= Config.unlockCostInExperiencePoints(getUnlocked(player));
    }

    public static void chargeForUnlock(Player player, int alreadyUnlocked) {
        if (player.getAbilities().instabuild || !Config.requiresExperienceToUnlock()) {
            return;
        }
        player.giveExperiencePoints(-Config.unlockCostInExperiencePoints(alreadyUnlocked));
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

    /** Updates the client-side virtual store after a server page swap or close. */
    public static void applyClientPage(Player player, int row, int unlocked, List<ItemStack> stacks) {
        InfiniteInventoryState state = state(player);
        state.setUnlockedSlots(unlocked);
        int start = Math.max(0, row) * 9;
        for (int index = 0; index < stacks.size() && start + index < state.size(); index++) {
            state.setItem(start + index, stacks.get(index));
        }
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

    /**
     * Extends Inventory.clearOrCountMatchingItems to virtual slots which are
     * not currently represented by a vanilla inventory page.
     */
    public static int clearOrCountMatchingOverflow(ServerPlayer player, Predicate<ItemStack> predicate,
                                                    int maxCount, int excludedStart, int excludedEnd) {
        InfiniteInventoryState state = state(player);
        boolean simulate = maxCount == 0;
        int matched = 0;
        boolean changed = false;

        int firstVirtualSlot = excludedStart >= 0 ? 0 : 27;
        for (int slot = firstVirtualSlot; slot < getUnlocked(player); slot++) {
            if (slot >= excludedStart && slot < excludedEnd) {
                continue;
            }
            ItemStack stack = state.getItem(slot);
            if (stack.isEmpty() || !predicate.test(stack)) {
                continue;
            }
            if (simulate) {
                matched += stack.getCount();
                continue;
            }
            if (maxCount >= 0 && matched >= maxCount) {
                break;
            }

            int removed = maxCount < 0 ? stack.getCount() : Math.min(maxCount - matched, stack.getCount());
            matched += removed;
            if (removed == stack.getCount()) {
                state.setItem(slot, ItemStack.EMPTY);
            } else {
                state.setItem(slot, stack.copyWithCount(stack.getCount() - removed));
            }
            changed = true;
        }

        if (changed) {
            markDirty(player);
        }
        return matched;
    }

    /** Drops legacy stacks that remain in slots which have since become locked. */
    public static void dropLockedItems(ServerPlayer player) {
        InfiniteInventoryState state = state(player);
        int unlocked = getUnlocked(player);
        ServerLevel level = player.serverLevel();
        boolean changed = false;

        for (int slot = unlocked; slot < state.storedSize(); slot++) {
            ItemStack stack = state.getStoredItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            dropAtPlayer(player, stack);
            state.clearStoredItem(slot);
            changed = true;
        }
        if (changed) {
            markDirty(player);
        }
    }

    public static void dropAtPlayer(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        ItemEntity item = new ItemEntity(level, player.getX(), player.getY() + 0.5D, player.getZ(), stack.copy());
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }

    /** Drops client-side menu state when leaving a world to prevent ghost slots in the next save. */
    public static void clearClientCache() {
        CLIENT_CACHE.clear();
    }
}
