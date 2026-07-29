package infiniteinvo.client;

import infiniteinvo.InfiniteInvo;
import infiniteinvo.Config;
import infiniteinvo.inventory.CreativeInventoryPaging;
import infiniteinvo.network.CloseCreativeInventoryPagingPayload;
import infiniteinvo.network.CreativeInventoryPageRequestPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Adds creative-style extended-inventory paging to vanilla container screens. */
public final class ContainerInventoryPagingController {
    private static final ResourceLocation INVENTORY_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            InfiniteInvo.MODID, "textures/gui/adjustable_gui.png");
    private static final int KNOB_SIZE = 8;
    private static final int TRACK_HEIGHT = 54;
    private static final Map<AbstractContainerScreen<?>, State> STATES = new WeakHashMap<>();
    private static int lastContainerRow;

    private ContainerInventoryPagingController() {
    }

    /**
     * Starts the page request before vanilla has a chance to render the
     * physical storage slots left over from the previous container page.
     */
    public static void opening(Screen newScreen) {
        if (!(newScreen instanceof AbstractContainerScreen<?> screen) || screen instanceof CreativeModeInventoryScreen
                || screen instanceof ScrollableInventoryScreen) {
            return;
        }

        Grid grid = findPlayerGrid(screen);
        if (grid != null) {
            beginOpening(screen, grid);
        }
    }

    public static void render(ContainerScreenEvent.Render.Foreground event) {
        if (!(event.getContainerScreen() instanceof AbstractContainerScreen<?> screen) || screen instanceof CreativeModeInventoryScreen
                || screen instanceof ScrollableInventoryScreen) {
            return;
        }

        Grid grid = findPlayerGrid(screen);
        if (grid == null) {
            return;
        }

        State state = STATES.computeIfAbsent(screen, ignored -> new State());
        state.grid = grid;
        if (!state.open) {
            beginOpening(screen, grid);
        }
        if (state.awaitingPage) {
            hidePendingPageItems(event.getGuiGraphics(), grid);
        }
        drawScrollbar(event.getGuiGraphics(), screen, grid, state.row);
        drawLockedSlots(event.getGuiGraphics(), grid, state.row, state.unlockedSlots);
    }

    public static void mouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        State state = stateFor(event.getScreen());
        if (state == null || event.getScrollDeltaY() == 0.0D) {
            return;
        }

        request(state, state.row + (event.getScrollDeltaY() < 0.0D ? 1 : -1), false);
        event.setCanceled(true);
    }

    public static void mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        State state = stateFor(event.getScreen());
        if (state == null || event.getButton() != 0 || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || !isOverScrollbar(screen, state.grid, event.getMouseX(), event.getMouseY())) {
            return;
        }

        state.dragging = true;
        requestFromMouse(state, screen, event.getMouseY());
        event.setCanceled(true);
    }

    public static void mouseDragged(ScreenEvent.MouseDragged.Pre event) {
        State state = stateFor(event.getScreen());
        if (state == null || !state.dragging || event.getMouseButton() != 0
                || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }

        requestFromMouse(state, screen, event.getMouseY());
        event.setCanceled(true);
    }

    public static void mouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        State state = stateFor(event.getScreen());
        if (state != null && state.dragging && event.getButton() == 0) {
            state.dragging = false;
            event.setCanceled(true);
        }
    }

    public static void closing(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
            State state = STATES.remove(screen);
            if (state != null && state.open) {
                state.open = false;
                if (state.pageLocksApplied) {
                    IpnCompat.captureMappedPageLocks(state.row);
                    IpnCompat.applyMappedPageLocks(0);
                }
                if (!Config.REMEMBER_CONTAINER_PAGE.get()) {
                    lastContainerRow = 0;
                }
                PacketDistributor.sendToServer(CloseCreativeInventoryPagingPayload.INSTANCE);
            }
        }
    }

    private static State stateFor(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?> container) || container instanceof CreativeModeInventoryScreen
                || container instanceof ScrollableInventoryScreen) {
            return null;
        }
        State state = STATES.get(container);
        return state != null && state.open && state.grid != null ? state : null;
    }

    private static Grid findPlayerGrid(AbstractContainerScreen<?> screen) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return null;
        }

        Inventory inventory = minecraft.player.getInventory();
        List<Slot> slots = new ArrayList<>();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container == inventory && slot.getContainerSlot() >= 9 && slot.getContainerSlot() < 36) {
                slots.add(slot);
            }
        }
        if (slots.size() != CreativeInventoryPaging.PAGE_SIZE) {
            return null;
        }

        slots.sort(Comparator.comparingInt((Slot slot) -> slot.y).thenComparingInt(slot -> slot.x));
        int rightmostSlotX = slots.stream().mapToInt(slot -> slot.x).max().orElseThrow();
        return new Grid(slots.getFirst().x, slots.getFirst().y, rightmostSlotX, List.copyOf(slots));
    }

    /** Returns false for an out-of-date page response that would overwrite a newer page request. */
    public static boolean shouldApplyPageData(int row) {
        if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)
                || screen instanceof CreativeModeInventoryScreen || screen instanceof ScrollableInventoryScreen) {
            return true;
        }

        State state = STATES.get(screen);
        return state == null || !state.open || !state.awaitingPage || state.requestedRow == row;
    }

    private static void beginOpening(AbstractContainerScreen<?> screen, Grid grid) {
        State state = STATES.computeIfAbsent(screen, ignored -> new State());
        state.grid = grid;
        state.open = true;
        state.pageLocksApplied = false;
        IpnCompat.migrateNativeStorageLocks();
        request(state, Config.REMEMBER_CONTAINER_PAGE.get() ? lastContainerRow : 0, true);
    }

    private static void request(State state, int row, boolean force) {
        int target = Math.max(0, Math.min(row, CreativeInventoryPaging.maxRow()));
        if (!force && target == state.row) {
            return;
        }
        if (state.pageLocksApplied) {
            IpnCompat.captureMappedPageLocks(state.row);
        }
        state.row = target;
        state.requestedRow = target;
        state.awaitingPage = true;
        if (Config.REMEMBER_CONTAINER_PAGE.get()) {
            lastContainerRow = target;
        }
        clearNativeStorageSlots();
        PacketDistributor.sendToServer(new CreativeInventoryPageRequestPayload(target));
    }

    public static void receivePageData(int row, int unlocked) {
        if (Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen) {
            State state = STATES.get(screen);
            if (state != null && state.open) {
                if (state.awaitingPage && state.requestedRow != row) {
                    return;
                }
                state.row = row;
                state.unlockedSlots = unlocked;
                IpnCompat.applyMappedPageLocks(row);
                state.pageLocksApplied = true;
                state.awaitingPage = false;
            }
        }
    }

    private static void clearNativeStorageSlots() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        Inventory inventory = minecraft.player.getInventory();
        for (int index = 9; index < 36; index++) {
            inventory.setItem(index, ItemStack.EMPTY);
        }
    }

    private static void hidePendingPageItems(GuiGraphics graphics, Grid grid) {
        for (Slot slot : grid.slots) {
            graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0xFF8B8B8B);
        }
    }

    /** Client-side counterpart to the server lock check, used by quick-craft previews. */
    public static boolean isMappedSlotUnlocked(Inventory inventory, int inventorySlot) {
        if (inventorySlot < 9 || inventorySlot >= 36 || Minecraft.getInstance().player == null
                || inventory.player != Minecraft.getInstance().player
                || !(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)) {
            return true;
        }

        State state = STATES.get(screen);
        return state == null || !state.open || inventorySlot - 9 + state.row * 9 < state.unlockedSlots;
    }

    /** Hides physical slots until the server confirms the requested virtual page. */
    public static boolean isAwaitingMappedPage(Inventory inventory, int inventorySlot) {
        if (inventorySlot < 9 || inventorySlot >= 36 || Minecraft.getInstance().player == null
                || inventory.player != Minecraft.getInstance().player
                || !(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)) {
            return false;
        }

        State state = STATES.get(screen);
        return state != null && state.open && state.awaitingPage;
    }

    private static void requestFromMouse(State state, AbstractContainerScreen<?> screen, double mouseY) {
        int relative = (int) Math.round(mouseY - screen.getGuiTop() - state.grid.y + 1);
        int row = Math.round((float) relative / TRACK_HEIGHT * CreativeInventoryPaging.maxRow());
        request(state, row, false);
    }

    private static boolean isOverScrollbar(AbstractContainerScreen<?> screen, Grid grid, double mouseX, double mouseY) {
        int x = screen.getGuiLeft() + grid.rightmostSlotX + 19;
        int y = screen.getGuiTop() + grid.y - 1;
        return mouseX >= x && mouseX < x + 8 && mouseY >= y && mouseY < y + TRACK_HEIGHT;
    }

    private static void drawScrollbar(GuiGraphics graphics, AbstractContainerScreen<?> screen, Grid grid, int row) {
        int x = grid.rightmostSlotX + 19;
        int y = grid.y - 1;
        graphics.blit(INVENTORY_TEXTURE, x, y, 52, 166, 8, 18);
        graphics.blit(INVENTORY_TEXTURE, x, y + 18, 44, 166, 8, 18);
        graphics.blit(INVENTORY_TEXTURE, x, y + 36, 36, 166, 8, 18);
        int knobY = y + Math.round((float) row / CreativeInventoryPaging.maxRow() * 46.0F);
        graphics.blit(INVENTORY_TEXTURE, x, knobY, 60, 166, KNOB_SIZE, KNOB_SIZE);
    }

    private static void drawLockedSlots(GuiGraphics graphics, Grid grid, int row, int unlockedSlots) {
        for (int index = 0; index < grid.slots.size(); index++) {
            Slot slot = grid.slots.get(index);
            int virtualSlot = row * 9 + slot.getContainerSlot() - 9;
            if (virtualSlot >= unlockedSlots) {
                graphics.blit(INVENTORY_TEXTURE, slot.x - 1, slot.y - 1, 18, 166, 18, 18);
            }
        }
    }

    private record Grid(int x, int y, int rightmostSlotX, List<Slot> slots) {
    }

    private static final class State {
        private int row;
        private int requestedRow;
        private boolean open;
        private boolean pageLocksApplied;
        private boolean awaitingPage;
        private boolean dragging;
        // The server will replace this with the authoritative unlock count.
        // Rendering no lock marker until then is preferable to a false locked flash.
        private int unlockedSlots = Integer.MAX_VALUE;
        private Grid grid;
    }
}
