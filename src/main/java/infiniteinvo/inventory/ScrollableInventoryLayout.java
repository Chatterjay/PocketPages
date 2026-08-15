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
    public static final int CRAFT_X = 103;
    public static final int CRAFT_Y = 44;
    public static final int RESULT_X = 159;
    public static final int RESULT_Y = 54;
    public static final int OFFHAND_X = 81;
    public static final int OFFHAND_Y = 62;
    public static final int PLAYER_RENDER_LEFT_X = 26;
    public static final int PLAYER_RENDER_TOP_Y = 8;
    public static final int PLAYER_RENDER_RIGHT_X = 75;
    public static final int PLAYER_RENDER_BOTTOM_Y = 78;
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
    /** Reusable interior grid pieces from the original inventory background. */
    public static final int FRAMELESS_BACKGROUND_SPRITE_X = 79;
    public static final int HEADER_BACKGROUND_SPRITE_Y = 0;
    public static final int FRAMELESS_BACKGROUND_SPRITE_Y = 3;
    public static final int GRID_INTERIOR_SPRITE_X = 25;
    public static final int GRID_TOP_INTERIOR_SPRITE_Y = GRID_BACKGROUND_Y;
    public static final int GRID_MIDDLE_INTERIOR_SPRITE_Y = GRID_BACKGROUND_Y + SLOT_SIZE;
    public static final int GRID_BOTTOM_INTERIOR_SPRITE_Y = GRID_BACKGROUND_Y + 2 * SLOT_SIZE;
    public static final int HOVER_SPRITE_X = 112;
    public static final int HOVER_SPRITE_Y = 166;
    public static final int HOVER_SPRITE_SIZE = 16;
    public static final int CRAFTING_LABEL_X = 102;
    public static final int CRAFTING_LABEL_Y = 32;
    public static final int RECIPE_BOOK_BUTTON_WIDTH = SLOT_SIZE;
    public static final int RECIPE_BOOK_BUTTON_HEIGHT = SLOT_SIZE;
    public static final int UNLOCK_BUTTON_X = 87;
    public static final int UNLOCK_BUTTON_Y = 7;

    private ScrollableInventoryLayout() {
    }

}
