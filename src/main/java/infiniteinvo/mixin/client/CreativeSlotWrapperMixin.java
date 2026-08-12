package infiniteinvo.mixin.client;

import infiniteinvo.inventory.MutableSlotAccess;
import infiniteinvo.inventory.WrappedSlotAccess;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen$SlotWrapper")
abstract class CreativeSlotWrapperMixin implements WrappedSlotAccess, MutableSlotAccess {
    @Shadow @Final Slot target;

    @Override
    public Slot infiniteinvo$getTargetSlot() {
        return target;
    }

    @Override
    public void infiniteinvo$setContainer(Container container) {
        ((MutableSlotAccess) target).infiniteinvo$setContainer(container);
    }

    @Override
    public void infiniteinvo$setContainerSlot(int slot) {
        ((MutableSlotAccess) target).infiniteinvo$setContainerSlot(slot);
    }
}
