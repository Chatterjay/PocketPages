package infiniteinvo;

import com.mojang.brigadier.Command;
import infiniteinvo.inventory.InfiniteInventoryData;
import infiniteinvo.inventory.InfiniteInventoryState;
import infiniteinvo.inventory.CreativeInventoryPaging;
import infiniteinvo.inventory.ExtendedInventory;
import infiniteinvo.inventory.ScrollableInventoryMenu;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

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
                    int cleared = 0;
                    for (int slot = 0; slot < InfiniteInventoryData.getUnlocked(player); slot++) {
                        if (!player.getInventory().items.get(slot + 9).isEmpty()) {
                            cleared++;
                        }
                    }
                    CreativeInventoryPaging.clearAll(player);
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
        if (!event.isWasDeath()) {
            return;
        }

        if (!keepInventory) {
            InfiniteInventoryState replacementState = new InfiniteInventoryState();
            if (Config.KEEP_UNLOCKS_ON_DEATH.get()) {
                replacementState.setUnlockedSlots(previous.getUnlockedSlots());
            }
            InfiniteInventoryData.replaceState(replacement, replacementState);
        }

        // Vanilla only knows about its original slots during the clone. Restore
        // the expanded physical list from SavedData after the replacement exists.
        ExtendedInventory.initialize(replacement);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ExtendedInventory.initialize(player);
            InfiniteInventoryData.dropLegacyPlaceholderItems(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CreativeInventoryPaging.restoreVanillaPage(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.tickCount % Config.LOCKED_ITEM_DROP_CHECK_INTERVAL_TICKS.get() != 0) {
            return;
        }

        InfiniteInventoryData.dropLockedItems(player);
    }

}
