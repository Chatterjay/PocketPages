package infiniteinvo.integration;

import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosApi;

/** Adds Curios' validated equipment handler to InfiniteInvo's player view. */
public final class CuriosCompat {
    private CuriosCompat() {
    }

    public static void append(Player player, List<IItemHandlerModifiable> handlers) {
        CuriosApi.getCuriosInventory(player).flatMap(curios -> {
            IItemHandlerModifiable equipped = curios.getEquippedCurios();
            return equipped.getSlots() > 0 ? java.util.Optional.of(equipped) : java.util.Optional.empty();
        }).ifPresent(handlers::add);
    }
}
