package infiniteinvo.mixin.client;

import infiniteinvo.client.ScrollableInventoryScreen;
import java.util.List;
import java.util.Set;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Excludes locked virtual slots from IPN's sortable storage area. */
@Pseudo
@Mixin(targets = "org.anti_ad.mc.ipnext.inventory.AreaTypes", remap = false)
abstract class IpnAreaTypesMixin {
    @Unique
    private static final ThreadLocal<SlotLists> infiniteinvo$slotLists = new ThreadLocal<>();

    @Inject(method = "fillSlots", at = @At("HEAD"), remap = false)
    private void infiniteinvo$rememberSlotLists(Set<?> types, List<Slot> slots, List<Integer> slotIndices,
                                                CallbackInfo callback) {
        infiniteinvo$slotLists.set(new SlotLists(slots, slotIndices));
    }

    @Inject(method = "fillSlots", at = @At("RETURN"), remap = false)
    private void infiniteinvo$excludeLockedVirtualSlots(CallbackInfo callback) {
        SlotLists lists = infiniteinvo$slotLists.get();
        infiniteinvo$slotLists.remove();
        if (lists == null) {
            return;
        }
        lists.slotIndices().removeIf(index -> index >= 0 && index < lists.slots().size()
                && ScrollableInventoryScreen.isIpnVirtualSlotLocked(lists.slots().get(index)));
    }

    @Unique
    private record SlotLists(List<Slot> slots, List<Integer> slotIndices) {
    }
}
