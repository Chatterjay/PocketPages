package pocketpages.mixin;

import pocketpages.DebugLog;
import pocketpages.PocketPages;
import pocketpages.inventory.CreativeInventoryPaging;
import pocketpages.inventory.PocketPagesInventoryData;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
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

    @Inject(method = "getItem", at = @At("HEAD"), cancellable = true)
    private void pocketpages$readMappedStorage(CallbackInfoReturnable<ItemStack> callback) {
        int storageSlot = pocketpages$mappedStorageSlot();
        if (storageSlot >= 0 && container instanceof Inventory inventory) {
            int physicalSlot = storageSlot + 9;
            if (physicalSlot < inventory.items.size()) {
                callback.setReturnValue(inventory.items.get(physicalSlot));
            }
        }
    }

    @Inject(method = "set", at = @At("HEAD"), cancellable = true)
    private void pocketpages$writeMappedStorage(ItemStack stack,
                                                 org.spongepowered.asm.mixin.injection.callback.CallbackInfo callback) {
        int storageSlot = pocketpages$mappedStorageSlot();
        if (storageSlot >= 0 && container instanceof Inventory inventory) {
            int physicalSlot = storageSlot + 9;
            if (physicalSlot < inventory.items.size()) {
                DebugLog.debug("[Paging][Server] mapped write menuSlot={} storageSlot={} physicalSlot={} old={} new={}",
                        ((Slot) (Object) this).getSlotIndex(), storageSlot, physicalSlot,
                        DebugLog.stack(inventory.items.get(physicalSlot)), DebugLog.stack(stack));
                inventory.items.set(physicalSlot, stack);
                inventory.setChanged();
                callback.cancel();
            }
        }
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void pocketpages$removeMappedStorage(int amount,
                                                   CallbackInfoReturnable<ItemStack> callback) {
        int storageSlot = pocketpages$mappedStorageSlot();
        if (storageSlot >= 0 && container instanceof Inventory inventory) {
            int physicalSlot = storageSlot + 9;
            if (physicalSlot < inventory.items.size()) {
                ItemStack removed = ContainerHelper.removeItem(inventory.items, physicalSlot, amount);
                DebugLog.debug("[Paging][Server] mapped remove menuSlot={} storageSlot={} physicalSlot={} amount={} removed={}",
                        ((Slot) (Object) this).getSlotIndex(), storageSlot, physicalSlot, amount,
                        DebugLog.stack(removed));
                inventory.setChanged();
                callback.setReturnValue(removed);
            }
        }
    }

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void pocketpages$rejectLockedPlacement(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (pocketpages$isMappedSlot()) {
            if (pocketpages$isMappedSlotLocked()
                    || stack.is(PocketPages.LOCKED_SLOT.asItem())
                    || !PocketPagesInventoryData.canInsertIntoVirtualSlot(stack)) {
                callback.setReturnValue(false);
            } else {
                callback.setReturnValue(true);
            }
        }
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void pocketpages$rejectLockedPickup(Player player, CallbackInfoReturnable<Boolean> callback) {
        if (pocketpages$isMappedSlot()) {
            callback.setReturnValue(!pocketpages$isMappedSlotLocked());
        }
    }

    private boolean pocketpages$isMappedSlotLocked() {
        int storageSlot = pocketpages$mappedStorageSlot();
        return storageSlot >= 0 && container instanceof Inventory inventory
                && storageSlot >= PocketPagesInventoryData.getUnlocked(inventory.player);
    }

    private boolean pocketpages$isMappedSlot() {
        return pocketpages$mappedStorageSlot() >= 0;
    }

    private int pocketpages$mappedStorageSlot() {
        return CreativeInventoryPaging.getMappedStorageSlot((Slot) (Object) this);
    }
}
