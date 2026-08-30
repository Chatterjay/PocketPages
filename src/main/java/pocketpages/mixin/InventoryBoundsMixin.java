package pocketpages.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps vanilla insertion within the original 36 physical main-inventory slots. */
@Mixin(Inventory.class)
abstract class InventoryBoundsMixin {
    private static final int VANILLA_MAIN_SIZE = 36;
    @Shadow @Final public NonNullList<ItemStack> items;

    @Inject(method = "getFreeSlot", at = @At("HEAD"), cancellable = true)
    private void pocketpages$limitFreeSlot(CallbackInfoReturnable<Integer> callback) {
        Inventory inventory = (Inventory) (Object) this;
        int end = Math.min(items.size(), VANILLA_MAIN_SIZE);
        for (int slot = 0; slot < end; slot++) {
            if (items.get(slot).isEmpty() && inventory.canPlaceItem(slot, ItemStack.EMPTY)) {
                callback.setReturnValue(slot);
                return;
            }
        }
        callback.setReturnValue(-1);
    }

    @Inject(method = "getSlotWithRemainingSpace", at = @At("HEAD"), cancellable = true)
    private void pocketpages$limitMergeSlot(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        Inventory inventory = (Inventory) (Object) this;
        int end = Math.min(items.size(), VANILLA_MAIN_SIZE);
        ItemStack selected = items.get(inventory.selected);
        if (hasRoom(selected, stack, inventory)) {
            callback.setReturnValue(inventory.selected);
            return;
        }
        ItemStack offhand = inventory.offhand.getFirst();
        if (hasRoom(offhand, stack, inventory)) {
            callback.setReturnValue(40);
            return;
        }
        for (int slot = 0; slot < end; slot++) {
            ItemStack existing = items.get(slot);
            if (hasRoom(existing, stack, inventory) && inventory.canPlaceItem(slot, stack)) {
                callback.setReturnValue(slot);
                return;
            }
        }
        callback.setReturnValue(-1);
    }

    private static boolean hasRoom(ItemStack existing, ItemStack incoming, Inventory inventory) {
        return !existing.isEmpty() && existing.isStackable()
                && ItemStack.isSameItemSameComponents(existing, incoming)
                && existing.getCount() < inventory.getMaxStackSize(existing);
    }
}
