package infiniteinvo.mixin;

import infiniteinvo.inventory.InfiniteInventoryData;
import infiniteinvo.inventory.CreativeInventoryPaging;
import infiniteinvo.inventory.ScrollableInventoryMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Routes only the remainder of a normal player-inventory insertion to overflow. */
@Mixin(Inventory.class)
abstract class InventoryOverflowMixin {
    @Shadow @Final public Player player;

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true)
    private void infiniteinvo$insertOverflow(int preferredSlot, ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (!(player instanceof ServerPlayer serverPlayer) || stack.isEmpty()) {
            return;
        }

        if (InfiniteInventoryData.insertOverflow(serverPlayer, stack) > 0) {
            callback.setReturnValue(true);
        }
        infiniteinvo$syncNativeMirror();
    }

    @Inject(method = "setItem", at = @At("RETURN"))
    private void infiniteinvo$syncNativeSetItem(int index, ItemStack stack, CallbackInfo callback) {
        if (player.containerMenu instanceof ScrollableInventoryMenu menu) {
            menu.syncNativeMirrorSlotFromPlayer(player, index);
        }
    }

    @Inject(method = "removeItem(II)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private void infiniteinvo$syncNativeRemoval(int index, int amount, CallbackInfoReturnable<ItemStack> callback) {
        if (player.containerMenu instanceof ScrollableInventoryMenu menu) {
            menu.syncNativeMirrorSlotFromPlayer(player, index);
        }
    }

    @Inject(method = "removeItemNoUpdate", at = @At("RETURN"))
    private void infiniteinvo$syncNativeRemovalNoUpdate(int index, CallbackInfoReturnable<ItemStack> callback) {
        if (player.containerMenu instanceof ScrollableInventoryMenu menu) {
            menu.syncNativeMirrorSlotFromPlayer(player, index);
        }
    }

    @Inject(method = "removeItem(Lnet/minecraft/world/item/ItemStack;)V", at = @At("RETURN"))
    private void infiniteinvo$syncNativeStackRemoval(ItemStack stack, CallbackInfo callback) {
        infiniteinvo$syncNativeMirror();
    }

    @Inject(method = "clearContent", at = @At("RETURN"))
    private void infiniteinvo$syncNativeClear(CallbackInfo callback) {
        infiniteinvo$syncNativeMirror();
    }

    @Inject(method = "clearOrCountMatchingItems", at = @At("RETURN"), cancellable = true)
    private void infiniteinvo$syncNativeClearMatching(java.util.function.Predicate<ItemStack> predicate, int count,
                                                      net.minecraft.world.Container container,
                                                      CallbackInfoReturnable<Integer> callback) {
        if (player instanceof ServerPlayer serverPlayer) {
            int nativeCleared = callback.getReturnValue();
            int mappedStart = CreativeInventoryPaging.captureMappedPage(serverPlayer);
            int overflowCleared = 0;
            if (count == 0 || count < 0 || nativeCleared < count) {
                int remaining = count == 0 ? 0 : count < 0 ? -1 : count - nativeCleared;
                overflowCleared = InfiniteInventoryData.clearOrCountMatchingOverflow(serverPlayer, predicate, remaining,
                        mappedStart, mappedStart < 0 ? -1 : mappedStart + CreativeInventoryPaging.PAGE_SIZE);
            }
            callback.setReturnValue(nativeCleared + overflowCleared);
        }
        infiniteinvo$syncNativeMirror();
    }

    private void infiniteinvo$syncNativeMirror() {
        if (player.containerMenu instanceof ScrollableInventoryMenu menu) {
            menu.syncNativeMirrorFromPlayer(player);
        }
    }
}
