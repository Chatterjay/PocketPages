package infiniteinvo.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.util.Tuple;
import net.minecraft.world.inventory.RecipeBookMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets Curios use the normal side-by-side recipe book layout when space permits. */
@Pseudo
@Mixin(targets = "top.theillusivec4.curios.client.gui.CuriosScreen")
abstract class CuriosRecipeBookLayoutMixin {
    private static final int INVENTORY_WIDTH = 176;
    private static final int RECIPE_BOOK_WIDTH = RecipeBookComponent.IMAGE_WIDTH;
    private static final int RECIPE_BOOK_GAP = 2;
    private static final int SIDE_LAYOUT_MIN_WIDTH = 379;

    @Shadow @Final private RecipeBookComponent recipeBookGui;
    @Shadow private ImageButton recipeBookButton;
    @Shadow public boolean widthTooNarrow;
    @Shadow public int panelWidth;
    @Shadow public abstract void updateRenderButtons();

    @Redirect(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;init(IILnet/minecraft/client/Minecraft;ZLnet/minecraft/world/inventory/RecipeBookMenu;)V"))
    private void infiniteinvo$initializeRecipeBook(
            RecipeBookComponent recipeBook,
            int width,
            int height,
            Minecraft minecraft,
            boolean ignoredWidthTooNarrow,
            RecipeBookMenu<?, ?> menu) {
        boolean narrow = isNarrow(width);
        recipeBook.init(narrow ? width : recipeBookWidth(width), height, minecraft, narrow, menu);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void infiniteinvo$layoutRecipeBook(CallbackInfo callback) {
        updateLayout();
    }

    @Inject(method = "lambda$init$0", at = @At("TAIL"), require = 0, remap = false)
    private void infiniteinvo$updateRecipeBookLayoutAfterToggle(
            Tuple<Integer, Integer> offsets,
            Button button,
            CallbackInfo callback) {
        updateLayout();
    }

    private void updateLayout() {
        Screen screen = (Screen) (Object) this;
        if (isNarrow(screen.width)) {
            widthTooNarrow = true;
            return;
        }

        widthTooNarrow = false;
        int left = recipeBookGui.isVisible()
                ? recipeBookLeft(screen.width) + RECIPE_BOOK_WIDTH + RECIPE_BOOK_GAP + panelWidth
                : (screen.width - INVENTORY_WIDTH) / 2;
        ((AbstractContainerScreenAccessor) (Object) this).infiniteinvo$setLeftPos(left);
        if (recipeBookButton != null) {
            recipeBookButton.setPosition(left + 104, screen.height / 2 - 22);
        }
        updateRenderButtons();
    }

    private boolean isNarrow(int width) {
        return width < SIDE_LAYOUT_MIN_WIDTH + panelWidth;
    }

    private int recipeBookWidth(int width) {
        // RecipeBookComponent centers against this virtual width. Reserving the
        // Curios panel puts its left edge immediately beside the recipe book.
        return width - panelWidth - 2 * RECIPE_BOOK_GAP - 2;
    }

    private int recipeBookLeft(int width) {
        return (width - RECIPE_BOOK_WIDTH - RECIPE_BOOK_GAP - panelWidth - INVENTORY_WIDTH) / 2;
    }
}
