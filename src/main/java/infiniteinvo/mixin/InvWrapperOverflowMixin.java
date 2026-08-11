package infiniteinvo.mixin;

import infiniteinvo.inventory.InfiniteInventoryItemHandler;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Extends generic NeoForge wrappers around a player inventory with overflow slots. */
@Mixin(InvWrapper.class)
abstract class InvWrapperOverflowMixin {
    @Shadow @Final private Container inv;

    @Inject(method = "getSlots", at = @At("HEAD"), cancellable = true)
    private void infiniteinvo$getSlots(CallbackInfoReturnable<Integer> callback) {
        if (inv instanceof Inventory inventory) {
            callback.setReturnValue(InfiniteInventoryItemHandler.getSlots(inventory));
        }
    }

    @Inject(method = "getStackInSlot", at = @At("HEAD"), cancellable = true)
    private void infiniteinvo$getStackInSlot(int slot, CallbackInfoReturnable<ItemStack> callback) {
        if (inv instanceof Inventory inventory && InfiniteInventoryItemHandler.isExposedOverflowSlot(inventory, slot)) {
            callback.setReturnValue(InfiniteInventoryItemHandler.getStackInSlot(inventory, slot));
        }
    }

    @Inject(method = "insertItem", at = @At("HEAD"), cancellable = true)
    private void infiniteinvo$insertItem(int slot, ItemStack stack, boolean simulate, CallbackInfoReturnable<ItemStack> callback) {
        if (inv instanceof Inventory inventory && InfiniteInventoryItemHandler.isExposedOverflowSlot(inventory, slot)) {
            callback.setReturnValue(InfiniteInventoryItemHandler.insertItem(inventory, slot, stack, simulate));
        }
    }

    @Inject(method = "extractItem", at = @At("HEAD"), cancellable = true)
    private void infiniteinvo$extractItem(int slot, int amount, boolean simulate, CallbackInfoReturnable<ItemStack> callback) {
        if (inv instanceof Inventory inventory && InfiniteInventoryItemHandler.isExposedOverflowSlot(inventory, slot)) {
            callback.setReturnValue(InfiniteInventoryItemHandler.extractItem(inventory, slot, amount, simulate));
        }
    }

    @Inject(method = "setStackInSlot", at = @At("HEAD"), cancellable = true)
    private void infiniteinvo$setStackInSlot(int slot, ItemStack stack, CallbackInfo callback) {
        if (inv instanceof Inventory inventory && InfiniteInventoryItemHandler.isExposedOverflowSlot(inventory, slot)) {
            InfiniteInventoryItemHandler.setStackInSlot(inventory, slot, stack);
            callback.cancel();
        }
    }

    @Inject(method = "getSlotLimit", at = @At("HEAD"), cancellable = true)
    private void infiniteinvo$getSlotLimit(int slot, CallbackInfoReturnable<Integer> callback) {
        if (inv instanceof Inventory inventory && InfiniteInventoryItemHandler.isExposedOverflowSlot(inventory, slot)) {
            callback.setReturnValue(InfiniteInventoryItemHandler.getSlotLimit(inventory, slot));
        }
    }

    @Inject(method = "isItemValid", at = @At("HEAD"), cancellable = true)
    private void infiniteinvo$isItemValid(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (inv instanceof Inventory inventory && InfiniteInventoryItemHandler.isExposedOverflowSlot(inventory, slot)) {
            callback.setReturnValue(InfiniteInventoryItemHandler.isItemValid(inventory, slot, stack));
        }
    }
}
