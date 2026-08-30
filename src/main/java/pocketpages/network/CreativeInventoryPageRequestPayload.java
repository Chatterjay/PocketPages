package pocketpages.network;

import pocketpages.PocketPages;
import pocketpages.DebugLog;
import pocketpages.inventory.CreativeInventoryPaging;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Requests a three-row extended inventory page for the vanilla creative inventory tab. */
public record CreativeInventoryPageRequestPayload(int row, int sessionId, int requestId, long knownRevision) implements CustomPacketPayload {
    public static final Type<CreativeInventoryPageRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PocketPages.MODID, "creative_inventory_page_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CreativeInventoryPageRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CreativeInventoryPageRequestPayload::row,
            ByteBufCodecs.VAR_INT, CreativeInventoryPageRequestPayload::sessionId,
            ByteBufCodecs.VAR_INT, CreativeInventoryPageRequestPayload::requestId,
            ByteBufCodecs.VAR_LONG, CreativeInventoryPageRequestPayload::knownRevision,
            CreativeInventoryPageRequestPayload::new);

    @Override
    public Type<CreativeInventoryPageRequestPayload> type() {
        return TYPE;
    }

    public static void handle(CreativeInventoryPageRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                DebugLog.debug("[Paging][Network][Server] received creative request player={} row={} session={} requestId={} knownRevision={}",
                        player.getName().getString(), payload.row(), payload.sessionId(), payload.requestId(),
                        payload.knownRevision());
                CreativeInventoryPaging.selectRow(player, payload.row(), payload.sessionId(), payload.requestId(),
                        payload.knownRevision());
            }
        });
    }
}
