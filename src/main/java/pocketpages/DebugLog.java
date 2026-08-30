package pocketpages;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Debug-only details for tracing virtual inventory synchronization. */
public final class DebugLog {
    private DebugLog() {
    }

    public static boolean enabled() {
        return Config.DEBUG_LOGGING.get();
    }

    public static void debug(String message, Object... arguments) {
        if (enabled()) {
            PocketPages.LOGGER.info("[Debug] " + message, arguments);
        }
    }

    /** Does not serialize the full components/NBT payload. */
    public static String stack(ItemStack stack) {
        if (!enabled()) {
            return "disabled";
        }
        if (stack == null || stack.isEmpty()) {
            return "empty";
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem())
                + " x" + stack.getCount()
                + " (componentsHash=" + stack.getComponents().hashCode() + ")";
    }
}
