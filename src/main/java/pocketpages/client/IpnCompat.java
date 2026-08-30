package pocketpages.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.neoforged.fml.ModList;

/** Optional, reflection-based bridge for IPN lock state in virtual inventory slots. */
public final class IpnCompat {
    private static final int VIRTUAL_SLOT_BASE = 1_000_000;
    private static Method getInstance;
    private static Method getLockedSlots;
    private static Field lockSlotsHandlerInstance;
    private static Method getStoredLockedSlots;
    private static Field lockSlotsLoaderInstance;
    private static Method saveLockedSlots;
    private static boolean unavailable;

    private IpnCompat() {
    }

    static Set<Integer> lockedInventorySlots() {
        if (!isAvailable()) {
            return Set.of();
        }

        try {
            if (getInstance == null) {
                Class<?> ipn = Class.forName("org.anti_ad.mc.ipn.api.access.IPN");
                getInstance = ipn.getMethod("getInstance");
                getLockedSlots = ipn.getMethod("getLockedSlots");
            }
            Object value = getLockedSlots.invoke(getInstance.invoke(null));
            if (value instanceof Collection<?> slots) {
                Set<Integer> result = new HashSet<>();
                for (Object slot : slots) {
                    if (slot instanceof Integer index) {
                        result.add(index);
                    }
                }
                return result;
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            unavailable = true;
        }
        return Set.of();
    }

    public static boolean isVirtualSlotLocked(int virtualSlot) {
        return isVirtualSlotLocked(lockedInventorySlots(), virtualSlot);
    }

    /**
     * Includes a lock toggled in a remapped container before that page is
     * captured into PocketPages's stable virtual lock key.
     */
    public static boolean isVirtualOrMappedSlotLocked(int virtualSlot) {
        Set<Integer> lockedSlots = lockedInventorySlots();
        return isVirtualSlotLocked(lockedSlots, virtualSlot)
                || lockedSlots.contains(mappedSlotKey(virtualSlot));
    }

    static boolean isVirtualSlotLocked(Set<Integer> lockedSlots, int virtualSlot) {
        return lockedSlots.contains(lockKey(virtualSlot));
    }

    /**
     * Extends IPN's own player-slot map while the scrollable inventory is open.
     * IPN then owns its normal rendering, configuration keys, and lock swipe
     * behavior for the virtual slots too.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Map appendVisibleVirtualSlotLocations(Map locations) {
        ScrollableInventoryScreen screen = ScrollableInventoryScreen.current();
        if (screen == null || locations.isEmpty()) {
            return locations;
        }

        Object samplePoint = locations.values().iterator().next();
        try {
            var pointConstructor = samplePoint.getClass().getConstructor(int.class, int.class);
            Map<Object, Object> result = new LinkedHashMap<>(locations);
            for (Slot slot : screen.visibleVirtualSlots()) {
                int virtualSlot = screen.storageSlot(slot);
                if (virtualSlot < screen.unlockedSlots()) {
                    // Replace the vanilla inventory key for this coordinate.
                    // Keeping both keys makes IPN toggle the native key first,
                    // so a stable virtual lock needs a second click to clear.
                    result.remove(mappedSlotKey(virtualSlot));
                    result.put(lockKey(virtualSlot), pointConstructor.newInstance(slot.x, slot.y));
                }
            }
            return result;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return locations;
        }
    }

    /**
     * Keeps IPN's per-slot refill indicator on the real vanilla hotbar.
     * Extended slots must not become refill targets just because paging maps
     * them to the player inventory at runtime.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Map restrictAutoRefillToHotbar(Map locations) {
        if (locations.isEmpty()) {
            return Map.of();
        }

        Set<SlotPosition> hotbarPositions = currentHotbarPositions();
        if (hotbarPositions.isEmpty()) {
            return Map.of();
        }

        Map<Object, Object> result = new LinkedHashMap<>();
        for (Object entryObject : locations.entrySet()) {
            Map.Entry entry = (Map.Entry) entryObject;
            if (entry.getKey() instanceof Number key && key.intValue() >= 0 && key.intValue() < 9
                    && isHotbarPosition(entry.getValue(), hotbarPositions)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private static Set<SlotPosition> currentHotbarPositions() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof AbstractContainerScreen<?> screen) || minecraft.player == null) {
            return Set.of();
        }

        Set<SlotPosition> positions = new HashSet<>();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container == minecraft.player.getInventory()
                    && slot.getContainerSlot() >= 0 && slot.getContainerSlot() < 9) {
                positions.add(new SlotPosition(slot.x, slot.y));
            }
        }
        return positions;
    }

    private static boolean isHotbarPosition(Object point, Set<SlotPosition> hotbarPositions) {
        if (point == null) {
            return false;
        }
        try {
            Method getX = point.getClass().getMethod("getX");
            Method getY = point.getClass().getMethod("getY");
            int x = ((Number) getX.invoke(point)).intValue();
            int y = ((Number) getY.invoke(point)).intValue();
            return hotbarPositions.contains(new SlotPosition(x, y));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private record SlotPosition(int x, int y) {
    }

    /** Converts old first-page IPN locks into stable virtual inventory locks. */
    static void migrateNativeStorageLocks() {
        Set<Integer> locks = mutableLockedSlots();
        if (locks == null) {
            return;
        }

        boolean changed = false;
        for (int index = 0; index < 27; index++) {
            if (locks.remove(index + 9)) {
                changed |= locks.add(lockKey(index));
            }
        }
        if (changed) {
            saveLocks();
        }
    }

    /** Saves IPN's current native player-storage locks back into the virtual page. */
    static void captureMappedPageLocks(int row) {
        Set<Integer> locks = mutableLockedSlots();
        if (locks == null) {
            return;
        }

        boolean changed = false;
        for (int index = 0; index < 27; index++) {
            int virtualSlot = row * 9 + index;
            boolean locked = locks.remove(mappedSlotKey(virtualSlot));
            changed |= locked ? locks.add(lockKey(virtualSlot)) : locks.remove(lockKey(virtualSlot));
        }
        if (changed) {
            saveLocks();
        }
    }

    /** Projects stable virtual locks onto the native slots IPN sees on the current page. */
    static void applyMappedPageLocks(int row) {
        Set<Integer> locks = mutableLockedSlots();
        if (locks == null) {
            return;
        }

        boolean changed = false;
        for (int index = 0; index < 27; index++) {
            int virtualSlot = row * 9 + index;
            changed |= isVirtualSlotLocked(locks, virtualSlot)
                    ? locks.add(mappedSlotKey(virtualSlot))
                    : locks.remove(mappedSlotKey(virtualSlot));
        }
        if (changed) {
            saveLocks();
        }
    }

    private static boolean isAvailable() {
        return !unavailable && ModList.get().isLoaded("inventoryprofilesnext");
    }

    private static int lockKey(int virtualSlot) {
        return VIRTUAL_SLOT_BASE + virtualSlot;
    }

    private static int mappedSlotKey(int virtualSlot) {
        return virtualSlot + 9;
    }

    @SuppressWarnings("unchecked")
    private static Set<Integer> mutableLockedSlots() {
        if (!isAvailable()) {
            return null;
        }

        try {
            if (getStoredLockedSlots == null) {
                Class<?> handler = Class.forName("org.anti_ad.mc.ipnext.event.LockSlotsHandler");
                lockSlotsHandlerInstance = handler.getField("INSTANCE");
                getStoredLockedSlots = handler.getMethod("getLockedInvSlotsStoredValue");

                Class<?> loader = Class.forName("org.anti_ad.mc.ipnext.parser.LockSlotsLoader");
                lockSlotsLoaderInstance = loader.getField("INSTANCE");
                saveLockedSlots = loader.getMethod("save");
            }
            Object locks = getStoredLockedSlots.invoke(lockSlotsHandlerInstance.get(null));
            return locks instanceof Set<?> ? (Set<Integer>) locks : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            unavailable = true;
            return null;
        }
    }

    private static void saveLocks() {
        try {
            if (saveLockedSlots != null && lockSlotsLoaderInstance != null) {
                saveLockedSlots.invoke(lockSlotsLoaderInstance.get(null));
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            unavailable = true;
        }
    }
}
