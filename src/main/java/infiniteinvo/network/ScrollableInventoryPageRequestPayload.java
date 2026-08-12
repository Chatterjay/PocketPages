package infiniteinvo.network;

import infiniteinvo.InfiniteInvo;
import infiniteinvo.DebugLog;
import infiniteinvo.inventory.ScrollableInventoryMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;

/** Requests an absolute page for the open scrollable inventory. */
public record ScrollableInventoryPageRequestPayload(int containerId, int page, int requestId) implements CustomPacketPayload {
    public static final Type<ScrollableInventoryPageRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(InfiniteInvo.MODID, "scrollable_inventory_page"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ScrollableInventoryPageRequestPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ScrollableInventoryPageRequestPayload::containerId,
                    ByteBufCodecs.VAR_INT, ScrollableInventoryPageRequestPayload::page,
                    ByteBufCodecs.VAR_INT, ScrollableInventoryPageRequestPayload::requestId,
                    ScrollableInventoryPageRequestPayload::new);

    @Override
    public Type<ScrollableInventoryPageRequestPayload> type() {
        return TYPE;
    }

    public static void handle(ScrollableInventoryPageRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            DebugLog.debug("[Paging][Network][Server] received scroll request player={} containerId={} page={} requestId={} currentMenu={}",
                    player.getName().getString(), payload.containerId(), payload.page(), payload.requestId(),
                    player.containerMenu.containerId);
            if (!(player.containerMenu instanceof ScrollableInventoryMenu menu)
                    || menu.containerId != payload.containerId()) {
                DebugLog.debug("[Paging][Network][Server] scroll request rejected: menu mismatch player={}",
                        player.getName().getString());
                return;
            }
            if (menu.acceptScrollRequest(payload.requestId())) {
                DebugLog.debug("[Paging][Server] scroll request accepted player={} oldPage={} newPage={} requestId={}",
                        player.getName().getString(), menu.getScrollPos(), payload.page(), payload.requestId());
                menu.setScrollPosition(payload.page());
                // The client must remap ScrollSlot indices before these native
                // menu packets reach it, otherwise a new page can populate the
                // slots still pointing at the previous page.
                PacketDistributor.sendToPlayer(player, new ScrollableInventoryPageDataPayload(
                        menu.containerId, menu.getScrollPos(), payload.requestId(), menu.visibleStacks()));
                // A page remap changes what every visible Slot refers to even
                // when the stacks compare equal (for example, two pages of
                // stone x64). Delta synchronization would omit those values
                // and leave the client showing its old backing slots until a
                // click happens to force another update.
                player.containerMenu.broadcastFullState();
                DebugLog.debug("[Paging][Server] scroll complete player={} page={} visibleSlots={}",
                        player.getName().getString(), menu.getScrollPos(), menu.describeVisibleSlots());
            } else {
                DebugLog.debug("[Paging][Server] scroll request rejected: old request player={} requestId={}",
                        player.getName().getString(), payload.requestId());
            }
        });
    }
}
