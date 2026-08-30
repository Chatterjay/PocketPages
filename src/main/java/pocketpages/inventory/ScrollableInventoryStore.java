package pocketpages.inventory;

import pocketpages.Config;
import net.minecraft.world.entity.player.Player;

/** Menu view over the player's real extended Inventory.items slots. */
public final class ScrollableInventoryStore extends PlayerExtraInventoryContainer {

    private ScrollableInventoryStore(Player owner) {
        super(owner);
    }

    static ScrollableInventoryStore load(Player player) {
        return new ScrollableInventoryStore(player);
    }

    @Override
    public int getContainerSize() {
        return Config.totalExtraSlots();
    }

}
