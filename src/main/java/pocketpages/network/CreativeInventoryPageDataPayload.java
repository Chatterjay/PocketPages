package pocketpages.network;

import pocketpages.PocketPages;
import pocketpages.DebugLog;
import pocketpages.client.CreativeInventoryController;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server response used to refresh the vanilla creative inventory slots after a page swap. */
public record CreativeInventoryPageDataPayload(int row, int unlockedSlots, int sessionId, int requestId,
                                               long revision, int stateId, List<ItemStack> stacks)
        implements CustomPacketPayload {
    public static final Type<CreativeInventoryPageDataPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PocketPages.MODID, "creative_inventory_page_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CreativeInventoryPageDataPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeVarInt(payload.row());
                        buffer.writeVarInt(payload.unlockedSlots());
                        buffer.writeVarInt(payload.sessionId());
                        buffer.writeVarInt(payload.requestId());
                        buffer.writeVarLong(payload.revision());
                        buffer.writeVarInt(payload.stateId());
                        ItemStack.OPTIONAL_LIST_STREAM_CODEC.encode(buffer, payload.stacks());
                    },
                    buffer -> new CreativeInventoryPageDataPayload(
                            buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                            buffer.readVarLong(), buffer.readVarInt(),
                            ItemStack.OPTIONAL_LIST_STREAM_CODEC.decode(buffer)));

    @Override
    public Type<CreativeInventoryPageDataPayload> type() {
        return TYPE;
    }

    public static void handle(CreativeInventoryPageDataPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            DebugLog.debug("[Paging][Network][Client] received creative response row={} session={} requestId={} stackCount={}",
                    payload.row(), payload.sessionId(), payload.requestId(), payload.stacks().size());
            CreativeInventoryController.applyPage(
                    payload.row(), payload.unlockedSlots(), payload.sessionId(), payload.requestId(),
                    payload.revision(), payload.stateId(), payload.stacks());
        });
    }
}
