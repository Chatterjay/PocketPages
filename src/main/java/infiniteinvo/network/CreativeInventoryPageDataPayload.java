package infiniteinvo.network;

import infiniteinvo.InfiniteInvo;
import infiniteinvo.DebugLog;
import infiniteinvo.client.CreativeInventoryController;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server response used to refresh the vanilla creative inventory slots after a page swap. */
public record CreativeInventoryPageDataPayload(int row, int unlockedSlots, int sessionId, int requestId,
                                               List<ItemStack> stacks) implements CustomPacketPayload {
    public static final Type<CreativeInventoryPageDataPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(InfiniteInvo.MODID, "creative_inventory_page_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CreativeInventoryPageDataPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CreativeInventoryPageDataPayload::row,
            ByteBufCodecs.VAR_INT, CreativeInventoryPageDataPayload::unlockedSlots,
            ByteBufCodecs.VAR_INT, CreativeInventoryPageDataPayload::sessionId,
            ByteBufCodecs.VAR_INT, CreativeInventoryPageDataPayload::requestId,
            ItemStack.OPTIONAL_LIST_STREAM_CODEC, CreativeInventoryPageDataPayload::stacks,
            CreativeInventoryPageDataPayload::new);

    @Override
    public Type<CreativeInventoryPageDataPayload> type() {
        return TYPE;
    }

    public static void handle(CreativeInventoryPageDataPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            DebugLog.debug("[Paging][Network][Client] received creative response row={} session={} requestId={} stackCount={}",
                    payload.row(), payload.sessionId(), payload.requestId(), payload.stacks().size());
            CreativeInventoryController.applyPage(
                    payload.row(), payload.unlockedSlots(), payload.sessionId(), payload.requestId(), payload.stacks());
        });
    }
}
