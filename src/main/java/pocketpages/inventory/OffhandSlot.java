package pocketpages.inventory;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

final class OffhandSlot extends Slot {
    private final Player owner;

    OffhandSlot(Container container, Player owner, int x, int y) {
        super(container, 40, x, y);
        this.owner = owner;
    }

    @Override
    public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
        owner.onEquipItem(EquipmentSlot.OFFHAND, oldStack, newStack);
        super.setByPlayer(newStack, oldStack);
    }

    @Override
    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
    }
}
