package pocketpages.client;

import pocketpages.DebugLog;
import pocketpages.api.PocketPagesSortingArea;
import pocketpages.api.PocketPagesSortingApi;
import pocketpages.api.PocketPagesSortingSlot;
import pocketpages.api.PocketPagesSortingView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.anti_ad.mc.ipnext.inventory.AreaType;
import org.anti_ad.mc.ipnext.inventory.AreaTypes;
import org.anti_ad.mc.ipnext.inventory.ItemArea;
import org.anti_ad.mc.ipnext.ingame.InventoryKt;

/** IPN area adapters kept outside the mixin package for transformed target classes. */
public final class IpnAreaTypeCompat {
    private static final ThreadLocal<SlotLists> FILL_SLOTS = new ThreadLocal<>();

    private IpnAreaTypeCompat() {
    }

    public static void rememberFillSlotsArguments(List<Slot> slots, List<Integer> slotIndices) {
        FILL_SLOTS.set(new SlotLists(slots, slotIndices));
    }

    public static void finishFillSlots() {
        SlotLists lists = FILL_SLOTS.get();
        FILL_SLOTS.remove();
        if (lists != null) {
            excludeVirtualPlayerSlots(lists.slots(), lists.slotIndices());
        }
    }

    public static void excludeVirtualPlayerSlots(List<Slot> slots, List<Integer> slotIndices) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        AbstractContainerMenu menu = minecraft.player.containerMenu;
        if (!isSameSlotList(menu, slots)) {
            return;
        }

