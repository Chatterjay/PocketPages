package infiniteinvo.client;

import infiniteinvo.InfiniteInvo;
import infiniteinvo.inventory.CreativeInventoryPaging;
import infiniteinvo.inventory.InfiniteInventoryData;
import infiniteinvo.network.CloseCreativeInventoryPagingPayload;
import infiniteinvo.network.CreativeInventoryPageRequestPayload;
import infiniteinvo.network.ClearInfiniteInventoryPayload;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
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
            request(state, 0, true);
        }
        drawScrollbar(event.getGuiGraphics(), screen, state.row);
    }

    public static void mouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        State state = stateForOpenCreativeInventory(event.getScreen());
        if (state == null || event.getScrollDeltaY() == 0.0D) {
            return;
        }

        request(state, state.row + (event.getScrollDeltaY() < 0.0D ? 1 : -1), false);
        event.setCanceled(true);
    }

    public static void mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        State state = stateForOpenCreativeInventory(event.getScreen());
        if (state == null || event.getButton() != 0 || !(event.getScreen() instanceof CreativeModeInventoryScreen screen)) {
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
            event.setCanceled(true);
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

    public static void applyPage(int row, int unlockedSlots, List<ItemStack> stacks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        for (int i = 0; i < CreativeInventoryPaging.PAGE_SIZE; i++) {
            ItemStack stack = i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY;
            minecraft.player.getInventory().setItem(i + 9, stack.copy());
        }
        InfiniteInventoryData.applyClientPage(minecraft.player, row, unlockedSlots, stacks);
        IpnCompat.applyMappedPageLocks(row);
        if (minecraft.screen instanceof CreativeModeInventoryScreen screen) {
            State state = STATES.get(screen);
            if (state != null && state.open) {
                state.pageLocksApplied = true;
            }
        }
        ContainerInventoryPagingController.receivePageData(row, unlockedSlots);
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
        if (!force && target == state.row) {
            return;
        }
        if (state.pageLocksApplied) {
            IpnCompat.captureMappedPageLocks(state.row);
        }
        state.row = target;
        PacketDistributor.sendToServer(new CreativeInventoryPageRequestPayload(target));
    }

    private static void requestFromMouse(State state, CreativeModeInventoryScreen screen, double mouseY) {
        int relative = (int) Math.round(mouseY - screen.getGuiTop() - TRACK_Y);
        int row = Math.round((float) relative / TRACK_HEIGHT * CreativeInventoryPaging.maxRow());
        request(state, row, false);
    }

    private static boolean isOverScrollbar(CreativeModeInventoryScreen screen, double mouseX, double mouseY) {
        int left = screen.getGuiLeft() + TRACK_X;
        int top = screen.getGuiTop() + TRACK_Y;
        return mouseX >= left && mouseX < left + TRACK_WIDTH
                && mouseY >= top && mouseY < top + TRACK_HEIGHT;
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
        if (state.pageLocksApplied) {
            IpnCompat.captureMappedPageLocks(state.row);
            IpnCompat.applyMappedPageLocks(0);
        }
        state.open = false;
        state.dragging = false;
        PacketDistributor.sendToServer(CloseCreativeInventoryPagingPayload.INSTANCE);
    }

    private static final class State {
        private int row;
        private boolean open;
        private boolean pageLocksApplied;
        private boolean dragging;
        private boolean destroyRequested;
    }
}
