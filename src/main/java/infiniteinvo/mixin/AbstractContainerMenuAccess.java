package infiniteinvo.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Gives the player-only menu reuse of InventoryMenu's private backing state. */
@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccess {
    @Mutable
    @Accessor("menuType")
    void infiniteinvo$setMenuType(MenuType<?> menuType);

    @Mutable
    @Accessor("containerId")
    void infiniteinvo$setContainerId(int containerId);

    @Accessor("lastSlots")
    NonNullList<ItemStack> infiniteinvo$getLastSlots();

    @Accessor("remoteSlots")
    NonNullList<ItemStack> infiniteinvo$getRemoteSlots();
}
