package infiniteinvo.mixin.client;

import infiniteinvo.client.VirtualInventoryDropQueue;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Routes standard drop clicks through the extended-inventory synchronization queue. */
@Mixin(MultiPlayerGameMode.class)
abstract class ClientInventoryDropQueueMixin {
    @Inject(method = "handleInventoryMouseClick", at = @At("HEAD"), cancellable = true)
    private void infiniteinvo$queueVirtualInventoryDrops(int containerId, int slotId, int button, ClickType clickType,
                                                         Player player, CallbackInfo callback) {
        if (VirtualInventoryDropQueue.enqueue((MultiPlayerGameMode) (Object) this, containerId, slotId, button,
                clickType, player)) {
            callback.cancel();
        }
    }
}
