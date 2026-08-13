package infiniteinvo.mixin;

import infiniteinvo.inventory.PlayerInventoryItemHandler;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.wrapper.PlayerInvWrapper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Makes NeoForge's vanilla player capability expose InfiniteInvo slots. */
@Mixin(PlayerInvWrapper.class)
abstract class PlayerInvWrapperMixin {
    @Unique
    private PlayerInventoryItemHandler infiniteinvo$delegate;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/player/Inventory;)V", at = @At("RETURN"))
    private void infiniteinvo$createDelegate(Inventory inventory, CallbackInfo callback) {
        infiniteinvo$delegate = new PlayerInventoryItemHandler(inventory);
    }

    public int getSlots() {
        return infiniteinvo$delegate == null ? 0 : infiniteinvo$delegate.getSlots();
    }

    public ItemStack getStackInSlot(int slot) {
        return infiniteinvo$delegate == null ? ItemStack.EMPTY : infiniteinvo$delegate.getStackInSlot(slot);
    }

    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return infiniteinvo$delegate == null ? stack : infiniteinvo$delegate.insertItem(slot, stack, simulate);
    }

    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return infiniteinvo$delegate == null
                ? ItemStack.EMPTY
                : infiniteinvo$delegate.extractItem(slot, amount, simulate);
    }

    public void setStackInSlot(int slot, ItemStack stack) {
        if (infiniteinvo$delegate != null) {
            infiniteinvo$delegate.setStackInSlot(slot, stack);
        }
    }

    public int getSlotLimit(int slot) {
        return infiniteinvo$delegate == null ? 0 : infiniteinvo$delegate.getSlotLimit(slot);
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        return infiniteinvo$delegate != null && infiniteinvo$delegate.isItemValid(slot, stack);
    }
}
