package infiniteinvo.inventory;

import net.minecraft.world.Container;

public interface MutableSlotAccess {
    void infiniteinvo$setContainer(Container container);

    void infiniteinvo$setContainerSlot(int slot);
}
