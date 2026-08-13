package infiniteinvo.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.minecraft.world.item.ItemStack;

/** Standard NeoForge view of a player's vanilla and InfiniteInvo storage. */
public final class PlayerInventoryItemHandler extends CombinedInvWrapper {
    private final Inventory inventory;

    public PlayerInventoryItemHandler(Inventory inventory) {
        super(
                vanillaRange(inventory, 0, Inventory.INVENTORY_SIZE),
                vanillaRange(inventory, Inventory.INVENTORY_SIZE, Inventory.INVENTORY_SIZE + inventory.armor.size()),
                vanillaRange(inventory, Inventory.INVENTORY_SIZE + inventory.armor.size(),
                        Inventory.INVENTORY_SIZE + inventory.armor.size() + inventory.offhand.size()),
                new VirtualInventoryItemHandler(inventory)
        );
        this.inventory = inventory;
    }

    private static IItemHandlerModifiable vanillaRange(Inventory inventory, int start, int end) {
        ExtendedInventory.ensure(inventory);
        return new RangedWrapper(new InvWrapper(inventory), start, end);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        ItemStack remainder = super.insertItem(slot, stack, simulate);
        if (!simulate && remainder.getCount() < stack.getCount()) {
            syncMenus();
        }
        return remainder;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack extracted = super.extractItem(slot, amount, simulate);
        if (!simulate && !extracted.isEmpty()) {
            syncMenus();
        }
        return extracted;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        super.setStackInSlot(slot, stack);
        syncMenus();
    }

    private void syncMenus() {
        if (inventory.player instanceof ServerPlayer serverPlayer) {
            serverPlayer.inventoryMenu.broadcastChanges();
            if (serverPlayer.containerMenu != serverPlayer.inventoryMenu) {
                serverPlayer.containerMenu.broadcastChanges();
            }
        }
    }
}
