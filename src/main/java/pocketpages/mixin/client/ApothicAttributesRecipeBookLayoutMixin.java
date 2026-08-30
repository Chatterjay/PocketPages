package pocketpages.mixin.client;

import dev.shadowsoffire.apothic_attributes.ALConfig;
import dev.shadowsoffire.apothic_attributes.client.AttributesGui;
import dev.shadowsoffire.apothic_attributes.client.ButtonPlacement;
import pocketpages.client.PlayerScreenCompatibilityLayout;
import pocketpages.client.ScrollableInventoryScreen;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reapplies the extended layout after Apothic Attributes resets its parent screen. */
@Pseudo
@Mixin(targets = "dev.shadowsoffire.apothic_attributes.client.AttributesGui", remap = false)
abstract class ApothicAttributesRecipeBookLayoutMixin {
    @Shadow(remap = false) @Final private InventoryScreen parent;
    @Shadow(remap = false) @Final private ImageButton toggleBtn;
    @Shadow(remap = false) @Final private AttributesGui.HideUnchangedButton hideUnchangedBtn;
    @Shadow(remap = false) private int leftPos;
    @Shadow(remap = false) private int topPos;

    @Inject(method = "toggleVisibility", at = @At("TAIL"), remap = false, require = 0)
    private void pocketpages$restoreRecipeBookLayout(CallbackInfo callback) {
        if ((Object) parent instanceof ScrollableInventoryScreen screen) {
            screen.refreshCompatibilityLayout();
            PlayerScreenCompatibilityLayout.PanelAnchor panel = PlayerScreenCompatibilityLayout.leftPanel(
                    parent, AttributesGui.WIDTH);
            leftPos = panel.left();
            topPos = panel.top();
            ButtonPlacement.positionGuiButton(toggleBtn, ALConfig.attributesGuiButtonOffset,
                    parent.getGuiLeft(), parent.getGuiTop());
            hideUnchangedBtn.setPosition(leftPos + 7, topPos + 151);
        }
    }
}
