package infiniteinvo.mixin;

import infiniteinvo.inventory.InfiniteInventoryItemHandler;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes PlayerMainInvWrapper expose the same overflow slots as a direct InvWrapper. */
@Mixin(PlayerMainInvWrapper.class)
abstract class PlayerMainInvWrapperOverflowMixin {
    @Shadow @Final private Inventory inventoryPlayer;

    public int getSlots() {
        return InfiniteInventoryItemHandler.getSlots(inventoryPlayer);
    }

    public ItemStack getStackInSlot(int slot) {
        return InfiniteInventoryItemHandler.isOverflowSlot(inventoryPlayer, slot)
                ? InfiniteInventoryItemHandler.getStackInSlot(inventoryPlayer, slot)
                : inventoryPlayer.getItem(slot);
    }

    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (InfiniteInventoryItemHandler.isOverflowSlot(inventoryPlayer, slot)) {
            return InfiniteInventoryItemHandler.extractItem(inventoryPlayer, slot, amount, simulate);
        }
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = inventoryPlayer.getItem(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return simulate ? stack.copyWithCount(Math.min(amount, stack.getCount())) : inventoryPlayer.removeItem(slot, amount);
    }

    public void setStackInSlot(int slot, ItemStack stack) {
        if (InfiniteInventoryItemHandler.isOverflowSlot(inventoryPlayer, slot)) {
            InfiniteInventoryItemHandler.setStackInSlot(inventoryPlayer, slot, stack);
        } else {
            inventoryPlayer.setItem(slot, stack);
        }
    }

    public int getSlotLimit(int slot) {
        return InfiniteInventoryItemHandler.isOverflowSlot(inventoryPlayer, slot)
                ? InfiniteInventoryItemHandler.getSlotLimit(inventoryPlayer, slot)
                : inventoryPlayer.getMaxStackSize();
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        return InfiniteInventoryItemHandler.isOverflowSlot(inventoryPlayer, slot)
                ? InfiniteInventoryItemHandler.isItemValid(inventoryPlayer, slot, stack)
                : inventoryPlayer.canPlaceItem(slot, stack);
    }

    @Inject(method = "insertItem", at = @At("HEAD"), cancellable = true)
    private void infiniteinvo$insertOverflow(int slot, ItemStack stack, boolean simulate, CallbackInfoReturnable<ItemStack> callback) {
        if (InfiniteInventoryItemHandler.isOverflowSlot(inventoryPlayer, slot)) {
            callback.setReturnValue(InfiniteInventoryItemHandler.insertItem(inventoryPlayer, slot, stack, simulate));
        }
    }
}
