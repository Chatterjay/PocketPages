package pocketpages.network;

import pocketpages.PocketPages;
import pocketpages.inventory.CreativeInventoryPaging;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Clears only PocketPages's extended state after the native creative destroy action. */
public record ClearPocketPagesInventoryPayload() implements CustomPacketPayload {
    public static final ClearPocketPagesInventoryPayload INSTANCE = new ClearPocketPagesInventoryPayload();
    public static final Type<ClearPocketPagesInventoryPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PocketPages.MODID, "clear_pocket_pages"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClearPocketPagesInventoryPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<ClearPocketPagesInventoryPayload> type() {
        return TYPE;
    }

    public static void handle(ClearPocketPagesInventoryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.getAbilities().instabuild) {
                CreativeInventoryPaging.clearAll(player);
            }
        });
    }
}
