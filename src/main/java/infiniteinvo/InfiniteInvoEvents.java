package infiniteinvo;

import com.mojang.brigadier.Command;
import infiniteinvo.inventory.InfiniteInventoryData;
import infiniteinvo.inventory.InfiniteInventoryState;
import infiniteinvo.inventory.CreativeInventoryPaging;
import infiniteinvo.inventory.ScrollableInventoryMenu;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class InfiniteInvoEvents {
    private InfiniteInvoEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("infiniteinvo")
                .then(Commands.literal("open").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ScrollableInventoryMenu.open(player);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("clear").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    InfiniteInventoryState state = InfiniteInventoryData.state(player);
                    int cleared = 0;
                    for (int slot = 0; slot < state.size(); slot++) {
                        if (!state.getItem(slot).isEmpty()) {
                            state.setItem(slot, ItemStack.EMPTY);
                            cleared++;
                        }
                    }
                    for (int slot = 9; slot < 36; slot++) {
                        player.getInventory().setItem(slot, ItemStack.EMPTY);
                    }
                    InfiniteInventoryData.markDirty(player);
                    player.containerMenu.broadcastChanges();
                    return cleared;
                })));
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer original) || !(event.getEntity() instanceof ServerPlayer replacement)) {
            return;
        }

        InfiniteInventoryState previous = InfiniteInventoryData.state(original);
        boolean keepInventory = original.serverLevel().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY);
        if (!event.isWasDeath() || keepInventory) {
            return;
        }

        dropExtraSlots(original, previous);
        InfiniteInventoryState replacementState = new InfiniteInventoryState();
        if (Config.KEEP_UNLOCKS_ON_DEATH.get()) {
            replacementState.setUnlockedSlots(previous.getUnlockedSlots());
        }
        InfiniteInventoryData.replaceState(replacement, replacementState);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CreativeInventoryPaging.restoreVanillaPage(player);
        }
    }

    private static void dropExtraSlots(ServerPlayer player, InfiniteInventoryState state) {
        ServerLevel level = player.serverLevel();
        for (int slot = 27; slot < state.size(); slot++) {
            ItemStack stack = state.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ItemEntity item = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), stack.copy());
            item.setDefaultPickUpDelay();
            level.addFreshEntity(item);
        }
    }

    @SubscribeEvent
    public static void onItemPickupPre(ItemEntityPickupEvent.Pre event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || hasVanillaInventorySpace(player.getInventory(), event.getItemEntity().getItem())) {
            return;
        }

        moveToOverflow(player, event.getItemEntity(), event);
    }

    @SubscribeEvent
    public static void onItemPickupPost(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer() instanceof ServerPlayer player && !event.getCurrentStack().isEmpty()) {
            moveToOverflow(player, event.getItemEntity(), null);
        }
    }

    private static boolean hasVanillaInventorySpace(Inventory inventory, ItemStack incoming) {
        for (ItemStack existing : inventory.items) {
            if (existing.isEmpty()) {
                return true;
            }
            if (ItemStack.isSameItemSameComponents(existing, incoming) && existing.getCount() < existing.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private static void moveToOverflow(ServerPlayer player, ItemEntity itemEntity, ItemEntityPickupEvent.Pre preEvent) {
        ItemStack stack = itemEntity.getItem();
        int inserted = InfiniteInventoryData.insertOverflow(player, stack);
        if (inserted <= 0) {
            return;
        }

        player.take(itemEntity, inserted);
        if (stack.isEmpty()) {
            itemEntity.discard();
        }
        if (preEvent != null) {
            preEvent.setCanPickup(TriState.FALSE);
        }
    }

}
