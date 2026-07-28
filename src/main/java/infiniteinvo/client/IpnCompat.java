package infiniteinvo.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModList;

/** Optional, reflection-based bridge for IPN lock state in virtual inventory slots. */
final class IpnCompat {
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

    static boolean isVirtualSlotLocked(int virtualSlot) {
        return isVirtualSlotLocked(lockedInventorySlots(), virtualSlot);
    }

    static boolean isVirtualSlotLocked(Set<Integer> lockedSlots, int virtualSlot) {
        return lockedSlots.contains(lockKey(virtualSlot));
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
            int nativeSlot = index + 9;
            int virtualSlot = row * 9 + index;
            boolean locked = locks.remove(nativeSlot);
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
            int nativeSlot = index + 9;
            int virtualSlot = row * 9 + index;
            changed |= isVirtualSlotLocked(locks, virtualSlot)
                    ? locks.add(nativeSlot)
                    : locks.remove(nativeSlot);
        }
        if (changed) {
            saveLocks();
        }
    }

    static boolean toggleVirtualSlotLock(int virtualSlot) {
        Set<Integer> locks = mutableLockedSlots();
        if (locks == null) {
            return false;
        }

        int key = lockKey(virtualSlot);
        boolean locked = !locks.remove(key);
        if (locked) {
            locks.add(key);
        }
        saveLocks();
        return locked;
    }

    static void setVirtualSlotLocked(int virtualSlot, boolean locked) {
        Set<Integer> locks = mutableLockedSlots();
        if (locks == null) {
            return;
        }

        int key = lockKey(virtualSlot);
        if (locked ? locks.add(key) : locks.remove(key)) {
            saveLocks();
        }
    }

    static boolean isLockConfigurationModifierDown() {
        return isAvailable() && Screen.hasAltDown();
    }

    private static boolean isAvailable() {
        return !unavailable && ModList.get().isLoaded("inventoryprofilesnext");
    }

    private static int lockKey(int virtualSlot) {
        return VIRTUAL_SLOT_BASE + virtualSlot;
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
