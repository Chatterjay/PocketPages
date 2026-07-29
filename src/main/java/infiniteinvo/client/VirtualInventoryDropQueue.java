package infiniteinvo.client;

import infiniteinvo.inventory.ScrollableInventoryMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

/**
 * Preserves IPN lock checks while forwarding standard throw clicks immediately.
 *
 * The first implementation queued one click per client tick to prevent stale
 * mirrored slots from overwriting picked-up items. The mirror is now reconciled
 * in real time, so delaying bulk actions only makes third-party inventory tools
 * visibly slow without adding any protection.
 */
public final class VirtualInventoryDropQueue {
    private static boolean dispatching;

    private VirtualInventoryDropQueue() {
    }

    /** Returns true when the original click was handled or intentionally blocked. */
    public static boolean enqueue(MultiPlayerGameMode gameMode, int containerId, int slotId, int button,
                                  ClickType clickType, Player player) {
        if (dispatching || clickType != ClickType.THROW || slotId < 0) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof ScrollableInventoryScreen)
                || minecraft.player != player
                || !(player.containerMenu instanceof ScrollableInventoryMenu menu)
                || menu.containerId != containerId
                || !menu.isValidSlotIndex(slotId)) {
            return false;
        }

        Slot slot = menu.getSlot(slotId);
        if (ScrollableInventoryScreen.isIpnVirtualSlotLocked(slot)) {
            return true;
        }

        dispatching = true;
        try {
            // Re-enter the vanilla method. The guard above lets this inner call bypass
            // the mixin and preserves Minecraft's normal packet/click implementation.
            gameMode.handleInventoryMouseClick(containerId, slotId, button, clickType, player);
        } finally {
            dispatching = false;
        }
        return true;
    }
}
