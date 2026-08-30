package pocketpages.mixin.client;

import pocketpages.client.ContainerInventoryPagingController;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Disables interaction feedback for the page-fill slots of a mapped container. */
@Mixin(Slot.class)
abstract class ClientMappedPlayerSlotLockMixin {
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void pocketpages$rejectLockedMappedPlacement(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (pocketpages$isLockedMappedSlot()) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void pocketpages$rejectLockedMappedPickup(
            net.minecraft.world.entity.player.Player player, CallbackInfoReturnable<Boolean> callback) {
        if (pocketpages$isLockedMappedSlot()) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "isHighlightable", at = @At("HEAD"), cancellable = true)
    private void pocketpages$hideLockedMappedHighlight(CallbackInfoReturnable<Boolean> callback) {
        if (pocketpages$isLockedMappedSlot()) {
            callback.setReturnValue(false);
        }
    }

    private boolean pocketpages$isLockedMappedSlot() {
        return ContainerInventoryPagingController.isMappedSlotLocked((Slot) (Object) this);
    }
}
