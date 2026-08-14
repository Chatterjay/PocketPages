package infiniteinvo.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps a single-item hotbar drop from leaving a zero-count stack in the slot. */
@Mixin(Inventory.class)
abstract class SelectedItemDropMixin {
    @Shadow @Final public NonNullList<ItemStack> items;
    @Shadow public int selected;

    @Inject(method = "removeFromSelected(Z)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private void infiniteinvo$normalizeEmptySelectedSlot(boolean removeStack,
                                                          CallbackInfoReturnable<ItemStack> callback) {
        infiniteinvo$normalizeEmptyHotbarSlot(selected);
    }

    @Inject(method = "removeItem(II)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private void infiniteinvo$normalizeEmptyRemovedHotbarSlot(int slot, int amount,
                                                               CallbackInfoReturnable<ItemStack> callback) {
        infiniteinvo$normalizeEmptyHotbarSlot(slot);
    }

    @SuppressWarnings("ConstantValue")
    private void infiniteinvo$normalizeEmptyHotbarSlot(int slot) {
        if (slot >= 0 && slot < 9 && items.get(slot).isEmpty() && items.get(slot) != ItemStack.EMPTY) {
            items.set(slot, ItemStack.EMPTY);
            ((Inventory) (Object) this).setChanged();
        }
    }
}
