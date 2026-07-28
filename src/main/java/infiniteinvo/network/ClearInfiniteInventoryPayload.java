package infiniteinvo.network;

import infiniteinvo.InfiniteInvo;
import infiniteinvo.inventory.CreativeInventoryPaging;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Clears only InfiniteInvo's extended state after the native creative destroy action. */
public record ClearInfiniteInventoryPayload() implements CustomPacketPayload {
    public static final ClearInfiniteInventoryPayload INSTANCE = new ClearInfiniteInventoryPayload();
    public static final Type<ClearInfiniteInventoryPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(InfiniteInvo.MODID, "clear_infinite_inventory"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClearInfiniteInventoryPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<ClearInfiniteInventoryPayload> type() {
        return TYPE;
    }

    public static void handle(ClearInfiniteInventoryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.getAbilities().instabuild) {
                CreativeInventoryPaging.clearAll(player);
            }
        });
    }
}
