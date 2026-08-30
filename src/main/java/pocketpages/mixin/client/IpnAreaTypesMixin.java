package pocketpages.mixin.client;

import pocketpages.client.IpnAreaTypeCompat;
import java.util.List;
import java.util.Set;
import net.minecraft.world.inventory.Slot;
import org.anti_ad.mc.ipnext.inventory.AreaType;
import org.anti_ad.mc.ipnext.inventory.AreaTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Classifies PocketPages's visible slots as player storage for IPN. */
@Pseudo
@Mixin(targets = "org.anti_ad.mc.ipnext.inventory.AreaTypes", remap = false)
abstract class IpnAreaTypesMixin {
    @Inject(method = "fillSlots", at = @At("HEAD"), remap = false)
    private void pocketpages$rememberSlotLists(Set<?> types, List<Slot> slots, List<Integer> slotIndices,
                                                CallbackInfo callback) {
        IpnAreaTypeCompat.rememberFillSlotsArguments(slots, slotIndices);
    }

    @Inject(method = "fillSlots", at = @At("RETURN"), remap = false)
    private void pocketpages$excludeVirtualPlayerSlots(CallbackInfo callback) {
        IpnAreaTypeCompat.finishFillSlots();
    }

    @Inject(method = "getPlayerStorage", at = @At("RETURN"), cancellable = true, remap = false)
    private void pocketpages$includeVirtualPlayerSlots(CallbackInfoReturnable<AreaType> callback) {
        AreaType original = callback.getReturnValue();
        if (original != null) {
            callback.setReturnValue(IpnAreaTypeCompat.wrapPlayerStorage(
                    original, (AreaTypes) (Object) this));
        }
    }

    @Inject(method = "getLockedSlots", at = @At("RETURN"), cancellable = true, remap = false)
    private void pocketpages$includeVirtualLockedSlots(CallbackInfoReturnable<AreaType> callback) {
        AreaType original = callback.getReturnValue();
        if (original != null) {
            callback.setReturnValue(IpnAreaTypeCompat.wrapLockedSlots(original));
        }
    }

}
