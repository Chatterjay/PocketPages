package infiniteinvo.integration.client;

import io.wispforest.accessories.client.AccessoriesClient;
import infiniteinvo.client.ScrollableInventoryScreen;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector2i;

/** Opens the Accessories screen from InfiniteInvo's extended player inventory. */
public final class AccessoriesClientCompat {
    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath(
            "accessories", "textures/gui/accessories_open_icon.png");
    private static final ResourceLocation HOVERED_ICON = ResourceLocation.fromNamespaceAndPath(
            "accessories", "textures/gui/accessories_open_icon_hovered.png");

    private AccessoriesClientCompat() {
    }

    public static AbstractButton createButton(ScrollableInventoryScreen screen) {
        AccessoryEntryButton button = new AccessoryEntryButton();
        button.setTooltip(Tooltip.create(Component.translatable("accessories.open.screen")));
        button.updatePosition(screen);
        return button;
    }

    public static final class AccessoryEntryButton extends AbstractButton {
        private AccessoryEntryButton() {
            super(0, 0, 8, 8, Component.translatable("accessories.open.screen"));
        }

        public void updatePosition(ScrollableInventoryScreen screen) {
            Vector2i offset = io.wispforest.accessories.Accessories.config()
                    .screenOptions.inventoryButtonOffset();
            setPosition(screen.getGuiLeft() + offset.x, screen.getGuiTop() + offset.y);
        }

        @Override
        public void onPress() {
            AccessoriesClient.attemptToOpenScreen();
        }

        @Override
        protected void renderWidget(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY,
                                    float partialTick) {
            ResourceLocation texture = isHoveredOrFocused() ? HOVERED_ICON : ICON;
            graphics.blit(texture, getX(), getY(), 0, 0, getWidth(), getHeight(), getWidth(), getHeight());
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, getMessage());
        }
    }
}
