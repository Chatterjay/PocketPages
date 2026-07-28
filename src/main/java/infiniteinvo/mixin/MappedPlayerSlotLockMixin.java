package infiniteinvo.mixin;

import infiniteinvo.inventory.CreativeInventoryPaging;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents a normal menu from writing into a locked virtual slot mapped onto player storage. */
@Mixin(Slot.class)
abstract class MappedPlayerSlotLockMixin {
    @Shadow @Final public Container container;
    @Shadow @Final private int slot;

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void infiniteinvo$rejectLockedMappedSlot(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (isLockedMappedSlot()) {
            callback.setReturnValue(false);
        }
    }

    /**
     * Some inventory movers write through Slot#set rather than using the menu's
     * normal insertion path. A capacity lock must also reject those writes.
     */
    @Inject(method = "set", at = @At("HEAD"), cancellable = true)
    private void infiniteinvo$rejectLockedMappedSlotWrite(ItemStack stack, CallbackInfo callback) {
        if (!stack.isEmpty() && isLockedMappedSlot()) {
            callback.cancel();
        }
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void infiniteinvo$rejectLockedMappedSlotPickup(net.minecraft.world.entity.player.Player player,
                                                            CallbackInfoReturnable<Boolean> callback) {
        if (isLockedMappedSlot() && getItem().isEmpty()) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void infiniteinvo$rejectLockedMappedSlotRemoval(int amount, CallbackInfoReturnable<ItemStack> callback) {
        if (isLockedMappedSlot() && getItem().isEmpty()) {
            callback.setReturnValue(ItemStack.EMPTY);
        }
    }

    @Shadow public abstract ItemStack getItem();

    private boolean isLockedMappedSlot() {
        return container instanceof Inventory inventory
                && inventory.player instanceof ServerPlayer player
                && CreativeInventoryPaging.isMappedSlotLocked(player, slot);
    }
}
