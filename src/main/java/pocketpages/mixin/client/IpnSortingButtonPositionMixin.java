package pocketpages.mixin.client;

import pocketpages.client.PlayerScreenCompatibilityLayout;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Places IPN's player inventory tool cluster in the extended screen header. */
@Pseudo
@Mixin(targets = "org.anti_ad.mc.ipnext.gui.inject.SortingButtonCollectionWidget", remap = false)
abstract class IpnSortingButtonPositionMixin {
    @Shadow @Final private AbstractContainerScreen<?> screen;

    @Redirect(
            method = "postForegroundRender",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;getGuiLeft()I"),
            remap = false,
            require = 0)
    private int pocketpages$layoutLeft(AbstractContainerScreen<?> screen) {
        return PlayerScreenCompatibilityLayout.isExtendedPlayerInventory(screen)
                ? PlayerScreenCompatibilityLayout.ipnGuiLeft(screen)
                : screen.getGuiLeft();
    }

    @Redirect(
            method = "postForegroundRender",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;getGuiTop()I"),
            remap = false,
            require = 0)
    private int pocketpages$layoutTop(AbstractContainerScreen<?> screen) {
        return PlayerScreenCompatibilityLayout.isExtendedPlayerInventory(screen)
                ? PlayerScreenCompatibilityLayout.ipnGuiTop(screen)
                : screen.getGuiTop();
    }

    @Redirect(
            method = "postForegroundRender",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;getXSize()I"),
            remap = false,
            require = 0)
    private int pocketpages$layoutWidth(AbstractContainerScreen<?> screen) {
        return PlayerScreenCompatibilityLayout.isExtendedPlayerInventory(screen)
                ? PlayerScreenCompatibilityLayout.ipnGuiWidth(screen)
                : screen.getXSize();
    }

    @Redirect(
            method = "postForegroundRender",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;getYSize()I"),
            remap = false,
            require = 0)
    private int pocketpages$layoutHeight(AbstractContainerScreen<?> screen) {
        return PlayerScreenCompatibilityLayout.isExtendedPlayerInventory(screen)
                ? PlayerScreenCompatibilityLayout.ipnGuiHeight(screen)
                : screen.getYSize();
    }
}
