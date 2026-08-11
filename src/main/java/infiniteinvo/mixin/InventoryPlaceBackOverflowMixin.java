package infiniteinvo.mixin;

import infiniteinvo.inventory.InfiniteInventoryData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Inserts leftovers into virtual slots before vanilla drops them. */
@Mixin(Inventory.class)
abstract class InventoryPlaceBackOverflowMixin {
    @Shadow @Final public Player player;

    @Redirect(
            method = "placeItemBackInInventory(Lnet/minecraft/world/item/ItemStack;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;"
            )
    )
    private ItemEntity infiniteinvo$insertOverflowBeforeDrop(Player player, ItemStack stack, boolean throwRandomly) {
        if (this.player instanceof ServerPlayer serverPlayer && !stack.isEmpty()) {
            InfiniteInventoryData.insertOverflow(serverPlayer, stack);
        }
        return stack.isEmpty() ? null : player.drop(stack, throwRandomly);
    }
}
