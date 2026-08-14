package infiniteinvo.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps AE2 terminal menus from treating InfiniteInvo's virtual storage as native player slots. */
@Pseudo
@Mixin(targets = "appeng.menu.AEBaseMenu", remap = false)
abstract class Ae2PlayerInventorySlotsMixin {
    private static final int VANILLA_MAIN_INVENTORY_SIZE = 36;

    @Redirect(
            method = "createPlayerInventorySlots",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;size()I"),
            remap = false)
    private int infiniteinvo$limitTerminalPlayerSlots(NonNullList<ItemStack> items) {
        return Math.min(VANILLA_MAIN_INVENTORY_SIZE, items.size());
    }
}
