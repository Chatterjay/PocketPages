package infiniteinvo.mixin.client;

import infiniteinvo.client.ScrollableInventoryScreen;
import infiniteinvo.client.PlayerScreenCompatibilityLayout;
import infiniteinvo.inventory.ScrollableInventoryLayout;
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

/** Aligns the vanilla recipe book with InfiniteInvo's wider player inventory. */
@Mixin(InventoryScreen.class)
abstract class ScrollableInventoryRecipeBookLayoutMixin {
    @Shadow private boolean widthTooNarrow;

    @Redirect(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;init(IILnet/minecraft/client/Minecraft;ZLnet/minecraft/world/inventory/RecipeBookMenu;)V"))
    private void infiniteinvo$initializeRecipeBook(
            RecipeBookComponent recipeBook,
            int screenWidth,
            int screenHeight,
            Minecraft minecraft,
            boolean ignoredNarrow,
            RecipeBookMenu<?, ?> menu) {
        if (!infiniteinvo$isExtendedInventory()) {
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
    private int infiniteinvo$positionExtendedInventory(
            RecipeBookComponent recipeBook,
            int screenWidth,
            int imageWidth) {
        return infiniteinvo$recipeBookScreenPosition(recipeBook, screenWidth, imageWidth);
    }

    @Redirect(
            method = "lambda$init$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;updateScreenPosition(II)I"),
            require = 0)
    private int infiniteinvo$positionExtendedInventoryAfterToggle(
            RecipeBookComponent recipeBook,
            int screenWidth,
            int imageWidth) {
        return infiniteinvo$recipeBookScreenPosition(recipeBook, screenWidth, imageWidth);
    }

    private int infiniteinvo$recipeBookScreenPosition(
            RecipeBookComponent recipeBook,
            int screenWidth,
            int imageWidth) {
        if (!infiniteinvo$isExtendedInventory()) {
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
    private void infiniteinvo$positionRecipeBookButton(Args args) {
        if (!infiniteinvo$isExtendedInventory()) {
            return;
        }
        args.set(0, PlayerScreenCompatibilityLayout.recipeBookButtonX(infiniteinvo$guiLeft()));
        args.set(1, PlayerScreenCompatibilityLayout.recipeBookButtonY(infiniteinvo$guiTop()));
        args.set(2, ScrollableInventoryLayout.RECIPE_BOOK_BUTTON_WIDTH);
        args.set(3, ScrollableInventoryLayout.RECIPE_BOOK_BUTTON_HEIGHT);
    }

    @ModifyArgs(
            method = "lambda$init$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ImageButton;setPosition(II)V"),
            require = 0)
    private void infiniteinvo$repositionRecipeBookButtonAfterToggle(Args args) {
        if (!infiniteinvo$isExtendedInventory()) {
            return;
        }
        args.set(0, PlayerScreenCompatibilityLayout.recipeBookButtonX(infiniteinvo$guiLeft()));
        args.set(1, PlayerScreenCompatibilityLayout.recipeBookButtonY(infiniteinvo$guiTop()));
    }

    /**
     * The vanilla callback writes leftPos and the button position after
     * toggling the recipe book. Keep a final correction after that write so
     * optional screen mixins cannot leave the expanded inventory at vanilla
     * coordinates.
     */
    @Inject(method = "lambda$init$0", at = @At("TAIL"), require = 0)
    private void infiniteinvo$restoreExtendedLayoutAfterToggle(
            Button button, org.spongepowered.asm.mixin.injection.callback.CallbackInfo callback) {
        if ((Object) this instanceof ScrollableInventoryScreen screen) {
            screen.refreshCompatibilityLayout(button instanceof ImageButton imageButton ? imageButton : null);
        }
    }

    private boolean infiniteinvo$isExtendedInventory() {
        return (Object) this instanceof ScrollableInventoryScreen;
    }

    private int infiniteinvo$guiLeft() {
        return ((InventoryScreen) (Object) this).getGuiLeft();
    }

    private int infiniteinvo$guiTop() {
        return ((InventoryScreen) (Object) this).getGuiTop();
    }
}
