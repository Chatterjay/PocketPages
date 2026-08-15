package infiniteinvo.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/**
 * Geometry overrides for integrations that cannot use the player inventory's
 * native coordinates directly.
 */
public final class PlayerScreenCompatibilityLayout {
    private static final int IPN_HEADER_SHIFT = 50;

    private PlayerScreenCompatibilityLayout() {
    }

    public static boolean isExtendedPlayerInventory(AbstractContainerScreen<?> screen) {
        return screen instanceof ScrollableInventoryScreen;
    }

    /**
     * IPN derives its tool positions from the screen rectangle on every
     * render. Moving that virtual rectangle upward keeps its tool cluster in
     * the reserved header rather than on top of storage slots and the scroll
     * bar.
     */
    public static int ipnGuiTop(AbstractContainerScreen<?> screen) {
        return screen.getGuiTop() - IPN_HEADER_SHIFT;
    }

    public static int ipnGuiLeft(AbstractContainerScreen<?> screen) {
        return screen.getGuiLeft();
    }

    public static int ipnGuiWidth(AbstractContainerScreen<?> screen) {
        return screen.getXSize();
    }

    public static int ipnGuiHeight(AbstractContainerScreen<?> screen) {
        return screen.getYSize();
    }
}
