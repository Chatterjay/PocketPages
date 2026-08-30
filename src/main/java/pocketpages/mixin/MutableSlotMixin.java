package pocketpages.mixin;

import pocketpages.inventory.MutableSlotAccess;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Slot.class)
abstract class MutableSlotMixin implements MutableSlotAccess {
    @Shadow @Final @Mutable public Container container;
    @Shadow @Final @Mutable private int slot;

    @Override
    public void pocketpages$setContainer(Container container) {
        this.container = container;
    }

    @Override
    public void pocketpages$setContainerSlot(int slot) {
        this.slot = slot;
    }
}
