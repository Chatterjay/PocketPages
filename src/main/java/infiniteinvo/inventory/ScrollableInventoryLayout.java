package infiniteinvo.inventory;

/**
 * Pixel coordinates from the original 1.8.9 InfiniteInvo screen.  Keeping all
 * geometry here prevents the menu hit boxes and the tiled background drifting
 * apart when the interface changes.
 */
public final class ScrollableInventoryLayout {
    public static final int SLOT_SIZE = 18;
    public static final int EXTRA_COLUMNS = 3;
    public static final int EXTRA_ROWS = 3;
    public static final int COLUMNS = 9 + EXTRA_COLUMNS;
    public static final int VISIBLE_ROWS = 3 + EXTRA_ROWS;

    public static final int IMAGE_WIDTH = 169 + EXTRA_COLUMNS * SLOT_SIZE + 15;
    public static final int IMAGE_HEIGHT = 137 + EXTRA_ROWS * SLOT_SIZE + 29;

    public static final int ARMOR_X = 8;
    public static final int ARMOR_Y = 8;
    public static final int CRAFT_X = 88;
    public static final int CRAFT_Y = 43;
    public static final int RESULT_X = 144;
    public static final int RESULT_Y = 53;
    public static final int GRID_X = 8;
    public static final int GRID_Y = 84;
    public static final int GRID_BACKGROUND_X = GRID_X - 1;
    public static final int GRID_BACKGROUND_Y = GRID_Y - 1;
    public static final int HOTBAR_Y = 142 + EXTRA_ROWS * SLOT_SIZE;
    public static final int SCROLL_X = 169 + EXTRA_COLUMNS * SLOT_SIZE;
    public static final int SCROLL_KNOB_X = SCROLL_X + 2;
    public static final int SCROLL_Y = GRID_BACKGROUND_Y;
    public static final int SCROLL_HEIGHT = VISIBLE_ROWS * SLOT_SIZE;
    public static final int SCROLL_KNOB_TRAVEL = SCROLL_HEIGHT - 8;
    public static final int UNLOCK_BUTTON_X = 87;
    public static final int UNLOCK_BUTTON_Y = 7;

    private ScrollableInventoryLayout() {
    }
}
