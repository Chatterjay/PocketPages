package infiniteinvo.client;

import infiniteinvo.InfiniteInvo;
import infiniteinvo.DebugLog;
import infiniteinvo.Config;
import infiniteinvo.inventory.CreativeInventoryPaging;
import infiniteinvo.inventory.InfiniteInventoryData;
import infiniteinvo.network.CloseCreativeInventoryPagingPayload;
import infiniteinvo.network.CreativeInventoryPageRequestPayload;
import infiniteinvo.network.ClearInfiniteInventoryPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.inventory.Slot;

/** Adds extended-inventory paging to the vanilla creative inventory tab. */
public final class CreativeInventoryController {
    private static final ResourceLocation INVENTORY_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            InfiniteInvo.MODID, "textures/gui/adjustable_gui.png");
    private static final int TRACK_X = 172;
    private static final int TRACK_Y = 53;
    private static final int TRACK_WIDTH = 8;
    private static final int TRACK_HEIGHT = 54;
    private static final int KNOB_SIZE = 8;
    private static final int PAGE_REQUEST_DEBOUNCE_TICKS = 2;
    private static final Map<CreativeModeInventoryScreen, State> STATES = new WeakHashMap<>();

    private CreativeInventoryController() {
    }

    public static void render(ContainerScreenEvent.Render.Foreground event) {
        if (!(event.getContainerScreen() instanceof CreativeModeInventoryScreen screen)) {
            return;
        }

        State state = STATES.computeIfAbsent(screen, ignored -> new State());
        if (!screen.isInventoryOpen()) {
            if (state.open) {
                close(state);
            }
            return;
        }

        if (!state.open) {
            state.open = true;
            state.sessionId = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
            state.nextRequestId = 0;
            IpnCompat.migrateNativeStorageLocks();
            request(state, 0, true);
        }
        drawScrollbar(event.getGuiGraphics(), screen, state.requestedRow);
        drawDisabledSlots(event.getGuiGraphics(), screen, state.unlockedSlots);
    }

    public static void mouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        State state = stateForOpenCreativeInventory(event.getScreen());
        if (state == null || event.getScrollDeltaY() == 0.0D
                || !(event.getScreen() instanceof CreativeModeInventoryScreen screen)
                || !isOverMappedInventory(screen, event.getMouseX(), event.getMouseY())) {
            return;
        }

        cancelQuickCraft(event.getScreen());
        int target = state.requestedRow + (event.getScrollDeltaY() < 0.0D ? 1 : -1);
        DebugLog.debug("[Paging][Client] creative scroll delta={} displayedRow={} requestedRow={} targetRow={} session={}",
                event.getScrollDeltaY(), state.displayedRow, state.requestedRow, target, state.sessionId);
        request(state, target, false);
        event.setCanceled(true);
    }

    public static void mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        State state = stateForOpenCreativeInventory(event.getScreen());
        if (state == null || !(event.getScreen() instanceof CreativeModeInventoryScreen screen)) {
            return;
        }

        if (state.awaitingPage || state.requestQueued) {
            event.setCanceled(true);
            return;
        }
        if (event.getButton() != 0) {
            return;
        }

        state.destroyRequested = isDestroySlot(screen, event.getMouseX(), event.getMouseY());
        if (state.destroyRequested) {
            return;
        }
        if (!isOverScrollbar(screen, event.getMouseX(), event.getMouseY())) return;

        state.dragging = true;
        requestFromMouse(state, screen, event.getMouseY());
        event.setCanceled(true);
    }

    /**
     * Runs after the native click path so vanilla and integration mods such as
     * Curios can clear their own inventories before the extended state changes.
     */
    public static void mousePressedPost(ScreenEvent.MouseButtonPressed.Post event) {
        State state = stateForOpenCreativeInventory(event.getScreen());
        if (state == null || event.getButton() != 0 || !state.destroyRequested) {
            return;
        }

        state.destroyRequested = false;
        if (event.wasClickHandled()) {
            PacketDistributor.sendToServer(ClearInfiniteInventoryPayload.INSTANCE);
        }
    }

    public static void mouseDragged(ScreenEvent.MouseDragged.Pre event) {
        State state = stateForOpenCreativeInventory(event.getScreen());
        if (state == null || !state.dragging || event.getMouseButton() != 0
                || !(event.getScreen() instanceof CreativeModeInventoryScreen screen)) {
            return;
        }

        requestFromMouse(state, screen, event.getMouseY());
        event.setCanceled(true);
    }

    public static void mouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        State state = stateForOpenCreativeInventory(event.getScreen());
        if (state != null && state.dragging && event.getButton() == 0) {
            state.dragging = false;
            state.requestDelay = 0;
            dispatchQueuedRequest(state);
            event.setCanceled(true);
        }
    }

    /** True while the creative inventory's mapped player page is being remapped. */
    public static boolean isPageChangePending(net.minecraft.world.inventory.AbstractContainerMenu menu) {
        if (!(Minecraft.getInstance().screen instanceof CreativeModeInventoryScreen screen)
                || screen.getMenu() != menu) {
            return false;
        }
        State state = STATES.get(screen);
        return state != null && state.open && (state.awaitingPage || state.requestQueued);
    }

    /** Coalesces rapid wheel and drag updates before replacing the native inventory page. */
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
        if (event.getScreen() instanceof CreativeModeInventoryScreen screen) {
            State state = STATES.remove(screen);
            if (state != null && state.open) {
                close(state);
            }
        }
    }

    public static void applyPage(int row, int unlockedSlots, int sessionId, int requestId, List<ItemStack> stacks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        DebugLog.debug("[Paging][Client] page response row={} session={} requestId={} stacks={} screen={}",
                row, sessionId, requestId, describeStacks(stacks),
                minecraft.screen == null ? "none" : minecraft.screen.getClass().getSimpleName());

        State creativeState = null;
        if (minecraft.screen instanceof CreativeModeInventoryScreen screen) {
            creativeState = STATES.get(screen);
            if (creativeState == null || !creativeState.open || creativeState.sessionId != sessionId
                    || (creativeState.awaitingPage
                    ? creativeState.inFlightRow != row
                    || (requestId != Integer.MAX_VALUE && creativeState.inFlightRequestId != requestId)
                    : creativeState.displayedRow != row)) {
                DebugLog.debug("[Paging][Client] page response rejected creative row={} session={} requestId={}",
                        row, sessionId, requestId);
                return;
            }
        } else if (!ContainerInventoryPagingController.shouldApplyPageData(row, sessionId, requestId)) {
            DebugLog.debug("[Paging][Client] page response rejected container row={} session={} requestId={}",
                    row, sessionId, requestId);
            return;
        }

        InfiniteInventoryData.applyClientPage(minecraft.player, row, unlockedSlots, stacks);
        CreativeInventoryPaging.mapClientMenu(minecraft.player, minecraft.player.inventoryMenu, row);
        if (!(minecraft.screen instanceof CreativeModeInventoryScreen)) {
            if (minecraft.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> container) {
                CreativeInventoryPaging.mapClientMenu(minecraft.player, container.getMenu(), row);
            }
        }
        IpnCompat.applyMappedPageLocks(row);
        if (creativeState != null) {
            creativeState.displayedRow = row;
            creativeState.unlockedSlots = unlockedSlots;
            creativeState.pageLocksApplied = true;
            creativeState.awaitingPage = false;
            creativeState.inFlightRow = -1;
            creativeState.inFlightRequestId = -1;
            if (creativeState.requestedRow != row) {
                creativeState.requestQueued = true;
                creativeState.requestDelay = 0;
                dispatchQueuedRequest(creativeState);
            }
            DebugLog.debug("[Paging][Client] creative page applied row={} displayedRow={} requestedRow={} nextQueued={}",
                    row, creativeState.displayedRow, creativeState.requestedRow, creativeState.requestQueued);
        } else {
            ContainerInventoryPagingController.receivePageData(row, unlockedSlots, sessionId, requestId);
        }
    }

    private static State stateForOpenCreativeInventory(net.minecraft.client.gui.screens.Screen screen) {
        if (!(screen instanceof CreativeModeInventoryScreen creative) || !creative.isInventoryOpen()) {
            return null;
        }
        State state = STATES.get(creative);
        return state != null && state.open ? state : null;
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
        if (force) {
            dispatchQueuedRequest(state);
        }
    }

    private static void requestFromMouse(State state, CreativeModeInventoryScreen screen, double mouseY) {
        int relative = (int) Math.round(mouseY - screen.getGuiTop() - TRACK_Y);
        int row = Math.round((float) relative / TRACK_HEIGHT * CreativeInventoryPaging.maxRow());
        request(state, row, false);
    }

    private static void cancelQuickCraft(net.minecraft.client.gui.screens.Screen screen) {
        if (screen instanceof QuickCraftCancellation cancellation) {
            cancellation.infiniteinvo$cancelQuickCraft();
        }
    }

    private static boolean isOverScrollbar(CreativeModeInventoryScreen screen, double mouseX, double mouseY) {
        int left = screen.getGuiLeft() + TRACK_X;
        int top = screen.getGuiTop() + TRACK_Y;
        return mouseX >= left && mouseX < left + TRACK_WIDTH
                && mouseY >= top && mouseY < top + TRACK_HEIGHT;
    }

    private static boolean isOverMappedInventory(CreativeModeInventoryScreen screen, double mouseX, double mouseY) {
        boolean overScrollbar = isOverScrollbar(screen, mouseX, mouseY);
        if (overScrollbar || Minecraft.getInstance().player == null) {
            return overScrollbar;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int slots = 0;
        for (Slot slot : screen.getMenu().slots) {
            if (!CreativeInventoryPaging.isPlayerStorageSlot(
                    screen.getMenu(), slot, Minecraft.getInstance().player.getInventory())) {
                continue;
            }
            minX = Math.min(minX, slot.x);
            minY = Math.min(minY, slot.y);
            maxX = Math.max(maxX, slot.x + 16);
            maxY = Math.max(maxY, slot.y + 16);
            slots++;
        }
        if (slots != CreativeInventoryPaging.PAGE_SIZE) {
            return false;
        }

        double relativeX = mouseX - screen.getGuiLeft();
        double relativeY = mouseY - screen.getGuiTop();
        return relativeX >= minX - 1 && relativeX < maxX + 1
                && relativeY >= minY - 1 && relativeY < maxY + 1;
    }

    private static boolean isDestroySlot(CreativeModeInventoryScreen screen, double mouseX, double mouseY) {
        Slot slot = findSlotAt(screen, mouseX, mouseY);
        if (slot == null) {
            return false;
        }

        // Vanilla allocates a dedicated one-slot container only for its trash
        // slot. Checking the slot object supports integrations that reposition it.
        return slot.getContainerSlot() == 0
                && slot.container.getContainerSize() == 1
                && slot.getItem().isEmpty();
    }

    private static Slot findSlotAt(CreativeModeInventoryScreen screen, double mouseX, double mouseY) {
        int relativeX = (int) mouseX - screen.getGuiLeft();
        int relativeY = (int) mouseY - screen.getGuiTop();
        for (Slot slot : screen.getMenu().slots) {
            if (relativeX >= slot.x && relativeX < slot.x + 16
                    && relativeY >= slot.y && relativeY < slot.y + 16) {
                return slot;
            }
        }
        return null;
    }

    private static void drawDisabledSlots(GuiGraphics graphics, CreativeModeInventoryScreen screen, int unlockedSlots) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        List<Slot> storageSlots = new ArrayList<>();
        for (Slot slot : screen.getMenu().slots) {
            if (CreativeInventoryPaging.getMappedStorageSlot(slot) >= 0) {
                storageSlots.add(slot);
            }
        }
        if (storageSlots.size() != CreativeInventoryPaging.PAGE_SIZE) {
            return;
        }

        for (Slot slot : storageSlots) {
            int virtualSlot = CreativeInventoryPaging.getMappedStorageSlot(slot);
            if (virtualSlot >= Config.totalExtraSlots()) {
                graphics.blit(INVENTORY_TEXTURE, slot.x, slot.y, 1, 167, 16, 16);
            } else if (virtualSlot >= unlockedSlots) {
                graphics.blit(INVENTORY_TEXTURE, slot.x, slot.y, 19, 167, 16, 16);
            }
        }
    }

    private static void drawScrollbar(GuiGraphics graphics, CreativeModeInventoryScreen screen, int row) {
        int x = TRACK_X;
        int y = TRACK_Y;
        graphics.blit(INVENTORY_TEXTURE, x, y, 52, 166, 8, 18);
        graphics.blit(INVENTORY_TEXTURE, x, y + 18, 44, 166, 8, 18);
        graphics.blit(INVENTORY_TEXTURE, x, y + 36, 36, 166, 8, 18);
        int knobY = y + Math.round((float) row / CreativeInventoryPaging.maxRow() * 46.0F);
        graphics.blit(INVENTORY_TEXTURE, x, knobY, 60, 166, KNOB_SIZE, KNOB_SIZE);
    }

    private static void close(State state) {
        if (Minecraft.getInstance().screen instanceof CreativeModeInventoryScreen screen) {
            CreativeInventoryPaging.restoreMenu(Minecraft.getInstance().player.inventoryMenu);
        }
        if (state.pageLocksApplied) {
            IpnCompat.captureMappedPageLocks(state.displayedRow);
            IpnCompat.applyMappedPageLocks(0);
        }
        state.open = false;
        state.dragging = false;
        state.requestQueued = false;
        PacketDistributor.sendToServer(new CloseCreativeInventoryPagingPayload(state.sessionId));
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
        DebugLog.debug("[Paging][Client] send creative page request row={} session={} requestId={} displayedRow={}",
                state.inFlightRow, state.sessionId, state.inFlightRequestId, state.displayedRow);
        PacketDistributor.sendToServer(new CreativeInventoryPageRequestPayload(
                state.inFlightRow, state.sessionId, state.inFlightRequestId));
    }

    private static String describeStacks(List<ItemStack> stacks) {
        if (!DebugLog.enabled()) {
            return "disabled";
        }
        StringBuilder result = new StringBuilder("[");
        for (int i = 0; i < stacks.size(); i++) {
            if (i > 0) {
                result.append(", ");
            }
            result.append(i + 9).append('=').append(DebugLog.stack(stacks.get(i)));
        }
        return result.append(']').toString();
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
        private boolean destroyRequested;
        private int requestDelay;
        private int unlockedSlots = Integer.MAX_VALUE;
    }
}
