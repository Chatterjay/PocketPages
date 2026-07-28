package infiniteinvo.network;

import infiniteinvo.InfiniteInvo;
import infiniteinvo.inventory.ScrollableInventoryMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server-bound request sent when the player opens the vanilla inventory keybind. */
public record OpenInfiniteInventoryPayload() implements CustomPacketPayload {
    public static final OpenInfiniteInventoryPayload INSTANCE = new OpenInfiniteInventoryPayload();
    public static final Type<OpenInfiniteInventoryPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(InfiniteInvo.MODID, "open_inventory"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenInfiniteInventoryPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<OpenInfiniteInventoryPayload> type() {
        return TYPE;
    }

    public static void handle(OpenInfiniteInventoryPayload payload, IPayloadContext context) {
        ScrollableInventoryMenu.open(context.player());
    }
}
