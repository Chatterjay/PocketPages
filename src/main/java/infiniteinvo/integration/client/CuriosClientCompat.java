package infiniteinvo.integration.client;

import infiniteinvo.client.ScrollableInventoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.client.CuriosClientConfig;
import top.theillusivec4.curios.client.gui.CuriosButton;
import top.theillusivec4.curios.client.gui.CuriosScreen;
import top.theillusivec4.curios.common.network.client.CPacketOpenCurios;

/** Client-only bridge for Curios' inventory entry point. */
public final class CuriosClientCompat {
    private static final Component OPEN_CURIOS = Component.translatable("key.curios.open.desc");
    private static final int BUTTON_SIZE = 10;
    private static final int INVENTORY_BUTTON_Y = 85;

    private CuriosClientCompat() {
    }

    public static boolean isButtonEnabled() {
        return CuriosClientConfig.CLIENT.enableButton.get();
    }

    public static ImageButton createButton(ScrollableInventoryScreen screen) {
        ImageButton button = new ImageButton(0, 0, BUTTON_SIZE, BUTTON_SIZE, CuriosButton.BIG,
                ignored -> openInventory(screen));
        button.setTooltip(Tooltip.create(OPEN_CURIOS));
        updatePosition(screen, button);
        return button;
    }

    public static void updatePosition(ScrollableInventoryScreen screen, ImageButton button) {
        Tuple<Integer, Integer> offset = CuriosScreen.getButtonOffset(false);
        button.setPosition(screen.getGuiLeft() + offset.getA() + 2,
                screen.getGuiTop() + offset.getB() + INVENTORY_BUTTON_Y);
    }

    private static void openInventory(ScrollableInventoryScreen screen) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (screen.getRecipeBookComponent().isVisible()) {
            screen.getRecipeBookComponent().toggleVisibility();
        }
        ItemStack carried = minecraft.player.containerMenu.getCarried();
        minecraft.player.containerMenu.setCarried(ItemStack.EMPTY);
        PacketDistributor.sendToServer(new CPacketOpenCurios(carried));
    }
}
