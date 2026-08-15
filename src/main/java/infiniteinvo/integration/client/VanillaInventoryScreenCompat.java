package infiniteinvo.integration.client;

import infiniteinvo.client.ScrollableInventoryScreen;
import infiniteinvo.inventory.ScrollableInventoryLayout;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.entity.player.Player;

/**
 * Presents InfiniteInvo's player window as a vanilla InventoryScreen to GUI
 * integrations. The delegated geometry keeps each integration's native
 * anchor and configuration intact.
 */
final class VanillaInventoryScreenCompat extends InventoryScreen {
    private final ScrollableInventoryScreen screen;

    VanillaInventoryScreenCompat(Player player, ScrollableInventoryScreen screen) {
        super(player);
        this.screen = screen;
        this.imageWidth = ScrollableInventoryLayout.IMAGE_WIDTH;
        this.imageHeight = ScrollableInventoryLayout.IMAGE_HEIGHT;
        this.width = screen.width;
        this.height = screen.height;
    }

    @Override
    public int getGuiLeft() {
        return screen.getGuiLeft();
    }

    @Override
    public int getGuiTop() {
        return screen.getGuiTop();
    }

    @Override
    public RecipeBookComponent getRecipeBookComponent() {
        return screen.getRecipeBookComponent();
    }
}
