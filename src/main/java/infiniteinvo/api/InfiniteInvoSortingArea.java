package infiniteinvo.api;

/**
 * A player-owned region exposed by InfiniteInvo's currently open menu.
 *
 * <p>Sorting integrations should use the focused region when one is present,
 * rather than infer ownership from a fixed range of vanilla slot indices.</p>
 */
public enum InfiniteInvoSortingArea {
    /** The visible page of the player's main InfiniteInvo storage. */
    PLAYER_STORAGE,
    /** The player's normal nine-slot hotbar. */
    PLAYER_HOTBAR,
    /** The four humanoid armor slots. */
    PLAYER_ARMOR,
    /** The player's offhand slot. */
    PLAYER_OFFHAND
}
