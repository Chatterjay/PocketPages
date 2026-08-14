package infiniteinvo.mixin;

import infiniteinvo.inventory.ExtendedInventoryContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents mapped page-fill slots from being used through direct server clicks. */
@Mixin(Slot.class)
abstract class MappedPlayerSlotLockMixin {
    @Shadow @Final public Container container;
    @Shadow @Final private int slot;

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void infiniteinvo$rejectLockedPlacement(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (container instanceof ExtendedInventoryContainer extended && !extended.isUnlocked(slot)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void infiniteinvo$rejectLockedPickup(Player player, CallbackInfoReturnable<Boolean> callback) {
        if (container instanceof ExtendedInventoryContainer extended && !extended.isUnlocked(slot)) {
            callback.setReturnValue(false);
        }
    }
}
