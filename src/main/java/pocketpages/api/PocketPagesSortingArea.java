package pocketpages.api;

/**
 * A player-owned region exposed by PocketPages's currently open menu.
 *
 * <p>Sorting integrations should use the focused region when one is present,
 * rather than infer ownership from a fixed range of vanilla slot indices.</p>
 */
public enum PocketPagesSortingArea {
    /** The visible page of the player's main PocketPages storage. */
    PLAYER_STORAGE,
    /** The player's normal nine-slot hotbar. */
    PLAYER_HOTBAR,
    /** The four humanoid armor slots. */
    PLAYER_ARMOR,
    /** The player's offhand slot. */
    PLAYER_OFFHAND
}
