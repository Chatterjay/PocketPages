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

/** Restores the real first player-inventory page after leaving the creative inventory tab. */
public record CloseCreativeInventoryPagingPayload(int sessionId) implements CustomPacketPayload {
    public static final Type<CloseCreativeInventoryPagingPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PocketPages.MODID, "close_creative_inventory_paging"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CloseCreativeInventoryPagingPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CloseCreativeInventoryPagingPayload::sessionId,
                    CloseCreativeInventoryPagingPayload::new);

    @Override
    public Type<CloseCreativeInventoryPagingPayload> type() {
        return TYPE;
    }

    public static void handle(CloseCreativeInventoryPagingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                DebugLog.debug("[Paging][Network][Server] received close player={} session={}",
                        player.getName().getString(), payload.sessionId());
                CreativeInventoryPaging.restoreVanillaPage(player, payload.sessionId());
            }
        });
    }
}
