package infiniteinvo.mixin.client;

import infiniteinvo.client.QuickCraftCancellation;
import java.util.Set;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Prevents a quick-craft preview from surviving a player-inventory page remap. */
@Mixin(AbstractContainerScreen.class)
abstract class ClientQuickCraftResetMixin implements QuickCraftCancellation {
    @Shadow @Final protected Set<Slot> quickCraftSlots;
    @Shadow protected boolean isQuickCrafting;
    @Shadow private boolean skipNextRelease;

    @Override
    public void infiniteinvo$cancelQuickCraft() {
        if (!isQuickCrafting) {
            return;
        }
        isQuickCrafting = false;
        quickCraftSlots.clear();
        skipNextRelease = true;
    }
}
