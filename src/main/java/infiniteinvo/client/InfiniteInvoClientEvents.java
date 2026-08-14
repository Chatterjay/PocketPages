package infiniteinvo.client;

import infiniteinvo.InfiniteInvo;
import infiniteinvo.inventory.InfiniteInventoryData;
import infiniteinvo.network.OpenInfiniteInventoryPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = InfiniteInvo.MODID, value = Dist.CLIENT)
public final class InfiniteInvoClientEvents {
    private static boolean openInfiniteInventoryNextTick;

    private InfiniteInvoClientEvents() {
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof InventoryScreen
                && Minecraft.getInstance().player != null
                && !Minecraft.getInstance().player.getAbilities().instabuild) {
            openInfiniteInventoryNextTick = true;
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onContainerForegroundRender(ContainerScreenEvent.Render.Foreground event) {
        CreativeInventoryController.render(event);
        ContainerInventoryPagingController.render(event);
    }

    @SubscribeEvent
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        CreativeInventoryController.mouseScrolled(event);
        ContainerInventoryPagingController.mouseScrolled(event);
    }

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        CreativeInventoryController.mousePressed(event);
        ContainerInventoryPagingController.mousePressed(event);
    }

    @SubscribeEvent
    public static void onMousePressedPost(ScreenEvent.MouseButtonPressed.Post event) {
        CreativeInventoryController.mousePressedPost(event);
    }

    @SubscribeEvent
    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        CreativeInventoryController.mouseDragged(event);
        ContainerInventoryPagingController.mouseDragged(event);
    }

    @SubscribeEvent
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        CreativeInventoryController.mouseReleased(event);
        ContainerInventoryPagingController.mouseReleased(event);
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        CreativeInventoryController.closing(event);
        ContainerInventoryPagingController.closing(event);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (openInfiniteInventoryNextTick) {
            openInfiniteInventoryNextTick = false;
            if (Minecraft.getInstance().player != null
                    && !Minecraft.getInstance().player.getAbilities().instabuild
                    && !(Minecraft.getInstance().screen instanceof ScrollableInventoryScreen)) {
                PacketDistributor.sendToServer(OpenInfiniteInventoryPayload.INSTANCE);
            }
        }
        CreativeInventoryController.tick();
        ContainerInventoryPagingController.tick();
    }

    @SubscribeEvent
    public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        openInfiniteInventoryNextTick = false;
        InfiniteInventoryData.clearClientCache();
    }
}
