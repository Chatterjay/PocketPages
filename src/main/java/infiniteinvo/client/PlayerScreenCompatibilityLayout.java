package infiniteinvo.client;

import infiniteinvo.inventory.ScrollableInventoryLayout;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;

/**
 * Shared anchors for the expanded player inventory and every integration that
 * attaches controls or a panel to an {@link AbstractContainerScreen}.
 */
public final class PlayerScreenCompatibilityLayout {
    private static final int IPN_HEADER_SHIFT = 50;
    private static final int RECIPE_BOOK_GAP = 2;
    private static final int RECIPE_BOOK_VIEWPORT_CENTER_OFFSET = 86;

    private PlayerScreenCompatibilityLayout() {
    }

    public static boolean isExtendedPlayerInventory(AbstractContainerScreen<?> screen) {
        return screen instanceof ScrollableInventoryScreen;
    }

    /**
     * Calculates the complete recipe-book layout from the expanded inventory
     * dimensions. Keeping this state in one place prevents init, input and
     * external panels from disagreeing about the window anchor.
     */
    public static RecipeBookLayout recipeBookLayout(int screenWidth, int inventoryWidth, boolean recipeBookVisible) {
        boolean narrow = isRecipeBookNarrow(screenWidth, inventoryWidth);
        int recipeBookLeft = recipeBookLeft(screenWidth, inventoryWidth);
        int inventoryLeft = recipeBookVisible && !narrow
                ? recipeBookLeft + RecipeBookComponent.IMAGE_WIDTH + RECIPE_BOOK_GAP
                : centeredLeft(screenWidth, inventoryWidth);
        int viewportWidth = narrow
                ? screenWidth
                : 2 * (recipeBookLeft + RECIPE_BOOK_VIEWPORT_CENTER_OFFSET) + RecipeBookComponent.IMAGE_WIDTH;
        return new RecipeBookLayout(narrow, inventoryLeft, viewportWidth);
    }

    public static int recipeBookButtonX(int guiLeft) {
        int afterPlayerPreview = guiLeft + ScrollableInventoryLayout.PLAYER_RENDER_RIGHT_X + 5;
        int afterOffhandStart = guiLeft + ScrollableInventoryLayout.OFFHAND_X - 1;
        return Math.max(afterPlayerPreview, afterOffhandStart);
    }

    public static int recipeBookButtonY(int guiTop) {
        int playerCenterY = guiTop + (ScrollableInventoryLayout.PLAYER_RENDER_TOP_Y
                + ScrollableInventoryLayout.PLAYER_RENDER_BOTTOM_Y) / 2;
        return playerCenterY - 1;
    }

    public static void positionRecipeBookButton(ImageButton button, int guiLeft, int guiTop) {
        button.setPosition(recipeBookButtonX(guiLeft), recipeBookButtonY(guiTop));
    }

    /** Returns the anchor for a panel placed immediately to the left of the player inventory. */
    public static PanelAnchor leftPanel(AbstractContainerScreen<?> screen, int panelWidth) {
        return new PanelAnchor(screen.getGuiLeft() - panelWidth, screen.getGuiTop());
    }

    /** Exposes the rendered inventory rectangle to integrations that need a stable anchor. */
    public static ScreenAnchor screenAnchor(AbstractContainerScreen<?> screen) {
        return new ScreenAnchor(screen.getGuiLeft(), screen.getGuiTop(), screen.getXSize(), screen.getYSize());
    }

    /**
     * IPN derives its tool positions from the screen rectangle on every
     * render. Moving that virtual rectangle upward keeps its tool cluster in
     * the reserved header rather than on top of storage slots and the scroll
     * bar.
     */
    public static int ipnGuiTop(AbstractContainerScreen<?> screen) {
        return screenAnchor(screen).top() - IPN_HEADER_SHIFT;
    }

    public static int ipnGuiLeft(AbstractContainerScreen<?> screen) {
        return screenAnchor(screen).left();
    }

    public static int ipnGuiWidth(AbstractContainerScreen<?> screen) {
        return screenAnchor(screen).width();
    }

    public static int ipnGuiHeight(AbstractContainerScreen<?> screen) {
        return screenAnchor(screen).height();
    }

    private static boolean isRecipeBookNarrow(int screenWidth, int inventoryWidth) {
        return screenWidth < inventoryWidth + RecipeBookComponent.IMAGE_WIDTH + RECIPE_BOOK_GAP;
    }

    private static int recipeBookLeft(int screenWidth, int inventoryWidth) {
        return (screenWidth - inventoryWidth - RecipeBookComponent.IMAGE_WIDTH - RECIPE_BOOK_GAP) / 2;
    }

    private static int centeredLeft(int screenWidth, int inventoryWidth) {
        return (screenWidth - inventoryWidth) / 2;
    }

    public record RecipeBookLayout(boolean narrow, int inventoryLeft, int viewportWidth) {
    }

    public record PanelAnchor(int left, int top) {
    }

    public record ScreenAnchor(int left, int top, int width, int height) {
    }
}
