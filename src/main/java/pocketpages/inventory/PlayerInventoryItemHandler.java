package pocketpages.inventory;

import pocketpages.integration.CuriosCompat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.fml.ModList;
import net.minecraft.world.item.ItemStack;

/** Standard NeoForge view of a player's vanilla and PocketPages storage. */
public final class PlayerInventoryItemHandler extends CombinedInvWrapper {
    private final Inventory inventory;

    public PlayerInventoryItemHandler(Inventory inventory) {
        super(allHandlers(inventory));
        this.inventory = inventory;
    }

    private static IItemHandlerModifiable[] allHandlers(Inventory inventory) {
        List<IItemHandlerModifiable> handlers = new ArrayList<>();
        handlers.add(vanillaRange(inventory, 0, Inventory.INVENTORY_SIZE));
        handlers.add(vanillaRange(inventory, Inventory.INVENTORY_SIZE,
                Inventory.INVENTORY_SIZE + inventory.armor.size()));
        handlers.add(vanillaRange(inventory, Inventory.INVENTORY_SIZE + inventory.armor.size(),
                Inventory.INVENTORY_SIZE + inventory.armor.size() + inventory.offhand.size()));
        handlers.add(new VirtualInventoryItemHandler(inventory));
        if (ModList.get().isLoaded("curios")) {
            CuriosCompat.append(inventory.player, handlers);
        }
        return handlers.toArray(IItemHandlerModifiable[]::new);
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
