package infiniteinvo.client;

import infiniteinvo.InfiniteInvo;
import net.minecraft.client.gui.screens.MenuScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = InfiniteInvo.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class InfiniteInvoClient {
    private InfiniteInvoClient() {
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(InfiniteInvo.INFINITE_INVENTORY_MENU.get(), ScrollableInventoryScreen::new);
    }
}