        PocketPagesSortingApi.findView(minecraft.player, menu).ifPresent(view -> {
            Set<Integer> virtualSlots = new LinkedHashSet<>();
            view.visibleStorageSlots().forEach(slot -> virtualSlots.add(slot.menuSlot()));
            int previousSize = slotIndices.size();
            slotIndices.removeIf(virtualSlots::contains);
            DebugLog.debug("[IPN] container area menu={} slots={} excludedVirtual={}",
                    menu.getClass().getSimpleName(), slotIndices.size(),
                    previousSize - slotIndices.size());
        });
    }

    private record SlotLists(List<Slot> slots, List<Integer> slotIndices) {
    }

    public static AreaType wrapPlayerStorage(AreaType original, AreaTypes areaTypes) {
        return new PocketPagesPlayerStorageArea(original, areaTypes);
    }

    public static AreaType wrapLockedSlots(AreaType original) {
        return new PocketPagesLockedSlotsArea(original);
    }

    private static boolean isSameSlotList(AbstractContainerMenu menu, List<Slot> slots) {
        if (menu == null || menu.slots.size() != slots.size()) {
            return false;
        }
        for (int index = 0; index < slots.size(); index++) {
            if (menu.slots.get(index) != slots.get(index)) {
                return false;
            }
        }
        return true;
    }

    private record PocketPagesPlayerStorageArea(AreaType original, AreaTypes areaTypes) implements AreaType {
        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public ItemArea getItemArea(AbstractContainerMenu menu, List slots) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                return original.getItemArea(menu, slots);
            }

            var viewResult = PocketPagesSortingApi.findView(minecraft.player, menu);
            if (viewResult.isEmpty()) {
                return original.getItemArea(menu, slots);
            }
            PocketPagesSortingView view = viewResult.get();

            // IPN's ordinary player-storage definition is fixed to slots 9..35.
            // Its sort-at-cursor path asks for that definition even when the
            // focused menu slot is in the hotbar, so select the unified region
            // before IPN builds its tracker.
            if (isSortAtCursorEnabled()) {
                var focusedArea = view.areaForSlot(InventoryKt.vFocusedSlot());
                if (focusedArea.isPresent()) {
                    DebugLog.debug("[IPN] focused area menu={} area={}",
                            menu.getClass().getSimpleName(), focusedArea.get());
                    if (focusedArea.get() == PocketPagesSortingArea.PLAYER_HOTBAR) {
                        return areaTypes.getPlayerHotbar().getItemArea(menu, slots);
                    }
                    if (focusedArea.get() == PocketPagesSortingArea.PLAYER_ARMOR
                            || focusedArea.get() == PocketPagesSortingArea.PLAYER_OFFHAND) {
                        return visibleRegionArea(view, focusedArea.get(), slots);
                    }
                }
            }

            ItemArea originalArea = original.getItemArea(menu, slots);

            LinkedHashSet<Integer> combined = new LinkedHashSet<>(originalArea.getSlotIndices());
            int virtualSlots = 0;
            boolean replacedVisibleStorage = false;
            for (var slot : view.visibleStorageSlots()) {
                if (slot.menuSlot() < 0 || slot.menuSlot() >= slots.size()) {
                    continue;
                }

                // The original IPN player region contains the same physical
                // menu slots. Replace it wholesale so a stable virtual lock
                // cannot be bypassed through that original region.
                replacedVisibleStorage |= combined.remove(slot.menuSlot());
                if (!slot.isSortable() || IpnCompat.isVirtualOrMappedSlotLocked(slot.storageSlot())) {
                    continue;
                }
                if (combined.add(slot.menuSlot())) {
                    virtualSlots++;
                }
            }
            if (virtualSlots == 0 && !replacedVisibleStorage) {
                return originalArea;
            }

            List<Integer> slotIndices = new ArrayList<>(combined);
            DebugLog.debug("[IPN] player area menu={} native={} virtual={} combined={}",
                    menu.getClass().getSimpleName(), originalArea.getSlotIndices().size(),
                    virtualSlots, slotIndices.size());
            return ItemArea.Companion.invoke(slots, slotIndices, false);
        }

        @SuppressWarnings("rawtypes")
        private static ItemArea visibleRegionArea(PocketPagesSortingView view, PocketPagesSortingArea area,
                                                  List slots) {
            List<Integer> slotIndices = view.slots(area).stream()
                    .filter(PocketPagesSortingSlot::isSortable)
                    .map(PocketPagesSortingSlot::menuSlot)
                    .filter(index -> index >= 0 && index < slots.size())
                    .toList();
            return ItemArea.Companion.invoke(slots, slotIndices, false);
        }

        @Override
        public AreaType plus(AreaType other) {
            return new CombinedArea(this, other, false);
        }

        @Override
        public AreaType minus(AreaType other) {
            return new CombinedArea(this, other, true);
        }
    }

    private record PocketPagesLockedSlotsArea(AreaType original) implements AreaType {
        @Override
        @SuppressWarnings("rawtypes")
        public ItemArea getItemArea(AbstractContainerMenu menu, List slots) {
            Minecraft minecraft = Minecraft.getInstance();
            ItemArea originalArea = original.getItemArea(menu, slots);
            if (minecraft.player == null) {
                return originalArea;
            }

            var viewResult = PocketPagesSortingApi.findView(minecraft.player, menu);
            if (viewResult.isEmpty()) {
                return originalArea;
            }

            LinkedHashSet<Integer> combined = new LinkedHashSet<>(originalArea.getSlotIndices());
            int virtualLocks = 0;
            for (var slot : viewResult.get().visibleStorageSlots()) {
                if (slot.menuSlot() < 0 || slot.menuSlot() >= slots.size()
                        || !IpnCompat.isVirtualOrMappedSlotLocked(slot.storageSlot())) {
                    continue;
                }
                if (combined.add(slot.menuSlot())) {
                    virtualLocks++;
                }
            }
            if (virtualLocks == 0) {
                return originalArea;
            }

            DebugLog.debug("[IPN] locked area menu={} native={} virtual={} combined={}",
                    menu.getClass().getSimpleName(), originalArea.getSlotIndices().size(),
                    virtualLocks, combined.size());
            return ItemArea.Companion.invoke(slots, new ArrayList<>(combined), false);
        }

        @Override
        public AreaType plus(AreaType other) {
            return new CombinedArea(this, other, false);
        }

        @Override
        public AreaType minus(AreaType other) {
            return new CombinedArea(this, other, true);
        }
    }

    private static boolean isSortAtCursorEnabled() {
        try {
            Class<?> settingsClass = Class.forName("org.anti_ad.mc.ipnext.config.SortSettings");
            Field instanceField = settingsClass.getField("INSTANCE");
            Object settings = instanceField.get(null);
            Method settingMethod = settingsClass.getMethod("getSORT_AT_CURSOR");
            Object setting = settingMethod.invoke(settings);
            Method valueMethod = setting.getClass().getMethod("getBooleanValue");
            return (Boolean) valueMethod.invoke(setting);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private record CombinedArea(AreaType left, AreaType right, boolean subtract) implements AreaType {
        @Override
        @SuppressWarnings("rawtypes")
        public ItemArea getItemArea(AbstractContainerMenu menu, List slots) {
            ItemArea leftArea = left.getItemArea(menu, slots);
            ItemArea rightArea = right.getItemArea(menu, slots);
            return subtract ? leftArea.minus(rightArea) : leftArea.plus(rightArea);
        }

        @Override
        public AreaType plus(AreaType other) {
            return new CombinedArea(this, other, false);
        }

        @Override
        public AreaType minus(AreaType other) {
            return new CombinedArea(this, other, true);
        }
    }
}
