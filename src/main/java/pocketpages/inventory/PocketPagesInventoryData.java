package pocketpages.inventory;

import pocketpages.Config;
import pocketpages.PocketPages;
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

public final class PocketPagesInventoryData {
    private static final String LEGACY_ROOT = "infiniteinvo";
    private static final String CURRENT_ROOT = PocketPages.MODID;
    private static final Map<UUID, PocketPagesInventoryState> CLIENT_CACHE = new HashMap<>();

    private PocketPagesInventoryData() {
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

    public static PocketPagesInventoryState state(Player player) {
        PocketPagesInventoryState state;
        if (player instanceof ServerPlayer serverPlayer) {
            state = PocketPagesInventorySavedData.get(serverPlayer.serverLevel()).getOrCreate(player.getUUID());
            if (player.hasData(PocketPages.LEGACY_INVENTORY_STATE)) {
                state = player.removeData(PocketPages.LEGACY_INVENTORY_STATE).copy();
                PocketPagesInventorySavedData.get(serverPlayer.serverLevel()).replace(player.getUUID(), state);
            }
        } else {
            state = CLIENT_CACHE.computeIfAbsent(player.getUUID(), ignored -> new PocketPagesInventoryState());
        }
        String storedRoot = player.getPersistentData().contains(CURRENT_ROOT) ? CURRENT_ROOT : LEGACY_ROOT;
        if (!state.isInitialized() && player.getPersistentData().contains(storedRoot)) {
            state.importLegacy(player.getPersistentData().getCompound(storedRoot), player.registryAccess());
            player.getPersistentData().remove(storedRoot);
            markDirty(player);
        }
        return state;
    }

    public static void markDirty(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PocketPagesInventorySavedData data = PocketPagesInventorySavedData.get(serverPlayer.serverLevel());
            data.getOrCreate(player.getUUID()).touch();
            data.setDirty();
        }
    }

    public static void replaceState(ServerPlayer player, PocketPagesInventoryState state) {
        PocketPagesInventorySavedData.get(player.serverLevel()).replace(player.getUUID(), state);
    }

    public static void syncInventorySlot(ServerPlayer player, int stateSlot) {
        PocketPagesInventoryState state = state(player);
        if (stateSlot < 0 || stateSlot >= state.size() || stateSlot + 9 >= player.getInventory().items.size()) {
            return;
        }
        ExtendedInventory.ensure(player.getInventory());
        state.setItemReference(stateSlot, player.getInventory().items.get(stateSlot + 9));
        markDirty(player);
    }

    /** Updates the client-side virtual store after a server page swap or close. */
    public static void applyClientPage(Player player, int row, int unlocked, List<ItemStack> stacks) {
        ExtendedInventory.ensure(player.getInventory());
        PocketPagesInventoryState state = state(player);
        state.setUnlockedSlots(unlocked);
        int start = Math.max(0, row) * 9;
        for (int index = 0; index < stacks.size() && start + index < state.size(); index++) {
            ItemStack stack = stacks.get(index).copy();
            state.setItemReference(start + index, stack);
            player.getInventory().items.set(start + index + 9, stack);
        }
    }

    /** Inserts only into the non-vanilla portion of the extended inventory. */
    public static int insertOverflow(ServerPlayer player, ItemStack remaining) {
        if (remaining.isEmpty() || !canInsertIntoVirtualSlot(remaining)) {
            return 0;
        }

        // Check capacity before mutating the live remainder.
        ItemStack simulated = remaining.copy();
        int possible = insertOverflowInternal(player, simulated, true);
        if (possible <= 0) {
            return 0;
        }

        ItemStack committed = remaining.copy();
        int inserted = insertOverflowInternal(player, committed, false);
        if (inserted > 0) {
            remaining.shrink(inserted);
            markDirty(player);
        }
        return inserted;
    }

    public static boolean canInsertIntoVirtualSlot(ItemStack stack) {
        return !stack.is(PocketPages.LOCKED_SLOT.asItem());
    }

    private static int insertOverflowInternal(ServerPlayer player, ItemStack remaining, boolean simulate) {
        if (remaining.isEmpty() || !canInsertIntoVirtualSlot(remaining)) {
            return 0;
        }

        ExtendedInventory.ensure(player.getInventory());
        int before = remaining.getCount();
        int unlocked = getUnlocked(player);

        for (int slot = 27; slot < unlocked && !remaining.isEmpty(); slot++) {
            ItemStack existing = player.getInventory().items.get(slot + 9);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, remaining)) {
                int moved = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
                if (moved > 0) {
                    if (!simulate) {
                        player.getInventory().items.set(slot + 9, existing.copyWithCount(existing.getCount() + moved));
                    }
                    remaining.shrink(moved);
                }
            }
        }

        for (int slot = 27; slot < unlocked && !remaining.isEmpty(); slot++) {
            if (player.getInventory().items.get(slot + 9).isEmpty()) {
                int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                if (!simulate) {
                    player.getInventory().items.set(slot + 9, remaining.copyWithCount(moved));
                }
                remaining.shrink(moved);
            }
        }

        return before - remaining.getCount();
    }

    /**
     * Extends Inventory.clearOrCountMatchingItems to virtual slots which are
     * not currently represented by a vanilla inventory page.
     */
    public static int clearOrCountMatchingOverflow(ServerPlayer player, Predicate<ItemStack> predicate,
                                                    int maxCount, int excludedStart, int excludedEnd) {
        ExtendedInventory.ensure(player.getInventory());
        boolean simulate = maxCount == 0;
        int matched = 0;
        boolean changed = false;

        int firstVirtualSlot = excludedStart >= 0 ? 0 : 27;
        for (int slot = firstVirtualSlot; slot < getUnlocked(player); slot++) {
            if (slot >= excludedStart && slot < excludedEnd) {
                continue;
            }
            ItemStack stack = player.getInventory().items.get(slot + 9);
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
                player.getInventory().items.set(slot + 9, ItemStack.EMPTY);
            } else {
                player.getInventory().items.set(slot + 9, stack.copyWithCount(stack.getCount() - removed));
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
        PocketPagesInventoryState state = state(player);
        ExtendedInventory.ensure(player.getInventory());
        int unlocked = getUnlocked(player);
        ServerLevel level = player.serverLevel();
        boolean changed = false;

        for (int slot = unlocked; slot < state.storedSize(); slot++) {
            ItemStack stack = slot < state.size()
                    ? player.getInventory().items.get(slot + 9)
                    : state.getStoredItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            dropAtPlayer(player, stack);
            if (slot < state.size()) {
                player.getInventory().items.set(slot + 9, ItemStack.EMPTY);
            } else {
                state.clearStoredItem(slot);
            }
            changed = true;
        }
        if (changed) {
            markDirty(player);
        }
    }

    /** Ejects the internal placeholder item if an old save contains it. */
    public static void dropLegacyPlaceholderItems(ServerPlayer player) {
        PocketPagesInventoryState state = state(player);
        ExtendedInventory.ensure(player.getInventory());
        int unlocked = getUnlocked(player);
        boolean changed = false;

        for (int slot = 27; slot < unlocked && slot < state.size(); slot++) {
            ItemStack stack = player.getInventory().items.get(slot + 9);
            if (stack.isEmpty() || canInsertIntoVirtualSlot(stack)) {
                continue;
            }

            dropAtPlayer(player, stack);
            player.getInventory().items.set(slot + 9, ItemStack.EMPTY);
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
