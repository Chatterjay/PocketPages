package pocketpages.mixin;

import pocketpages.inventory.PlayerInventoryItemHandler;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.wrapper.PlayerInvWrapper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Makes NeoForge's vanilla player capability expose PocketPages slots. */
@Mixin(PlayerInvWrapper.class)
abstract class PlayerInvWrapperMixin {
    @Unique
    private PlayerInventoryItemHandler pocketpages$delegate;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/player/Inventory;)V", at = @At("RETURN"))
    private void pocketpages$createDelegate(Inventory inventory, CallbackInfo callback) {
        pocketpages$delegate = new PlayerInventoryItemHandler(inventory);
    }

    public int getSlots() {
        return pocketpages$delegate == null ? 0 : pocketpages$delegate.getSlots();
    }

    public ItemStack getStackInSlot(int slot) {
        return pocketpages$delegate == null ? ItemStack.EMPTY : pocketpages$delegate.getStackInSlot(slot);
    }

    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return pocketpages$delegate == null ? stack : pocketpages$delegate.insertItem(slot, stack, simulate);
    }

    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return pocketpages$delegate == null
                ? ItemStack.EMPTY
                : pocketpages$delegate.extractItem(slot, amount, simulate);
    }

    public void setStackInSlot(int slot, ItemStack stack) {
        if (pocketpages$delegate != null) {
            pocketpages$delegate.setStackInSlot(slot, stack);
        }
    }

    public int getSlotLimit(int slot) {
        return pocketpages$delegate == null ? 0 : pocketpages$delegate.getSlotLimit(slot);
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        return pocketpages$delegate != null && pocketpages$delegate.isItemValid(slot, stack);
    }
}
