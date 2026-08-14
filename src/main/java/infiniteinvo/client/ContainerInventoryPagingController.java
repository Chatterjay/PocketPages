package infiniteinvo.client;

import infiniteinvo.InfiniteInvo;
import infiniteinvo.DebugLog;
import infiniteinvo.Config;
import infiniteinvo.inventory.CreativeInventoryPaging;
import infiniteinvo.inventory.InfiniteInventoryData;
import infiniteinvo.network.CloseCreativeInventoryPagingPayload;
import infiniteinvo.network.CreativeInventoryPageRequestPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Adds creative-style extended-inventory paging to vanilla container screens. */
public final class ContainerInventoryPagingController {
    private static final ResourceLocation INVENTORY_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            InfiniteInvo.MODID, "textures/gui/adjustable_gui.png");
    private static final int KNOB_SIZE = 8;
    private static final int TRACK_HEIGHT = 54;
    private static final int PAGE_REQUEST_DEBOUNCE_TICKS = 2;
    private static final Map<AbstractContainerScreen<?>, State> STATES = new WeakHashMap<>();
    private static int lastContainerRow;

    private ContainerInventoryPagingController() {
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
        drawScrollbar(event.getGuiGraphics(), screen, grid, state.requestedRow);
        drawLockedSlots(event.getGuiGraphics(), screen, grid, state.unlockedSlots);
    }

    public static void mouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        State state = stateFor(event.getScreen());
        if (state == null || event.getScrollDeltaY() == 0.0D
                || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || !isOverMappedInventory(screen, state.grid, event.getMouseX(), event.getMouseY())) {
            return;
        }

        cancelQuickCraft(event.getScreen());
        int target = state.requestedRow + (event.getScrollDeltaY() < 0.0D ? 1 : -1);
        DebugLog.debug("[Paging][Client] container scroll delta={} displayedRow={} requestedRow={} targetRow={} session={}",
                event.getScrollDeltaY(), state.displayedRow, state.requestedRow, target, state.sessionId);
        request(state, target, false);
        event.setCanceled(true);
    }

    public static void mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        State state = stateFor(event.getScreen());
        if (state == null || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }

        if (state.awaitingPage || state.requestQueued) {
            event.setCanceled(true);
            return;
        }
        if (isOverLockedSlot(screen, state.grid, state.unlockedSlots, event.getMouseX(), event.getMouseY())) {
            cancelQuickCraft(event.getScreen());
            event.setCanceled(true);
            return;
        }
        if (event.getButton() != 0 || !isOverScrollbar(screen, state.grid, event.getMouseX(), event.getMouseY())) {
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
            state.requestDelay = 0;
            dispatchQueuedRequest(state);
            event.setCanceled(true);
        }
    }

    /** Coalesces rapid wheel/drag updates before changing the server-side slot mapping. */
    public static void tick() {
        for (State state : List.copyOf(STATES.values())) {
            if (!state.open || state.awaitingPage || !state.requestQueued) {
                continue;
            }
            if (state.requestDelay > 0) {
                state.requestDelay--;
                continue;
            }
            dispatchQueuedRequest(state);
        }
    }

    public static void closing(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
            State state = STATES.remove(screen);
            if (state != null && state.open) {
                CreativeInventoryPaging.restoreMenu(screen.getMenu());
                if (Minecraft.getInstance().player != null) {
                    CreativeInventoryPaging.restoreMenu(Minecraft.getInstance().player.inventoryMenu);
                }
                state.open = false;
                if (state.pageLocksApplied) {
                    IpnCompat.captureMappedPageLocks(state.displayedRow);
                    IpnCompat.applyMappedPageLocks(0);
                }
                if (!Config.REMEMBER_CONTAINER_PAGE.get()) {
                    lastContainerRow = 0;
                }
                PacketDistributor.sendToServer(new CloseCreativeInventoryPagingPayload(state.sessionId));
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
            if (CreativeInventoryPaging.isPlayerStorageSlot(screen.getMenu(), slot, inventory)) {
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

    /** Returns false for a response that does not match the page currently awaited by this screen. */
    public static boolean shouldApplyPageData(int row, int sessionId, int requestId) {
        if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)
                || screen instanceof CreativeModeInventoryScreen || screen instanceof ScrollableInventoryScreen) {
            // Page packets are only valid while a paging screen is open. Once
            // it closes, vanilla container synchronization restores the real
            // inventory; applying a late page packet would resurrect stale UI
            // data in the client's physical slots.
            return false;
        }

        State state = STATES.get(screen);
        return state != null && state.open && state.sessionId == sessionId
                && (state.awaitingPage
                ? state.inFlightRow == row && state.inFlightRequestId == requestId
                : state.displayedRow == row);
    }

    private static void beginOpening(AbstractContainerScreen<?> screen, Grid grid) {
        State state = STATES.computeIfAbsent(screen, ignored -> new State());
        state.grid = grid;
        state.open = true;
        state.sessionId = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        state.nextRequestId = 0;
        state.pageLocksApplied = false;
        IpnCompat.migrateNativeStorageLocks();
        request(state, Config.REMEMBER_CONTAINER_PAGE.get() ? lastContainerRow : 0, true);
    }

    private static void request(State state, int row, boolean force) {
        int target = Math.max(0, Math.min(row, CreativeInventoryPaging.maxRow()));
        if (!force && target == state.requestedRow
                && (state.requestQueued || state.awaitingPage || target == state.displayedRow)) {
            return;
        }
        state.requestedRow = target;
        if (target == state.displayedRow && !state.awaitingPage) {
            state.requestQueued = false;
            return;
        }
        state.requestQueued = true;
        state.requestDelay = force ? 0 : PAGE_REQUEST_DEBOUNCE_TICKS;
        if (Config.REMEMBER_CONTAINER_PAGE.get()) {
            lastContainerRow = target;
        }
        if (force) {
            dispatchQueuedRequest(state);
        }
    }

    /** True while a regular container's player-storage page is being remapped. */
    public static boolean isPageChangePending(net.minecraft.world.inventory.AbstractContainerMenu menu) {
        if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)
                || screen.getMenu() != menu) {
            return false;
        }
        State state = STATES.get(screen);
        return state != null && state.open && (state.awaitingPage || state.requestQueued);
    }

    public static void receivePageData(int row, int unlocked, int sessionId, int requestId) {
        DebugLog.debug("[Paging][Client] container page confirmation row={} session={} requestId={} unlocked={}",
                row, sessionId, requestId, unlocked);
        if (Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen) {
            State state = STATES.get(screen);
            if (state != null && state.open) {
                if (state.sessionId != sessionId
                        || (state.awaitingPage
                        && state.inFlightRow != row)
                        || (state.awaitingPage && requestId != Integer.MAX_VALUE
                        && state.inFlightRequestId != requestId)) {
                    DebugLog.debug("[Paging][Client] container confirmation rejected row={} session={} requestId={}",
                            row, sessionId, requestId);
                    return;
                }
                state.displayedRow = row;
                state.unlockedSlots = unlocked;
                IpnCompat.applyMappedPageLocks(row);
                state.pageLocksApplied = true;
                state.awaitingPage = false;
                state.inFlightRow = -1;
                state.inFlightRequestId = -1;
                if (state.requestedRow != row) {
                    state.requestQueued = true;
                    state.requestDelay = 0;
                    dispatchQueuedRequest(state);
                }
            }
        }
    }

    /** Client-side counterpart to the server lock check, used by quick-craft previews. */
    public static boolean isMappedSlotUnlocked(Inventory inventory, int inventorySlot) {
        if (inventorySlot < 9 || Minecraft.getInstance().player == null
                || inventory.player != Minecraft.getInstance().player
                || !(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)) {
            return true;
        }
        return inventorySlot - 9 < InfiniteInventoryData.getUnlocked(Minecraft.getInstance().player);
    }

    /** Returns true for one of the disabled page-fill slots in an open container. */
    public static boolean isMappedSlotLocked(Slot slot) {
        if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)
                || screen instanceof CreativeModeInventoryScreen || screen instanceof ScrollableInventoryScreen) {
            return false;
        }
        int virtualSlot = CreativeInventoryPaging.getPagedStorageSlot(screen.getMenu(), slot);
        return virtualSlot >= 0
                && virtualSlot >= InfiniteInventoryData.getUnlocked(Minecraft.getInstance().player);
    }

    private static void requestFromMouse(State state, AbstractContainerScreen<?> screen, double mouseY) {
        int relative = (int) Math.round(mouseY - screen.getGuiTop() - state.grid.y + 1);
        int row = Math.round((float) relative / TRACK_HEIGHT * CreativeInventoryPaging.maxRow());
        request(state, row, false);
    }

    private static void cancelQuickCraft(Screen screen) {
        if (screen instanceof QuickCraftCancellation cancellation) {
            cancellation.infiniteinvo$cancelQuickCraft();
        }
    }

    private static void dispatchQueuedRequest(State state) {
        if (!state.open || state.awaitingPage || !state.requestQueued) {
            return;
        }
        if (state.pageLocksApplied) {
            IpnCompat.captureMappedPageLocks(state.displayedRow);
        }
        state.inFlightRow = state.requestedRow;
        state.inFlightRequestId = ++state.nextRequestId;
        state.awaitingPage = true;
        state.requestQueued = false;
        DebugLog.debug("[Paging][Client] send container page request row={} session={} requestId={} displayedRow={}",
                state.inFlightRow, state.sessionId, state.inFlightRequestId, state.displayedRow);
        PacketDistributor.sendToServer(new CreativeInventoryPageRequestPayload(
                state.inFlightRow, state.sessionId, state.inFlightRequestId));
    }

    private static boolean isOverScrollbar(AbstractContainerScreen<?> screen, Grid grid, double mouseX, double mouseY) {
        int x = screen.getGuiLeft() + grid.rightmostSlotX + 19;
        int y = screen.getGuiTop() + grid.y - 1;
        return mouseX >= x && mouseX < x + 8 && mouseY >= y && mouseY < y + TRACK_HEIGHT;
    }

    private static boolean isOverMappedInventory(AbstractContainerScreen<?> screen, Grid grid, double mouseX, double mouseY) {
        int left = screen.getGuiLeft() + grid.x - 1;
        int top = screen.getGuiTop() + grid.y - 1;
        int width = grid.rightmostSlotX - grid.x + 18;
        int height = 3 * 18;
        return (mouseX >= left && mouseX < left + width
                && mouseY >= top && mouseY < top + height)
                || isOverScrollbar(screen, grid, mouseX, mouseY);
    }

    private static boolean isOverLockedSlot(AbstractContainerScreen<?> screen, Grid grid, int unlockedSlots,
                                            double mouseX, double mouseY) {
        for (Slot slot : grid.slots) {
            if (mouseX >= screen.getGuiLeft() + slot.x && mouseX < screen.getGuiLeft() + slot.x + 16
                    && mouseY >= screen.getGuiTop() + slot.y && mouseY < screen.getGuiTop() + slot.y + 16
                    && CreativeInventoryPaging.getPagedStorageSlot(screen.getMenu(), slot) >= unlockedSlots) {
                return true;
            }
        }
        return false;
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

    private static void drawLockedSlots(GuiGraphics graphics, AbstractContainerScreen<?> screen,
                                        Grid grid, int unlockedSlots) {
        for (Slot slot : grid.slots) {
            int virtualSlot = CreativeInventoryPaging.getPagedStorageSlot(screen.getMenu(), slot);
            if (virtualSlot >= Config.totalExtraSlots()) {
                graphics.blit(INVENTORY_TEXTURE, slot.x, slot.y, 1, 167, 16, 16);
            } else if (virtualSlot >= unlockedSlots) {
                graphics.blit(INVENTORY_TEXTURE, slot.x, slot.y, 19, 167, 16, 16);
            }
        }
    }

    private record Grid(int x, int y, int rightmostSlotX, List<Slot> slots) {
    }

    private static final class State {
        private int displayedRow;
        private int requestedRow;
        private int inFlightRow = -1;
        private int inFlightRequestId = -1;
        private int sessionId;
        private int nextRequestId;
        private boolean open;
        private boolean pageLocksApplied;
        private boolean awaitingPage;
        private boolean requestQueued;
        private boolean dragging;
        private int requestDelay;
        // The server will replace this with the authoritative unlock count.
        // Rendering no lock marker until then is preferable to a false locked flash.
        private int unlockedSlots = Integer.MAX_VALUE;
        private Grid grid;
    }
}
