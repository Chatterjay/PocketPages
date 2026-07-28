package infiniteinvo.network;

import infiniteinvo.InfiniteInvo;
import infiniteinvo.inventory.CreativeInventoryPaging;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Requests a three-row extended inventory page for the vanilla creative inventory tab. */
public record CreativeInventoryPageRequestPayload(int row) implements CustomPacketPayload {
    public static final Type<CreativeInventoryPageRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(InfiniteInvo.MODID, "creative_inventory_page_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CreativeInventoryPageRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CreativeInventoryPageRequestPayload::row,
            CreativeInventoryPageRequestPayload::new);

    @Override
    public Type<CreativeInventoryPageRequestPayload> type() {
        return TYPE;
    }

    public static void handle(CreativeInventoryPageRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                CreativeInventoryPaging.selectRow(player, payload.row());
            }
        });
    }
}
