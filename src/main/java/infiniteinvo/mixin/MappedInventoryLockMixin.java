package infiniteinvo.mixin;

import infiniteinvo.inventory.CreativeInventoryPaging;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Guards the direct Inventory API paths that container automation can bypass. */
@Mixin(Inventory.class)
abstract class MappedInventoryLockMixin {
    @Shadow @Final public Player player;
    @Shadow @Final public NonNullList<ItemStack> items;

    @Inject(method = "setItem", at = @At("HEAD"), cancellable = true)
    private void infiniteinvo$rejectLockedMappedSet(int index, ItemStack stack, CallbackInfo callback) {
        if (!stack.isEmpty() && isLocked(index)) {
            callback.cancel();
        }
    }

    @Inject(method = "clearContent", at = @At("HEAD"), cancellable = true)
    private void infiniteinvo$rejectLockedMappedClear(CallbackInfo callback) {
        if (player instanceof ServerPlayer serverPlayer && CreativeInventoryPaging.hasMappedLockedSlots(serverPlayer)) {
            callback.cancel();
        }
    }

    @Inject(method = "getFreeSlot", at = @At("RETURN"), cancellable = true)
    private void infiniteinvo$skipLockedMappedFreeSlot(CallbackInfoReturnable<Integer> callback) {
        if (!isLocked(callback.getReturnValue())) {
            return;
        }

        for (int index = 0; index < items.size(); index++) {
            if (items.get(index).isEmpty() && !isLocked(index)) {
                callback.setReturnValue(index);
                return;
            }
        }
        callback.setReturnValue(-1);
    }

    @Inject(method = "getSlotWithRemainingSpace", at = @At("RETURN"), cancellable = true)
    private void infiniteinvo$skipLockedMappedMergeSlot(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        if (isLocked(callback.getReturnValue())) {
            callback.setReturnValue(-1);
        }
    }

    private boolean isLocked(int index) {
        return player instanceof ServerPlayer serverPlayer
                && CreativeInventoryPaging.isMappedSlotLocked(serverPlayer, index);
    }
}
