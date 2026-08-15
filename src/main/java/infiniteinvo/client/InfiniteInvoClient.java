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
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        net.minecraft.world.inventory.MenuType<? extends net.minecraft.world.inventory.InventoryMenu> menuType =
                (net.minecraft.world.inventory.MenuType) InfiniteInvo.INFINITE_INVENTORY_MENU.get();
        MenuScreens.ScreenConstructor<net.minecraft.world.inventory.InventoryMenu, ScrollableInventoryScreen> screen =
                (menu, inventory, title) -> new ScrollableInventoryScreen(
                        (infiniteinvo.inventory.ScrollableInventoryMenu) menu, inventory, title);
        event.register(menuType, screen);
    }
}
