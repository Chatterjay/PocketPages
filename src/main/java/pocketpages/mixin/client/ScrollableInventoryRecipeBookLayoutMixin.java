package pocketpages.mixin.client;

import pocketpages.client.ScrollableInventoryScreen;
import pocketpages.client.PlayerScreenCompatibilityLayout;
import pocketpages.inventory.ScrollableInventoryLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.inventory.RecipeBookMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Aligns the vanilla recipe book with PocketPages's wider player inventory. */
@Mixin(InventoryScreen.class)
abstract class ScrollableInventoryRecipeBookLayoutMixin {
    @Shadow private boolean widthTooNarrow;

    @Redirect(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;init(IILnet/minecraft/client/Minecraft;ZLnet/minecraft/world/inventory/RecipeBookMenu;)V"))
    private void pocketpages$initializeRecipeBook(
            RecipeBookComponent recipeBook,
            int screenWidth,
            int screenHeight,
            Minecraft minecraft,
            boolean ignoredNarrow,
            RecipeBookMenu<?, ?> menu) {
        if (!pocketpages$isExtendedInventory()) {
            recipeBook.init(screenWidth, screenHeight, minecraft, ignoredNarrow, menu);
            return;
        }

        PlayerScreenCompatibilityLayout.RecipeBookLayout layout = PlayerScreenCompatibilityLayout.recipeBookLayout(
                screenWidth, ScrollableInventoryLayout.IMAGE_WIDTH, recipeBook.isVisible());
        widthTooNarrow = layout.narrow();
        recipeBook.init(layout.viewportWidth(), screenHeight, minecraft, layout.narrow(), menu);
    }

    @Redirect(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;updateScreenPosition(II)I"))
    private int pocketpages$positionExtendedInventory(
            RecipeBookComponent recipeBook,
            int screenWidth,
            int imageWidth) {
        return pocketpages$recipeBookScreenPosition(recipeBook, screenWidth, imageWidth);
    }

    @Redirect(
            method = "lambda$init$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;updateScreenPosition(II)I"),
            require = 0)
    private int pocketpages$positionExtendedInventoryAfterToggle(
            RecipeBookComponent recipeBook,
            int screenWidth,
            int imageWidth) {
        return pocketpages$recipeBookScreenPosition(recipeBook, screenWidth, imageWidth);
    }

    private int pocketpages$recipeBookScreenPosition(
            RecipeBookComponent recipeBook,
            int screenWidth,
            int imageWidth) {
        if (!pocketpages$isExtendedInventory()) {
            return recipeBook.updateScreenPosition(screenWidth, imageWidth);
        }

        return PlayerScreenCompatibilityLayout.recipeBookLayout(
                screenWidth, imageWidth, recipeBook.isVisible()).inventoryLeft();
    }

    @ModifyArgs(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ImageButton;<init>(IIIILnet/minecraft/client/gui/components/WidgetSprites;Lnet/minecraft/client/gui/components/Button$OnPress;)V"))
    private void pocketpages$positionRecipeBookButton(Args args) {
        if (!pocketpages$isExtendedInventory()) {
            return;
        }
        args.set(0, PlayerScreenCompatibilityLayout.recipeBookButtonX(pocketpages$guiLeft()));
        args.set(1, PlayerScreenCompatibilityLayout.recipeBookButtonY(pocketpages$guiTop()));
        args.set(2, ScrollableInventoryLayout.RECIPE_BOOK_BUTTON_WIDTH);
        args.set(3, ScrollableInventoryLayout.RECIPE_BOOK_BUTTON_HEIGHT);
    }

    @ModifyArgs(
            method = "lambda$init$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ImageButton;setPosition(II)V"),
            require = 0)
    private void pocketpages$repositionRecipeBookButtonAfterToggle(Args args) {
        if (!pocketpages$isExtendedInventory()) {
            return;
        }
        args.set(0, PlayerScreenCompatibilityLayout.recipeBookButtonX(pocketpages$guiLeft()));
        args.set(1, PlayerScreenCompatibilityLayout.recipeBookButtonY(pocketpages$guiTop()));
    }

    /**
     * The vanilla callback writes leftPos and the button position after
     * toggling the recipe book. Keep a final correction after that write so
     * optional screen mixins cannot leave the expanded inventory at vanilla
     * coordinates.
     */
    @Inject(method = "lambda$init$0", at = @At("TAIL"), require = 0)
    private void pocketpages$restoreExtendedLayoutAfterToggle(
            Button button, org.spongepowered.asm.mixin.injection.callback.CallbackInfo callback) {
        if ((Object) this instanceof ScrollableInventoryScreen screen) {
            screen.refreshCompatibilityLayout(button instanceof ImageButton imageButton ? imageButton : null);
        }
    }

    private boolean pocketpages$isExtendedInventory() {
        return (Object) this instanceof ScrollableInventoryScreen;
    }

    private int pocketpages$guiLeft() {
        return ((InventoryScreen) (Object) this).getGuiLeft();
    }

    private int pocketpages$guiTop() {
        return ((InventoryScreen) (Object) this).getGuiTop();
    }
}
