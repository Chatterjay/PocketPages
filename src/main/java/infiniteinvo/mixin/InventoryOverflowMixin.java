package infiniteinvo.mixin;

import infiniteinvo.inventory.InfiniteInventoryData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sends the remainder of vanilla inventory insertion to the real extended slots. */
@Mixin(Inventory.class)
abstract class InventoryOverflowMixin {
    @Shadow @Final public Player player;

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true)
    private void infiniteinvo$insertOverflow(int preferredSlot, ItemStack stack,
                                              CallbackInfoReturnable<Boolean> callback) {
        if (player instanceof ServerPlayer serverPlayer && !stack.isEmpty()
                && InfiniteInventoryData.insertOverflow(serverPlayer, stack) > 0) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "placeItemBackInInventory(Lnet/minecraft/world/item/ItemStack;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void infiniteinvo$placeBackIntoOverflow(ItemStack stack, boolean sendPacket,
                                                     org.spongepowered.asm.mixin.injection.callback.CallbackInfo callback) {
        if (player instanceof ServerPlayer serverPlayer) {
            Inventory inventory = (Inventory) (Object) this;
            inventory.add(stack);
            if (!stack.isEmpty()) {
                player.drop(stack, false);
            }
            callback.cancel();
        }
    }

    @Inject(method = "clearOrCountMatchingItems", at = @At("RETURN"), cancellable = true)
    private void infiniteinvo$clearOverflow(java.util.function.Predicate<ItemStack> predicate,
                                             int maxCount, net.minecraft.world.Container container,
                                             CallbackInfoReturnable<Integer> callback) {
        if (player instanceof ServerPlayer serverPlayer) {
            int vanillaCount = callback.getReturnValue();
            if (maxCount > 0 && vanillaCount >= maxCount) {
                return;
            }
            int remaining = maxCount == 0 ? 0 : maxCount < 0 ? -1 : Math.max(0, maxCount - vanillaCount);
            int overflowCount = InfiniteInventoryData.clearOrCountMatchingOverflow(
                    serverPlayer, predicate, remaining, -1, -1);
            callback.setReturnValue(vanillaCount + overflowCount);
        }
    }
}
