package pocketpages.api;

import pocketpages.PocketPages;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.items.IItemHandler;

/** Public NeoForge capabilities exposed by PocketPages. */
public final class PocketPagesCapabilities {
    public static final EntityCapability<IItemHandler, Void> VIRTUAL_INVENTORY = EntityCapability.createVoid(
            ResourceLocation.fromNamespaceAndPath(PocketPages.MODID, "virtual_inventory"), IItemHandler.class);

    private PocketPagesCapabilities() {
    }
}
