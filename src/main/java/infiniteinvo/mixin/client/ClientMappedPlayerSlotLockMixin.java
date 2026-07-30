package infiniteinvo.mixin.client;

import infiniteinvo.client.ContainerInventoryPagingController;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps local quick-craft previews from selecting a server-locked mapped slot. */
@Mixin(Slot.class)
abstract class ClientMappedPlayerSlotLockMixin {
    @Shadow @Final public Container container;
    @Shadow @Final private int slot;

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void infiniteinvo$rejectLockedMappedPreview(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (container instanceof Inventory inventory
                && !ContainerInventoryPagingController.isMappedSlotUnlocked(inventory, slot)) {
            callback.setReturnValue(false);
        }
    }

}
