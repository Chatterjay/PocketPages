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
    }
}
