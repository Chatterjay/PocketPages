package infiniteinvo.client;

import infiniteinvo.InfiniteInvo;
import net.minecraft.client.gui.screens.MenuScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = InfiniteInvo.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = InfiniteInvo.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class InfiniteInvoClient {
    public InfiniteInvoClient(ModContainer modContainer) {
        IConfigScreenFactory configScreenFactory = (container, parent) -> new ConfigurationScreen(container, parent);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, configScreenFactory);
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(InfiniteInvo.INFINITE_INVENTORY_MENU.get(), ScrollableInventoryScreen::new);
    }
}
