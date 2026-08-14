package infiniteinvo.mixin.client;

import infiniteinvo.DebugLog;
import infiniteinvo.inventory.WrappedSlotAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sends creative hotbar drops through the server-authoritative creative packets. */
@Mixin(CreativeModeInventoryScreen.class)
abstract class CreativeHotbarDropMixin {
    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void infiniteinvo$dropCreativeHotbarItem(Slot slot, int slotId, int mouseButton, ClickType clickType,
                                                      CallbackInfo callback) {
        if (clickType != ClickType.THROW || slot == null || slot instanceof WrappedSlotAccess) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gameMode == null
                || slot.container != minecraft.player.getInventory()
                || slot.getContainerSlot() < 0 || slot.getContainerSlot() >= 9
                || !slot.hasItem()) {
            return;
        }

        ItemStack current = slot.getItem();
        int amount = mouseButton == 0 ? 1 : current.getCount();
        ItemStack dropped = current.copyWithCount(amount);
        ItemStack remaining = amount >= current.getCount()
                ? ItemStack.EMPTY
                : current.copyWithCount(current.getCount() - amount);
        slot.setByPlayer(remaining);

        // Creative inventory slots use the real player-inventory menu numbering
        // for server updates; the client picker menu has a different slot index.
        int serverSlot = 36 + slot.getContainerSlot();
        minecraft.gameMode.handleCreativeModeItemDrop(dropped);
        minecraft.gameMode.handleCreativeModeItemAdd(remaining, serverSlot);
        DebugLog.debug("[CreativeDrop] hotbar slot={} serverSlot={} dropped={} remaining={}",
                slot.getContainerSlot(), serverSlot, DebugLog.stack(dropped), DebugLog.stack(remaining));
        callback.cancel();
    }
}
