package pocketpages.mixin.client;

import pocketpages.client.IpnCompat;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps IPN's per-slot refill indicator on the vanilla hotbar only. */
@Pseudo
@Mixin(targets = "org.anti_ad.mc.ipnext.event.autorefill.AutoRefillHandler", remap = false)
abstract class IpnAutoRefillHandlerMixin {
    @Inject(method = "getSlotLocations", at = @At("RETURN"), cancellable = true, remap = false)
    @SuppressWarnings("rawtypes")
    private void pocketpages$restrictIndicatorToHotbar(CallbackInfoReturnable<Map> callback) {
        callback.setReturnValue(IpnCompat.restrictAutoRefillToHotbar(callback.getReturnValue()));
    }
}
