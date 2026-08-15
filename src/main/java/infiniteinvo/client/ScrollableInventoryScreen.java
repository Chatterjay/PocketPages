package infiniteinvo.client;

import infiniteinvo.Config;
import infiniteinvo.DebugLog;
import infiniteinvo.InfiniteInvo;
import infiniteinvo.inventory.InfiniteInventoryData;
import infiniteinvo.inventory.ScrollableInventoryLayout;
import infiniteinvo.inventory.ScrollableInventoryMenu;
import infiniteinvo.mixin.client.AbstractContainerScreenMenuAccessor;
import infiniteinvo.network.ScrollableInventoryPageRequestPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.List;

/**
 * Keeps the vanilla player screen as the public GUI type.  Third-party mods
 * can therefore continue to inject their normal InventoryScreen controls.
 */
public final class ScrollableInventoryScreen extends InventoryScreen {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(InfiniteInvo.MODID, "textures/gui/adjustable_gui.png");
    private final ScrollableInventoryMenu menu;
    private Button unlockButton;
    private ImageButton recipeBookButton;
    private float xMouse;
    private float yMouse;
    private boolean draggingScrollbar;
    private int requestedScrollPos;
    private int inFlightScrollPos = -1;
    private int nextScrollRequestId;

    public ScrollableInventoryScreen(ScrollableInventoryMenu menu, Inventory inventory, Component title) {
        super(inventory.player);
        this.menu = menu;
        ((AbstractContainerScreenMenuAccessor) (Object) this).infiniteinvo$setMenu(menu);
        this.imageWidth = ScrollableInventoryLayout.IMAGE_WIDTH;
        this.imageHeight = ScrollableInventoryLayout.IMAGE_HEIGHT;
        this.titleLabelX = ScrollableInventoryLayout.CRAFTING_LABEL_X;
        this.titleLabelY = ScrollableInventoryLayout.CRAFTING_LABEL_Y;
        this.requestedScrollPos = menu.getScrollPos();
    }

