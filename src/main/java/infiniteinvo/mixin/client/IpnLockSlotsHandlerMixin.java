package infiniteinvo.mixin.client;

import infiniteinvo.client.IpnCompat;
import infiniteinvo.client.ScrollableInventoryScreen;
import java.util.Map;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional IPN bridge: virtual slots join IPN's native lock model. */
@Pseudo
@Mixin(targets = "org.anti_ad.mc.ipnext.event.LockSlotsHandler", remap = false)
abstract class IpnLockSlotsHandlerMixin {
    @Inject(method = "getSlotLocations", at = @At("RETURN"), cancellable = true, remap = false)
    @SuppressWarnings("rawtypes")
    private void infiniteinvo$includeVisibleVirtualSlots(CallbackInfoReturnable<Map> callback) {
        callback.setReturnValue(IpnCompat.appendVisibleVirtualSlotLocations(callback.getReturnValue()));
    }

    @Inject(method = "isMappedSlotLocked", at = @At("RETURN"), cancellable = true, remap = false)
    private void infiniteinvo$recognizeVirtualLock(Slot slot, CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValue() && ScrollableInventoryScreen.isIpnVirtualSlotLocked(slot)) {
            callback.setReturnValue(true);
        }
    }
}
