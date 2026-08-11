package infiniteinvo.api;

import infiniteinvo.InfiniteInvo;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.items.IItemHandler;

/** Public NeoForge capabilities exposed by InfiniteInvo. */
public final class InfiniteInvoCapabilities {
    public static final EntityCapability<IItemHandler, Void> VIRTUAL_INVENTORY = EntityCapability.createVoid(
            ResourceLocation.fromNamespaceAndPath(InfiniteInvo.MODID, "virtual_inventory"), IItemHandler.class);

    private InfiniteInvoCapabilities() {
    }
}
