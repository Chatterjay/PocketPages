package pocketpages.mixin;

import pocketpages.inventory.PocketPagesInventoryData;
import java.util.function.Predicate;
import net.minecraft.core.NonNullList;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes standard read-only inventory queries see unlocked PocketPages slots.
 * Container sizing and item insertion remain limited to vanilla slots.
 */
@Mixin(Inventory.class)
abstract class InventoryFunctionalScanMixin {
    private static final int FIRST_OVERFLOW_SLOT = Inventory.INVENTORY_SIZE;

    @Shadow @Final public NonNullList<ItemStack> items;
    @Shadow @Final public Player player;

    @Inject(method = "contains(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true)
    private void pocketpages$findMatchingExtendedItem(ItemStack searched,
                                                        CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValue() && pocketpages$matchesExtended(
                stack -> !stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, searched))) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "contains(Lnet/minecraft/tags/TagKey;)Z", at = @At("RETURN"), cancellable = true)
    private void pocketpages$findMatchingExtendedTag(TagKey<Item> tag,
                                                       CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValue() && pocketpages$matchesExtended(
                stack -> !stack.isEmpty() && stack.is(tag))) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "contains(Ljava/util/function/Predicate;)Z", at = @At("RETURN"), cancellable = true)
    private void pocketpages$findMatchingExtendedPredicate(Predicate<ItemStack> predicate,
                                                             CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValue() && pocketpages$matchesExtended(predicate)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "isEmpty", at = @At("RETURN"), cancellable = true)
    private void pocketpages$checkExtendedItems(CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValue() && pocketpages$matchesExtended(stack -> !stack.isEmpty())) {
            callback.setReturnValue(false);
        }
    }

    @Unique
    private boolean pocketpages$matchesExtended(Predicate<ItemStack> predicate) {
        int end = Math.min(items.size(), 9 + PocketPagesInventoryData.getUnlocked(player));
        for (int slot = FIRST_OVERFLOW_SLOT; slot < end; slot++) {
            if (predicate.test(items.get(slot))) {
                return true;
            }
        }
        return false;
    }
}
