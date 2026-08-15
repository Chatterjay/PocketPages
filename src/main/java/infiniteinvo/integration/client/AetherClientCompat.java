package infiniteinvo.integration.client;

import com.aetherteam.aether.client.event.hooks.GuiHooks;
import com.aetherteam.aether.client.gui.component.inventory.AccessoryButton;
import com.aetherteam.aether.client.gui.screen.inventory.AetherAccessoriesScreen;
import net.minecraft.util.Tuple;

/** Opens The Aether's dedicated accessories inventory from InfiniteInvo. */
public final class AetherClientCompat {
    private AetherClientCompat() {
    }

    public static boolean isButtonEnabled() {
        return GuiHooks.isAccessoryButtonEnabled();
    }

    public static AccessoryButton createButton(VanillaInventoryScreenCompat screen) {
        Tuple<Integer, Integer> offset = AetherAccessoriesScreen.getButtonOffset(screen);
        return GuiHooks.setupAccessoryButton(screen, offset);
    }
}
