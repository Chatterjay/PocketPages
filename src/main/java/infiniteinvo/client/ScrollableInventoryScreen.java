package infiniteinvo.client;

import infiniteinvo.InfiniteInvo;
import infiniteinvo.inventory.ScrollableInventoryLayout;
import infiniteinvo.inventory.ScrollableInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class ScrollableInventoryScreen extends AbstractContainerScreen<ScrollableInventoryMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(InfiniteInvo.MODID, "textures/gui/adjustable_gui.png");
    private Button unlockButton;
    private float xMouse;
    private float yMouse;
    private boolean draggingScrollbar;

    public ScrollableInventoryScreen(ScrollableInventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = ScrollableInventoryLayout.IMAGE_WIDTH;
        this.imageHeight = ScrollableInventoryLayout.IMAGE_HEIGHT;
        this.titleLabelX = 87;
        this.titleLabelY = 32;
    }

    @Override
    protected void init() {
        super.init();
        if (minecraft != null && minecraft.player != null && minecraft.player.getAbilities().instabuild) {
            return;
        }
        int x = leftPos + ScrollableInventoryLayout.UNLOCK_BUTTON_X;
        int y = topPos + ScrollableInventoryLayout.UNLOCK_BUTTON_Y;
        unlockButton = Button.builder(Component.literal(""), b -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            }
        }).bounds(x, y, 74, 18).build();
        addRenderableWidget(unlockButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        xMouse = mouseX;
        yMouse = mouseY;
        if (unlockButton != null && minecraft != null && minecraft.player != null) {
            int cost = menu.getNextUnlockCost();
            boolean canUnlock = menu.getUnlockedSlots() < menu.getStore().getContainerSize()
                    && (minecraft.player.getAbilities().instabuild || minecraft.player.experienceLevel >= cost);
            unlockButton.active = canUnlock;
            unlockButton.setMessage(canUnlock
                    ? Component.translatable("infiniteinvo.unlockslot")
                    : Component.literal(minecraft.player.experienceLevel + " / " + cost + " XP"));
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        g.blit(TEXTURE, left, top, 0, 0, 169, 137);
        for (int col = 0; col < ScrollableInventoryLayout.EXTRA_COLUMNS; col++) {
            g.blit(TEXTURE, left + 169 + ScrollableInventoryLayout.SLOT_SIZE * col, top, 169, 0, 18, 137);
        }
        for (int row = 0; row < ScrollableInventoryLayout.EXTRA_ROWS; row++) {
            g.blit(TEXTURE, left, top + 137 + ScrollableInventoryLayout.SLOT_SIZE * row, 0, 119, 169, 18);
        }
        for (int col = 0; col < ScrollableInventoryLayout.EXTRA_COLUMNS; col++) {
            for (int row = 0; row < ScrollableInventoryLayout.EXTRA_ROWS; row++) {
                g.blit(TEXTURE, left + 169 + ScrollableInventoryLayout.SLOT_SIZE * col,
                        top + 137 + ScrollableInventoryLayout.SLOT_SIZE * row, 7, 83, 18, 18);
            }
        }
        int scrollbarWidth = menu.getMaxScroll() > 0 ? 0 : 8;
        int scrollbarX = left + ScrollableInventoryLayout.SCROLL_X;
        g.blit(TEXTURE, scrollbarX, top, 187, 0, 2, 119);
        g.blit(TEXTURE, scrollbarX + 2, top, 189 + scrollbarWidth, 0, 13 - scrollbarWidth, 119);
        for (int row = 0; row < ScrollableInventoryLayout.EXTRA_ROWS; row++) {
            int y = top + 119 + ScrollableInventoryLayout.SLOT_SIZE * row;
            g.blit(TEXTURE, scrollbarX, y, 187, 101, 2, 18);
            g.blit(TEXTURE, scrollbarX + 2, y, 189 + scrollbarWidth, 101, 13 - scrollbarWidth, 18);
        }
        int scrollbarBottomY = top + 119 + ScrollableInventoryLayout.EXTRA_ROWS * ScrollableInventoryLayout.SLOT_SIZE;
        g.blit(TEXTURE, scrollbarX, scrollbarBottomY, 187, 119, 2, 18);
        g.blit(TEXTURE, scrollbarX + 2, scrollbarBottomY, 189 + scrollbarWidth, 119, 13 - scrollbarWidth, 18);

        int hotbarBackgroundY = top + 137 + ScrollableInventoryLayout.EXTRA_ROWS * ScrollableInventoryLayout.SLOT_SIZE;
        g.blit(TEXTURE, left, hotbarBackgroundY, 0, 137, 169, 29);
        for (int col = 0; col < ScrollableInventoryLayout.EXTRA_COLUMNS; col++) {
            g.blit(TEXTURE, left + 169 + ScrollableInventoryLayout.SLOT_SIZE * col, hotbarBackgroundY, 169, 137, 18, 29);
        }
        g.blit(TEXTURE, scrollbarX, hotbarBackgroundY, 187 + scrollbarWidth, 137, 16 - scrollbarWidth, 29);

        if (minecraft != null && minecraft.player != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, left + 26, top + 8, left + 75, top + 78, 30, 0.0625F, xMouse, yMouse, minecraft.player);
        }

    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, Component.translatable("container.crafting"), 87, 32, 4210752, false);
        if (menu.getMaxScroll() > 0) {
            int knobY = ScrollableInventoryLayout.SCROLL_Y
                    + Math.round((float) menu.getScrollPos() / menu.getMaxScroll() * ScrollableInventoryLayout.SCROLL_KNOB_TRAVEL);
            guiGraphics.blit(TEXTURE, ScrollableInventoryLayout.SCROLL_KNOB_X, knobY, 60, 166, 8, 8);
        }

        for (int row = 0; row < menu.getVisibleRows(); row++) {
            for (int col = 0; col < menu.getVisibleColumns(); col++) {
                int slotIndex = col + (row + menu.getScrollPos()) * menu.getVisibleColumns();
                if (slotIndex >= menu.getStore().getContainerSize()) {
                    guiGraphics.blit(TEXTURE,
                            ScrollableInventoryLayout.GRID_BACKGROUND_X + col * ScrollableInventoryLayout.SLOT_SIZE,
                            ScrollableInventoryLayout.GRID_BACKGROUND_Y + row * ScrollableInventoryLayout.SLOT_SIZE,
                            0, 166, 18, 18);
                } else if (slotIndex >= menu.getUnlockedSlots()) {
                    guiGraphics.blit(TEXTURE,
                            ScrollableInventoryLayout.GRID_BACKGROUND_X + col * ScrollableInventoryLayout.SLOT_SIZE,
                            ScrollableInventoryLayout.GRID_BACKGROUND_Y + row * ScrollableInventoryLayout.SLOT_SIZE,
                            18, 166, 18, 18);
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0 && minecraft != null && minecraft.gameMode != null && menu.getMaxScroll() > 0) {
            int id = scrollY > 0 ? 1 : 2;
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
            menu.updateScroll(scrollY > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOverScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            setScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar && button == 0) {
            setScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        return menu.getMaxScroll() > 0
                && mouseX >= leftPos + ScrollableInventoryLayout.SCROLL_X
                && mouseX < leftPos + ScrollableInventoryLayout.SCROLL_X + 15
                && mouseY >= topPos + ScrollableInventoryLayout.SCROLL_Y
                && mouseY < topPos + ScrollableInventoryLayout.SCROLL_Y + ScrollableInventoryLayout.SCROLL_HEIGHT;
    }

    private void setScrollFromMouse(double mouseY) {
        int target = (int) Math.round((mouseY - topPos - ScrollableInventoryLayout.SCROLL_Y - 4)
                / ScrollableInventoryLayout.SCROLL_KNOB_TRAVEL * menu.getMaxScroll());
        target = Math.max(0, Math.min(menu.getMaxScroll(), target));
        int delta = target - menu.getScrollPos();
        if (delta == 0 || minecraft == null || minecraft.gameMode == null) {
            return;
        }
        int buttonId = delta > 0 ? 2 : 1;
        for (int i = 0; i < Math.abs(delta); i++) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
        menu.updateScroll(delta);
    }
}
