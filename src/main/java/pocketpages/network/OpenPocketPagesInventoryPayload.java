package pocketpages.network;

import pocketpages.PocketPages;
import pocketpages.inventory.ScrollableInventoryMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server-bound request sent when the player opens the vanilla inventory keybind. */
public record OpenPocketPagesInventoryPayload() implements CustomPacketPayload {
    public static final OpenPocketPagesInventoryPayload INSTANCE = new OpenPocketPagesInventoryPayload();
    public static final Type<OpenPocketPagesInventoryPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PocketPages.MODID, "open_inventory"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPocketPagesInventoryPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<OpenPocketPagesInventoryPayload> type() {
        return TYPE;
    }

    public static void handle(OpenPocketPagesInventoryPayload payload, IPayloadContext context) {
        ScrollableInventoryMenu.open(context.player());
    }
}
