package pocketpages.mixin.client;

import pocketpages.inventory.MutableSlotAccess;
import pocketpages.inventory.WrappedSlotAccess;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen$SlotWrapper")
abstract class CreativeSlotWrapperMixin implements WrappedSlotAccess, MutableSlotAccess {
    @Shadow @Final Slot target;

    @Override
    public Slot pocketpages$getTargetSlot() {
        return target;
    }

    @Override
    public void pocketpages$setContainer(Container container) {
        ((MutableSlotAccess) target).pocketpages$setContainer(container);
    }

    @Override
    public void pocketpages$setContainerSlot(int slot) {
        ((MutableSlotAccess) target).pocketpages$setContainerSlot(slot);
    }
}
