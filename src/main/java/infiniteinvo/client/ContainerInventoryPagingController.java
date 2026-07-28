package infiniteinvo.client;

import infiniteinvo.inventory.CreativeInventoryPaging;
import infiniteinvo.InfiniteInvo;
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
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Adds creative-style extended-inventory paging to vanilla container screens. */
public final class ContainerInventoryPagingController {
    private static final ResourceLocation INVENTORY_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            InfiniteInvo.MODID, "textures/gui/adjustable_gui.png");
    private static final int KNOB_SIZE = 8;
    private static final int TRACK_HEIGHT = 54;
    private static final Map<AbstractContainerScreen<?>, State> STATES = new WeakHashMap<>();

    private ContainerInventoryPagingController() {
    }

    public static void render(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || screen instanceof CreativeModeInventoryScreen
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
            state.open = true;
            request(state, 0, true);
        }
        drawScrollbar(event.getGuiGraphics(), screen, grid, state.row);
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
        return new Grid(slots.getFirst().x, slots.getFirst().y, rightmostSlotX);
    }

    private static void request(State state, int row, boolean force) {
        int target = Math.max(0, Math.min(row, CreativeInventoryPaging.maxRow()));
        if (!force && target == state.row) {
            return;
        }
        state.row = target;
        PacketDistributor.sendToServer(new CreativeInventoryPageRequestPayload(target));
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
        int x = screen.getGuiLeft() + grid.rightmostSlotX + 19;
        int y = screen.getGuiTop() + grid.y - 1;
        graphics.blit(INVENTORY_TEXTURE, x, y, 52, 166, 8, 18);
        graphics.blit(INVENTORY_TEXTURE, x, y + 18, 44, 166, 8, 18);
        graphics.blit(INVENTORY_TEXTURE, x, y + 36, 36, 166, 8, 18);
        int knobY = y + Math.round((float) row / CreativeInventoryPaging.maxRow() * 46.0F);
        graphics.blit(INVENTORY_TEXTURE, x, knobY, 60, 166, KNOB_SIZE, KNOB_SIZE);
    }

    private record Grid(int x, int y, int rightmostSlotX) {
    }

    private static final class State {
        private int row;
        private boolean open;
        private boolean dragging;
        private Grid grid;
    }
}
