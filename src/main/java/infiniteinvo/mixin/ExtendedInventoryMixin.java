package infiniteinvo.mixin;

import infiniteinvo.inventory.ExtendedInventory;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Inventory.class)
abstract class ExtendedInventoryMixin implements infiniteinvo.inventory.ExtendedInventoryAccess {
    private static final int VANILLA_MAIN_INVENTORY_SIZE = 36;

    @Shadow @Final @Mutable public NonNullList<ItemStack> items;
    @Shadow @Final public NonNullList<ItemStack> armor;
    @Shadow @Final public NonNullList<ItemStack> offhand;
    @Shadow @Final @Mutable private List<NonNullList<ItemStack>> compartments;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void infiniteinvo$expand(CallbackInfo callback) {
        ExtendedInventory.ensure((Inventory) (Object) this);
    }

    @Override
    public void infiniteinvo$replaceItems(NonNullList<ItemStack> items) {
        this.items = items;
        this.compartments = ImmutableList.of(new infiniteinvo.inventory.VanillaItemListView(this.items), this.armor, this.offhand);
    }

    @Inject(method = "load", at = @At("HEAD"))
    private void infiniteinvo$beginVanillaLoad(ListTag list, CallbackInfo callback) {
        ExtendedInventory.beginLoading(((Inventory) (Object) this).player);
    }

    /** Keeps vanilla's 100/150 equipment ids out of the expanded main-item list. */
    @Redirect(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;size()I"))
    private int infiniteinvo$limitLoadedMainSlots(NonNullList<ItemStack> list) {
        return list == this.items ? VANILLA_MAIN_INVENTORY_SIZE : list.size();
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void infiniteinvo$restoreExtended(ListTag list, CallbackInfo callback) {
        Inventory inventory = (Inventory) (Object) this;
        try {
            ExtendedInventory.restoreAfterVanillaLoad(inventory.player);
        } finally {
            ExtendedInventory.finishLoading(inventory.player);
        }
    }

    @Inject(method = "setChanged", at = @At("RETURN"))
    private void infiniteinvo$markDirty(CallbackInfo callback) {
        Inventory inventory = (Inventory) (Object) this;
        if (!ExtendedInventory.isLoading(inventory.player)) {
            infiniteinvo.inventory.InfiniteInventoryData.markDirty(inventory.player);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void infiniteinvo$tickExpandedSlots(CallbackInfo callback) {
        Inventory inventory = (Inventory) (Object) this;
        for (int slot = 36; slot < items.size(); slot++) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty()) {
                stack.inventoryTick(inventory.player.level(), inventory.player, slot, false);
            }
        }
    }

    @Inject(method = "dropAll", at = @At("TAIL"))
    private void infiniteinvo$dropExpandedSlots(CallbackInfo callback) {
        Inventory inventory = (Inventory) (Object) this;
        for (int slot = 36; slot < items.size(); slot++) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty()) {
                inventory.player.drop(stack, true, false);
                items.set(slot, ItemStack.EMPTY);
            }
        }
    }

    @Inject(method = "clearContent", at = @At("TAIL"))
    private void infiniteinvo$clearExpandedSlots(CallbackInfo callback) {
        for (int slot = 36; slot < items.size(); slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        infiniteinvo.inventory.ExtendedInventory.syncAll(((Inventory) (Object) this).player);
    }

    @Inject(method = "getContainerSize", at = @At("HEAD"), cancellable = true)
    private void infiniteinvo$keepVanillaContainerSize(CallbackInfoReturnable<Integer> callback) {
        callback.setReturnValue(36 + armor.size() + offhand.size());
    }

    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    private void infiniteinvo$saveVanillaSlotsOnly(ListTag list, CallbackInfoReturnable<ListTag> callback) {
        Inventory inventory = (Inventory) (Object) this;
        for (int slot = 0; slot < Math.min(VANILLA_MAIN_INVENTORY_SIZE, items.size()); slot++) {
            saveStack(list, items.get(slot), slot);
        }
        for (int slot = 0; slot < inventory.armor.size(); slot++) {
            saveStack(list, inventory.armor.get(slot), slot + 100);
        }
        for (int slot = 0; slot < inventory.offhand.size(); slot++) {
            saveStack(list, inventory.offhand.get(slot), slot + 150);
        }
        callback.setReturnValue(list);
    }

    private void saveStack(ListTag list, ItemStack stack, int slot) {
        if (stack.isEmpty()) {
            return;
        }
        Inventory inventory = (Inventory) (Object) this;
        CompoundTag entry = new CompoundTag();
        entry.putByte("Slot", (byte) slot);
        list.add(stack.save(inventory.player.registryAccess(), entry));
    }
}
