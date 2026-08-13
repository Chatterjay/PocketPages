package infiniteinvo.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

/** Standard NeoForge view of a player's vanilla and InfiniteInvo storage. */
public final class PlayerInventoryItemHandler extends CombinedInvWrapper {
    public PlayerInventoryItemHandler(Inventory inventory) {
        super(new InvWrapper(inventory), new VirtualInventoryItemHandler(inventory));
    }
}
