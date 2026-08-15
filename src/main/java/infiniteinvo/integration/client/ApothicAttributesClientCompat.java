package infiniteinvo.integration.client;

import dev.shadowsoffire.apothic_attributes.ALConfig;
import dev.shadowsoffire.apothic_attributes.client.AttributesGui;
import dev.shadowsoffire.apothic_attributes.client.ButtonPlacement;
import infiniteinvo.client.ScrollableInventoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.ImageButton;

/** Bridges Apothic Attributes' native panel to the extended player inventory. */
public final class ApothicAttributesClientCompat {
    private ApothicAttributesClientCompat() {
    }

    public static AttributesPanel createPanel(ScrollableInventoryScreen screen,
                                               VanillaInventoryScreenCompat parent) {
        if (!ALConfig.enableAttributesGui || Minecraft.getInstance().player == null) {
            return null;
        }
        return new AttributesPanel(screen, parent);
    }

    public static final class AttributesPanel {
        private final InfiniteInvoAttributesGui attributesGui;

        private AttributesPanel(ScrollableInventoryScreen screen, VanillaInventoryScreenCompat parent) {
            this.attributesGui = new InfiniteInvoAttributesGui(parent, screen);
        }

        public ImageButton toggleButton() {
            return attributesGui.toggleButton();
        }

        public AbstractButton hideUnchangedButton() {
            return attributesGui.hideUnchangedButton();
        }

        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            attributesGui.render(graphics, mouseX, mouseY, partialTick);
        }

        public void updatePosition() {
            attributesGui.updatePosition();
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!attributesGui.isMouseOver(mouseX, mouseY) || hideUnchangedButton().isMouseOver(mouseX, mouseY)) {
                return false;
            }
            attributesGui.mouseClicked(mouseX, mouseY, button);
            return true;
        }

        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            return attributesGui.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            return attributesGui.isMouseOver(mouseX, mouseY)
                    && attributesGui.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

    }

    private static final class InfiniteInvoAttributesGui extends AttributesGui {
        private final VanillaInventoryScreenCompat parent;
        private final ScrollableInventoryScreen screen;

        private InfiniteInvoAttributesGui(VanillaInventoryScreenCompat parent, ScrollableInventoryScreen screen) {
            super(parent);
            this.parent = parent;
            this.screen = screen;
        }

        @Override
        public void toggleVisibility() {
            super.toggleVisibility();
            screen.refreshCompatibilityLayout();
            updatePosition();
        }

        private ImageButton toggleButton() {
            return toggleBtn;
        }

        private void updatePosition() {
            leftPos = parent.getGuiLeft() - 131;
            topPos = parent.getGuiTop();
            ButtonPlacement.positionGuiButton(toggleBtn, ALConfig.attributesGuiButtonOffset,
                    parent.getGuiLeft(), parent.getGuiTop());
            if (hideUnchangedBtn.visible) {
                hideUnchangedBtn.setPosition(leftPos + 7, topPos + 151);
            }
        }

        private AbstractButton hideUnchangedButton() {
            return hideUnchangedBtn;
        }
    }

}
