package infiniteinvo.network;

import infiniteinvo.InfiniteInvo;
import infiniteinvo.client.ScrollableInventoryScreen;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Confirms a scrollable inventory page and carries its visible contents.
 *
 * The scroll slots are remapped client-side, so vanilla's menu cache cannot
 * reliably tell that identical stacks now belong to different real slots.
 */
public record ScrollableInventoryPageDataPayload(int containerId, int page, int requestId, List<ItemStack> stacks)
        implements CustomPacketPayload {
    public static final Type<ScrollableInventoryPageDataPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(InfiniteInvo.MODID, "scrollable_inventory_page_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ScrollableInventoryPageDataPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ScrollableInventoryPageDataPayload::containerId,
                    ByteBufCodecs.VAR_INT, ScrollableInventoryPageDataPayload::page,
                    ByteBufCodecs.VAR_INT, ScrollableInventoryPageDataPayload::requestId,
                    ItemStack.OPTIONAL_LIST_STREAM_CODEC, ScrollableInventoryPageDataPayload::stacks,
                    ScrollableInventoryPageDataPayload::new);

    @Override
    public Type<ScrollableInventoryPageDataPayload> type() {
        return TYPE;
    }

    public static void handle(ScrollableInventoryPageDataPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ScrollableInventoryScreen.applyServerPage(
                payload.containerId(), payload.page(), payload.requestId(), payload.stacks()));
    }
}