    @Override
    protected void init() {
        super.init();
        recipeBookButton = children().stream()
                .filter(ImageButton.class::isInstance)
                .map(ImageButton.class::cast)
                .findFirst()
                .orElse(null);
        IpnCompat.migrateNativeStorageLocks();
        if (minecraft != null && minecraft.player != null
                && (minecraft.player.getAbilities().instabuild || !Config.requiresExperienceToUnlock())) {
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
        // Optional inventory panels may update the vanilla anchor during input.
        // Restore our wider layout before vanilla renders its slots and recipe book.
        refreshCompatibilityLayout();
        xMouse = mouseX;
        yMouse = mouseY;
        if (unlockButton != null && minecraft != null && minecraft.player != null) {
            unlockButton.setPosition(leftPos + ScrollableInventoryLayout.UNLOCK_BUTTON_X,
                    topPos + ScrollableInventoryLayout.UNLOCK_BUTTON_Y);
            int unlockedSlots = menu.getUnlockedSlots();
            int totalSlots = menu.getStore().getContainerSize();
            int cost = menu.getNextUnlockCost();
            boolean hasLockedSlots = unlockedSlots < totalSlots;
            boolean canUnlock = hasLockedSlots && InfiniteInventoryData.canAffordNextUnlock(minecraft.player);
            unlockButton.active = canUnlock;
            int available = Config.usesExperiencePoints() ? minecraft.player.totalExperience : minecraft.player.experienceLevel;
            unlockButton.setMessage(!hasLockedSlots
                    ? Component.translatable("infiniteinvo.unlockslot.complete")
                    : canUnlock
                            ? Component.translatable("infiniteinvo.unlockslot")
                            : Component.literal(available + " / " + cost));
            updateUnlockButtonTooltip(unlockedSlots, totalSlots, cost, hasLockedSlots);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void updateUnlockButtonTooltip(int unlockedSlots, int totalSlots, int cost, boolean hasLockedSlots) {
        var tooltip = Component.translatable(
                "infiniteinvo.unlockslot.tooltip.unlocked", unlockedSlots, totalSlots)
                .withStyle(ChatFormatting.GRAY);
        tooltip.append(Component.literal("\n"));
        if (!hasLockedSlots) {
            tooltip.append(Component.translatable("infiniteinvo.unlockslot.tooltip.complete")
                    .withStyle(ChatFormatting.GREEN));
        } else {
            String costKey = Config.usesExperiencePoints()
                    ? "infiniteinvo.unlockslot.tooltip.cost.points"
                    : "infiniteinvo.unlockslot.tooltip.cost.levels";
            tooltip.append(Component.translatable(costKey, cost).withStyle(ChatFormatting.GOLD));
        }
        unlockButton.setTooltip(Tooltip.create(tooltip));
    }

    @Override
    public void containerTick() {
        super.containerTick();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        g.blit(TEXTURE, left, top, 0, 0, 169, 137);
        for (int col = 0; col < ScrollableInventoryLayout.EXTRA_COLUMNS; col++) {
            int x = left + 169 + ScrollableInventoryLayout.SLOT_SIZE * col;
            blitHeaderBackground(g, x, top, ScrollableInventoryLayout.SLOT_SIZE,
                    ScrollableInventoryLayout.GRID_BACKGROUND_Y);
        }
        // The arrow and result slot cross the original 169-pixel background edge.
        g.blit(TEXTURE, left + 169, top + ScrollableInventoryLayout.RESULT_Y - 1,
                169, ScrollableInventoryLayout.RESULT_Y - 1,
                ScrollableInventoryLayout.SLOT_SIZE, ScrollableInventoryLayout.SLOT_SIZE);

        drawAdditionalGridSlots(g, left, top);
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
            InventoryScreen.renderEntityInInventoryFollowsMouse(g,
                    left + ScrollableInventoryLayout.PLAYER_RENDER_LEFT_X,
                    top + ScrollableInventoryLayout.PLAYER_RENDER_TOP_Y,
                    left + ScrollableInventoryLayout.PLAYER_RENDER_RIGHT_X,
                    top + ScrollableInventoryLayout.PLAYER_RENDER_BOTTOM_Y,
                    30, 0.0625F, xMouse, yMouse, minecraft.player);
        }

    }

    private static void blitHeaderBackground(GuiGraphics graphics, int x, int y, int width, int height) {
        int topBorderHeight = Math.min(ScrollableInventoryLayout.FRAMELESS_BACKGROUND_SPRITE_Y, height);
        graphics.blit(TEXTURE, x, y,
                ScrollableInventoryLayout.FRAMELESS_BACKGROUND_SPRITE_X,
                ScrollableInventoryLayout.HEADER_BACKGROUND_SPRITE_Y,
                width, topBorderHeight);
        for (int offsetY = topBorderHeight; offsetY < height; offsetY += ScrollableInventoryLayout.SLOT_SIZE) {
            int tileHeight = Math.min(ScrollableInventoryLayout.SLOT_SIZE, height - offsetY);
            graphics.blit(TEXTURE, x, y + offsetY,
                    ScrollableInventoryLayout.FRAMELESS_BACKGROUND_SPRITE_X,
                    ScrollableInventoryLayout.FRAMELESS_BACKGROUND_SPRITE_Y,
                    width, tileHeight);
        }
    }

    private void drawAdditionalGridSlots(GuiGraphics graphics, int left, int top) {
        int gridX = left + ScrollableInventoryLayout.GRID_BACKGROUND_X;
        int gridY = top + ScrollableInventoryLayout.GRID_BACKGROUND_Y;
        int slotSize = ScrollableInventoryLayout.SLOT_SIZE;
        // Insert copies of the middle row before the original bottom row.
        // This leaves #25/#34 as the permanent bottom-left/bottom-right edge.
        for (int row = 2; row < ScrollableInventoryLayout.VISIBLE_ROWS - 1; row++) {
            graphics.blit(TEXTURE, left, gridY + row * slotSize,
                    0, ScrollableInventoryLayout.GRID_BACKGROUND_Y + slotSize, 169, slotSize);
        }
        if (ScrollableInventoryLayout.VISIBLE_ROWS > 3) {
            graphics.blit(TEXTURE, left,
                    gridY + (ScrollableInventoryLayout.VISIBLE_ROWS - 1) * slotSize,
                    0, ScrollableInventoryLayout.GRID_BACKGROUND_Y + 2 * slotSize, 169, slotSize);
        }

        // Reuse the matching row's interior slot while #14/#24/#34 remain
        // the right edge of their respective rows.
        for (int row = 0; row < ScrollableInventoryLayout.VISIBLE_ROWS; row++) {
            for (int col = 9; col < ScrollableInventoryLayout.COLUMNS - 1; col++) {
                int x = gridX + col * slotSize;
                int y = gridY + row * slotSize;
                if (row == 0) {
                    blitGridInterior(graphics, x, y, ScrollableInventoryLayout.GRID_TOP_INTERIOR_SPRITE_Y);
                } else if (row == ScrollableInventoryLayout.VISIBLE_ROWS - 1) {
                    blitGridInterior(graphics, x, y, ScrollableInventoryLayout.GRID_BOTTOM_INTERIOR_SPRITE_Y);
                } else {
                    blitGridInterior(graphics, x, y, ScrollableInventoryLayout.GRID_MIDDLE_INTERIOR_SPRITE_Y);
                }
            }
            int sourceY = row == 0
                    ? ScrollableInventoryLayout.GRID_BACKGROUND_Y
                    : row == ScrollableInventoryLayout.VISIBLE_ROWS - 1
                            ? ScrollableInventoryLayout.GRID_BACKGROUND_Y + 2 * slotSize
                            : ScrollableInventoryLayout.GRID_BACKGROUND_Y + slotSize;
            graphics.blit(TEXTURE, gridX + (ScrollableInventoryLayout.COLUMNS - 1) * slotSize,
                    gridY + row * slotSize, 169, sourceY, slotSize, slotSize);
        }
    }

    private static void blitGridInterior(GuiGraphics graphics, int x, int y, int spriteY) {
        graphics.blit(TEXTURE, x, y,
                ScrollableInventoryLayout.GRID_INTERIOR_SPRITE_X,
                spriteY,
                ScrollableInventoryLayout.SLOT_SIZE,
                ScrollableInventoryLayout.SLOT_SIZE);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, Component.translatable("container.crafting"),
                ScrollableInventoryLayout.CRAFTING_LABEL_X, ScrollableInventoryLayout.CRAFTING_LABEL_Y, 4210752, false);
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
                            ScrollableInventoryLayout.GRID_BACKGROUND_X + col * ScrollableInventoryLayout.SLOT_SIZE + 1,
                            ScrollableInventoryLayout.GRID_BACKGROUND_Y + row * ScrollableInventoryLayout.SLOT_SIZE + 1,
                            1, 167, 16, 16);
                } else if (slotIndex >= menu.getUnlockedSlots()) {
                    guiGraphics.blit(TEXTURE,
                            ScrollableInventoryLayout.GRID_BACKGROUND_X + col * ScrollableInventoryLayout.SLOT_SIZE + 1,
                            ScrollableInventoryLayout.GRID_BACKGROUND_Y + row * ScrollableInventoryLayout.SLOT_SIZE + 1,
                            19, 167, 16, 16);
                }
            }
        }

    }

    @Override
    protected void renderSlotHighlight(GuiGraphics graphics, net.minecraft.world.inventory.Slot slot,
                                       int mouseX, int mouseY, float partialTick) {
        if (isInfiniteInvoLockedSlot(slot) || !slot.isHighlightable()) {
            return;
        }
        if (isRecoloredGuiPackEnabled()) {
            renderTerminalStyleSlotHighlight(graphics, slot.x, slot.y);
        } else {
            super.renderSlotHighlight(graphics, slot, mouseX, mouseY, partialTick);
        }
    }

    private static void renderTerminalStyleSlotHighlight(GuiGraphics graphics, int x, int y) {
        final int width = 16;
        final int height = 16;
        final int borderColor = 0xFFDAFFFF;
        final int fillColor = 0x669CD3FF;

        graphics.hLine(x, x + width, y - 1, borderColor);
        graphics.hLine(x - 1, x + width, y + height, borderColor);
        graphics.vLine(x - 1, y - 2, y + height, borderColor);
        graphics.vLine(x + width, y - 2, y + height, borderColor);
        graphics.fillGradient(x, y, x + width, y + height, fillColor, fillColor);
    }

    private boolean isRecoloredGuiPackEnabled() {
        return minecraft != null && minecraft.getResourceManager().getResource(TEXTURE)
                .map(resource -> resource.sourcePackId().contains("recolored_gui"))
                .orElse(false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0 && minecraft != null && minecraft.gameMode != null && menu.getMaxScroll() > 0
                && isOverScrollableInventory(mouseX, mouseY)) {
            if ((Object) this instanceof QuickCraftCancellation cancellation) {
                cancellation.infiniteinvo$cancelQuickCraft();
            }
            requestScroll(requestedScrollPos + (scrollY > 0 ? -1 : 1));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** Keeps normal screen scrolling available outside the extended inventory grid. */
    private boolean isOverScrollableInventory(double mouseX, double mouseY) {
        int gridLeft = leftPos + ScrollableInventoryLayout.GRID_X - 1;
        int gridTop = topPos + ScrollableInventoryLayout.GRID_Y - 1;
        int gridWidth = ScrollableInventoryLayout.COLUMNS * ScrollableInventoryLayout.SLOT_SIZE;
        int gridHeight = ScrollableInventoryLayout.VISIBLE_ROWS * ScrollableInventoryLayout.SLOT_SIZE;
        return (mouseX >= gridLeft && mouseX < gridLeft + gridWidth
                && mouseY >= gridTop && mouseY < gridTop + gridHeight)
                || isOverScrollbar(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOverScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            setScrollFromMouse(mouseY);
            return true;
        }
        if (isInfiniteInvoLockedVirtualSlot(virtualSlotAt(mouseX, mouseY))) {
            return true;
        }
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        // InventoryScreen's recipe-book callback writes the vanilla position
        // after toggling. Reapply the extended position in the same input pass.
        refreshCompatibilityLayout();
        return handled;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar && button == 0) {
            setScrollFromMouse(mouseY);
            return true;
        }
        if (isInfiniteInvoLockedVirtualSlot(virtualSlotAt(mouseX, mouseY))) {
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

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType clickType) {
        if (inFlightScrollPos >= 0 || requestedScrollPos != menu.getScrollPos()) {
            return;
        }
        super.slotClicked(slot, slotId, mouseButton, clickType);
    }

    /** Restores the expanded inventory geometry after another mod changes it. */
    public void refreshCompatibilityLayout() {
        refreshCompatibilityLayout(recipeBookButton);
    }

    /**
     * Reapplies the layout after the vanilla recipe-book callback has moved its
     * button. The callback supplies the authoritative button instance because
     * optional integrations may add widgets during screen initialization.
     */
    public void refreshCompatibilityLayout(ImageButton callbackButton) {
        if (minecraft == null || minecraft.gameMode == null || minecraft.gameMode.hasInfiniteItems()) {
            return;
        }
        if (callbackButton != null) {
            recipeBookButton = callbackButton;
        }
        PlayerScreenCompatibilityLayout.RecipeBookLayout layout = PlayerScreenCompatibilityLayout.recipeBookLayout(
                width, imageWidth, getRecipeBookComponent().isVisible());
        leftPos = layout.inventoryLeft();
        if (recipeBookButton != null) {
            PlayerScreenCompatibilityLayout.positionRecipeBookButton(recipeBookButton, leftPos, topPos);
        }
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        return menu.getMaxScroll() > 0
                && mouseX >= leftPos + ScrollableInventoryLayout.SCROLL_X
                && mouseX < leftPos + ScrollableInventoryLayout.SCROLL_X + 15
                && mouseY >= topPos + ScrollableInventoryLayout.SCROLL_Y
                && mouseY < topPos + ScrollableInventoryLayout.SCROLL_Y + ScrollableInventoryLayout.SCROLL_HEIGHT;
    }

    private int virtualSlotAt(double mouseX, double mouseY) {
        int relativeX = (int) mouseX - leftPos - ScrollableInventoryLayout.GRID_X;
        int relativeY = (int) mouseY - topPos - ScrollableInventoryLayout.GRID_Y;
        if (relativeX < 0 || relativeY < 0) {
            return -1;
        }

        int col = relativeX / ScrollableInventoryLayout.SLOT_SIZE;
        int row = relativeY / ScrollableInventoryLayout.SLOT_SIZE;
        if (col >= menu.getVisibleColumns() || row >= menu.getVisibleRows()
                || relativeX % ScrollableInventoryLayout.SLOT_SIZE >= 16
                || relativeY % ScrollableInventoryLayout.SLOT_SIZE >= 16) {
            return -1;
        }
        return col + (row + menu.getScrollPos()) * menu.getVisibleColumns();
    }

    private boolean isInfiniteInvoLockedSlot(net.minecraft.world.inventory.Slot slot) {
        if (slot == null) {
            return false;
        }

        int relativeX = slot.x - ScrollableInventoryLayout.GRID_X;
        int relativeY = slot.y - ScrollableInventoryLayout.GRID_Y;
        if (relativeX < 0 || relativeY < 0 || relativeX % ScrollableInventoryLayout.SLOT_SIZE != 0
                || relativeY % ScrollableInventoryLayout.SLOT_SIZE != 0) {
            return false;
        }

        int col = relativeX / ScrollableInventoryLayout.SLOT_SIZE;
        int row = relativeY / ScrollableInventoryLayout.SLOT_SIZE;
        if (col >= menu.getVisibleColumns() || row >= menu.getVisibleRows()) {
            return false;
        }
        int virtualSlot = col + (row + menu.getScrollPos()) * menu.getVisibleColumns();
        return isInfiniteInvoLockedVirtualSlot(virtualSlot);
    }

    private boolean isInfiniteInvoLockedVirtualSlot(int virtualSlot) {
        return virtualSlot >= menu.getUnlockedSlots();
    }

    static ScrollableInventoryScreen current() {
        return Minecraft.getInstance().screen instanceof ScrollableInventoryScreen screen ? screen : null;
    }

    /** True while the visible scroll slots are awaiting a server-confirmed page remap. */
    public static boolean isPageChangePending(net.minecraft.world.inventory.AbstractContainerMenu menu) {
        ScrollableInventoryScreen screen = current();
        return screen != null && screen.menu == menu
                && (screen.inFlightScrollPos >= 0 || screen.requestedScrollPos != screen.menu.getScrollPos());
    }

    /** Applies the server-confirmed page and its authoritative visible stacks. */
    public static void applyServerPage(int containerId, int page, int requestId, List<ItemStack> stacks) {
        ScrollableInventoryScreen screen = current();
        if (screen == null || screen.menu.containerId != containerId
                || screen.inFlightScrollPos != page
                || screen.nextScrollRequestId != requestId) {
            return;
        }

        screen.menu.setScrollPosition(page);
        int firstSlot = page * screen.menu.getVisibleColumns();
        int visibleSlots = Math.min(screen.menu.getVisibleGridSlots(), stacks.size());
        for (int index = 0; index < visibleSlots; index++) {
            screen.menu.getStore().setItem(firstSlot + index, stacks.get(index).copy());
        }
        screen.inFlightScrollPos = -1;
        DebugLog.debug("[Paging][Client] applied scroll confirmation containerId={} page={} requestId={} stackCount={}",
                containerId, page, requestId, stacks.size());
        screen.dispatchScrollRequest();
    }

    public static boolean isIpnVirtualSlotLocked(Slot slot) {
        ScrollableInventoryScreen screen = current();
        int storageSlot = screen == null ? -1 : screen.menu.getVisibleStorageSlot(slot);
        return screen != null && storageSlot >= 0 && storageSlot < screen.menu.getUnlockedSlots()
                && IpnCompat.isVirtualSlotLocked(storageSlot);
    }

    List<Slot> visibleVirtualSlots() {
        return menu.slots.stream().filter(menu::isVisibleStorageSlot).toList();
    }

    int storageSlot(Slot slot) {
        return menu.getVisibleStorageSlot(slot);
    }

    int unlockedSlots() {
        return menu.getUnlockedSlots();
    }

    private void setScrollFromMouse(double mouseY) {
        int target = (int) Math.round((mouseY - topPos - ScrollableInventoryLayout.SCROLL_Y - 4)
                / ScrollableInventoryLayout.SCROLL_KNOB_TRAVEL * menu.getMaxScroll());
        target = Math.max(0, Math.min(menu.getMaxScroll(), target));
        if (target == requestedScrollPos || minecraft == null) {
            return;
        }
        if ((Object) this instanceof QuickCraftCancellation cancellation) {
            cancellation.infiniteinvo$cancelQuickCraft();
        }
        requestScroll(target);
    }

    private void requestScroll(int target) {
        requestedScrollPos = Math.max(0, Math.min(menu.getMaxScroll(), target));
        DebugLog.debug("[Paging][Client] scroll target requested displayedPage={} requestedPage={} targetPage={} containerId={}",
                menu.getScrollPos(), requestedScrollPos, target, menu.containerId);
        dispatchScrollRequest();
    }

    private void dispatchScrollRequest() {
        if (minecraft == null || inFlightScrollPos >= 0 || requestedScrollPos == menu.getScrollPos()) {
            return;
        }
        inFlightScrollPos = requestedScrollPos;
        DebugLog.debug("[Paging][Client] send scroll request containerId={} page={} requestId={} currentPage={}",
                menu.containerId, inFlightScrollPos, nextScrollRequestId + 1, menu.getScrollPos());
        PacketDistributor.sendToServer(new ScrollableInventoryPageRequestPayload(
                menu.containerId, inFlightScrollPos, ++nextScrollRequestId));
    }
}
