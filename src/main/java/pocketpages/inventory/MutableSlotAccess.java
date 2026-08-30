package pocketpages.inventory;

import net.minecraft.world.Container;

public interface MutableSlotAccess {
    void pocketpages$setContainer(Container container);

    void pocketpages$setContainerSlot(int slot);
}
