package infiniteinvo.inventory;

import infiniteinvo.Config;
import infiniteinvo.InfiniteInvo;
import infiniteinvo.DebugLog;
import org.anti_ad.mc.ipn.api.IPNSlotsIgnoreForInventoryTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@IPNSlotsIgnoreForInventoryTypes(value = {}, ignoreCraftingSlots = true)
public final class ScrollableInventoryMenu extends RecipeBookMenu<CraftingInput, CraftingRecipe> {
    public static final int COLUMNS = ScrollableInventoryLayout.COLUMNS;
    public static final int VISIBLE_ROWS = ScrollableInventoryLayout.VISIBLE_ROWS;
    public static final int VISIBLE_GRID_SLOTS = COLUMNS * VISIBLE_ROWS;
    private static final int RESULT_SLOT = 0;
    private static final int CRAFTING_START = RESULT_SLOT + 1;
    private static final int ARMOR_START = CRAFTING_START + 4;
    private static final int GRID_START = ARMOR_START + 4;
    private static final int HOTBAR_START = GRID_START + VISIBLE_GRID_SLOTS;

    private final Inventory playerInventory;
    private final ScrollableInventoryStore store;
    private final ResultContainer resultSlots = new ResultContainer();
    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, 2, 2);
    private final ContainerData data;
    private final DataSlot scrollData;
    private final ScrollSlot[] gridSlots = new ScrollSlot[VISIBLE_GRID_SLOTS];
    private int scrollPos;
    private final int maxScroll;
    private int lastScrollRequestId;

    public ScrollableInventoryMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ScrollableInventoryStore.load(playerInventory.player), new SimpleData(playerInventory.player));
    }

    public ScrollableInventoryMenu(int containerId, Inventory playerInventory, ScrollableInventoryStore store, ContainerData data) {
        super(InfiniteInvo.INFINITE_INVENTORY_MENU.get(), containerId);
        this.playerInventory = playerInventory;
        this.store = store;
        this.data = data;
        this.maxScroll = Math.max(0, (int)Math.ceil(store.getContainerSize() / (double)COLUMNS) - VISIBLE_ROWS);
        this.scrollData = new ScrollData(this);

        addResultAndCraftingSlots();
        addArmorSlots();
        addGridSlots();
        addHotbarSlots();
        addOffhandSlot();
        addDataSlots(data);
        addDataSlot(scrollData);
        updateScroll(0);
    }

    public static void open(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            InfiniteInventoryData.dropLegacyPlaceholderItems(serverPlayer);
            serverPlayer.openMenu(new SimpleMenuProvider((id, inv, p) -> new ScrollableInventoryMenu(id, inv),
                    Component.translatable("container.infiniteinvo.inventory")));
        }
    }

    public int getScrollPos() {
        return scrollPos;
    }

    public void updateScroll(int delta) {
        setScrollPosition(scrollPos + delta);
    }

    public void setScrollPosition(int position) {
        int previous = scrollPos;
        scrollPos = Math.max(0, Math.min(maxScroll, position));
        int base = scrollPos * COLUMNS;
        for (int i = 0; i < gridSlots.length; i++) {
            gridSlots[i].setVirtualIndex(base + i);
        }
        DebugLog.debug("[Paging][Server] menu page changed player={} oldPage={} requestedPage={} resolvedPage={} virtualStart={} visibleSlots={}",
                playerInventory.player.getName().getString(), previous, position, scrollPos, base, describeVisibleSlots());
    }

    public boolean acceptScrollRequest(int requestId) {
        if (requestId <= lastScrollRequestId) {
            return false;
        }
        lastScrollRequestId = requestId;
        return true;
    }

    public int getMaxScroll() {
        return maxScroll;
    }

    public int getUnlockedSlots() {
        return data.get(0);
    }

    public int getNextUnlockCost() {
        return data.get(1);
    }

    public int getVisibleColumns() {
        return COLUMNS;
    }

    public int getVisibleRows() {
        return VISIBLE_ROWS;
    }

    public int getVisibleGridSlots() {
        return VISIBLE_GRID_SLOTS;
    }

    public ScrollableInventoryStore getStore() {
        return store;
    }

    public String getPlayerName() {
        return playerInventory.player.getName().getString();
    }

    public String describeVisibleSlots() {
        StringBuilder result = new StringBuilder("[");
        for (int i = 0; i < gridSlots.length; i++) {
            if (i > 0) {
                result.append(", ");
            }
            ScrollSlot slot = gridSlots[i];
            result.append(slot.getContainerSlot()).append('=').append(DebugLog.stack(slot.getItem()));
        }
        return result.append(']').toString();
    }

    /** Snapshot the page content sent with a confirmed page remap. */
    public List<ItemStack> visibleStacks() {
        List<ItemStack> stacks = new ArrayList<>(gridSlots.length);
        for (ScrollSlot slot : gridSlots) {
            stacks.add(slot.getItem().copy());
        }
        return stacks;
    }

    public boolean unlockOne(Player player) {
        if (InfiniteInventoryData.unlockOne(player)) {
            data.set(0, InfiniteInventoryData.getUnlocked(player));
            data.set(1, InfiniteInventoryData.nextUnlockCost(player));
            return true;
        }
        return false;
    }

    public boolean isUnlocked(int slot) {
        return slot < data.get(0);
    }

    private void addResultAndCraftingSlots() {
        addSlot(new ResultSlot(playerInventory.player, craftSlots, resultSlots, 0,
                ScrollableInventoryLayout.RESULT_X, ScrollableInventoryLayout.RESULT_Y));
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                addSlot(new Slot(craftSlots, col + row * 2,
                        ScrollableInventoryLayout.CRAFT_X + col * ScrollableInventoryLayout.SLOT_SIZE,
                        ScrollableInventoryLayout.CRAFT_Y + row * ScrollableInventoryLayout.SLOT_SIZE));
            }
        }
    }

    private void addArmorSlots() {
        addSlot(new ArmorSlot(playerInventory, playerInventory.player, EquipmentSlot.HEAD, 39,
                ScrollableInventoryLayout.ARMOR_X, ScrollableInventoryLayout.ARMOR_Y,
                ResourceLocation.withDefaultNamespace("item/empty_armor_slot_helmet")));
        addSlot(new ArmorSlot(playerInventory, playerInventory.player, EquipmentSlot.CHEST, 38,
                ScrollableInventoryLayout.ARMOR_X, ScrollableInventoryLayout.ARMOR_Y + ScrollableInventoryLayout.SLOT_SIZE,
                ResourceLocation.withDefaultNamespace("item/empty_armor_slot_chestplate")));
        addSlot(new ArmorSlot(playerInventory, playerInventory.player, EquipmentSlot.LEGS, 37,
                ScrollableInventoryLayout.ARMOR_X, ScrollableInventoryLayout.ARMOR_Y + 2 * ScrollableInventoryLayout.SLOT_SIZE,
                ResourceLocation.withDefaultNamespace("item/empty_armor_slot_leggings")));
        addSlot(new ArmorSlot(playerInventory, playerInventory.player, EquipmentSlot.FEET, 36,
                ScrollableInventoryLayout.ARMOR_X, ScrollableInventoryLayout.ARMOR_Y + 3 * ScrollableInventoryLayout.SLOT_SIZE,
                ResourceLocation.withDefaultNamespace("item/empty_armor_slot_boots")));
    }

    private void addGridSlots() {
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int x = ScrollableInventoryLayout.GRID_X + col * ScrollableInventoryLayout.SLOT_SIZE;
                int y = ScrollableInventoryLayout.GRID_Y + row * ScrollableInventoryLayout.SLOT_SIZE;
                ScrollSlot slot = new ScrollSlot(this, store, 0, x, y);
                gridSlots[row * COLUMNS + col] = slot;
                addSlot(slot);
            }
        }
    }

    private void addHotbarSlots() {
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col,
                    ScrollableInventoryLayout.GRID_X + col * ScrollableInventoryLayout.SLOT_SIZE,
                    ScrollableInventoryLayout.HOTBAR_Y));
        }
    }

    private void addOffhandSlot() {
        addSlot(new OffhandSlot(playerInventory, playerInventory.player,
                ScrollableInventoryLayout.OFFHAND_X, ScrollableInventoryLayout.OFFHAND_Y));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int gridEnd = GRID_START + VISIBLE_GRID_SLOTS;
        int hotbarEnd = HOTBAR_START + 9;

        if (index == RESULT_SLOT) {
            if (!moveItemStackTo(stack, GRID_START, hotbarEnd, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, copy);
        } else if (index >= CRAFTING_START && index < GRID_START) {
            if (!moveItemStackTo(stack, GRID_START, hotbarEnd, false)) return ItemStack.EMPTY;
        } else if (index >= GRID_START && index < gridEnd) {
            if (!moveItemStackTo(stack, HOTBAR_START, hotbarEnd, false)) return ItemStack.EMPTY;
        } else if (index >= HOTBAR_START && index < hotbarEnd) {
            if (!moveItemStackTo(stack, GRID_START, gridEnd, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, GRID_START, hotbarEnd, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY, copy);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void fillCraftSlotsStackedContents(StackedContents stackedContents) {
        craftSlots.fillStackedContents(stackedContents);
    }

    @Override
    public void clearCraftingContent() {
        resultSlots.clearContent();
        craftSlots.clearContent();
    }

    @Override
    public boolean recipeMatches(RecipeHolder<CraftingRecipe> recipe) {
        return recipe.value().matches(craftSlots.asCraftInput(), playerInventory.player.level());
    }

    @Override
    public int getResultSlotIndex() {
        return RESULT_SLOT;
    }

    @Override
    public int getGridWidth() {
        return craftSlots.getWidth();
    }

    @Override
    public int getGridHeight() {
        return craftSlots.getHeight();
    }

    @Override
    public int getSize() {
        return CRAFTING_START + 4;
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    @Override
    public boolean shouldMoveToInventory(int slotIndex) {
        return slotIndex != RESULT_SLOT;
    }

    @Override
    public void slotsChanged(Container container) {
        if (container == craftSlots && !playerInventory.player.level().isClientSide) {
            updateCraftingResult((ServerPlayer) playerInventory.player);
        }
    }

    private void updateCraftingResult(ServerPlayer player) {
        CraftingInput input = craftSlots.asCraftInput();
        ItemStack result = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> recipe = player.level().getServer().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, player.level());
        if (recipe.isPresent() && resultSlots.setRecipeUsed(player.level(), player, recipe.get())) {
            ItemStack crafted = recipe.get().value().assemble(input, player.level().registryAccess());
            if (crafted.isItemEnabled(player.level().enabledFeatures())) {
                result = crafted;
            }
        }
        resultSlots.setItem(0, result);
        setRemoteSlot(RESULT_SLOT, result);
        player.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), RESULT_SLOT, result));
    }

    @Override
    public void removed(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            returnOrDrop(serverPlayer, getCarried());
            setCarried(ItemStack.EMPTY);
            resultSlots.clearContent();
            returnCraftingItems(serverPlayer);
            InfiniteInventoryData.dropLockedItems(serverPlayer);
        } else {
            resultSlots.clearContent();
        }
    }

    private void returnCraftingItems(ServerPlayer player) {
        for (int slot = 0; slot < craftSlots.getContainerSize(); slot++) {
            returnOrDrop(player, craftSlots.removeItemNoUpdate(slot));
        }
    }

    /** Mirrors vanilla close behavior while allowing Inventory.add to use overflow slots. */
    private static void returnOrDrop(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (player.isAlive() && !player.hasDisconnected()) {
            player.getInventory().add(stack);
        }
        if (!stack.isEmpty()) {
            player.drop(stack, false);
        }
    }

    public void syncFromPlayer(Player player) {
        store.syncFromPlayer(player);
        data.set(0, InfiniteInventoryData.getUnlocked(player));
        data.set(1, InfiniteInventoryData.nextUnlockCost(player));
        updateScroll(0);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            if (!InfiniteInventoryData.canAffordNextUnlock(player)) {
                return false;
            }
            int alreadyUnlocked = InfiniteInventoryData.getUnlocked(player);
            if (unlockOne(player)) {
                InfiniteInventoryData.chargeForUnlock(player, alreadyUnlocked);
                return true;
            }
            return false;
        }
        return super.clickMenuButton(player, id);
    }

    private static final class SimpleData implements ContainerData {
        private final Player player;
        private int unlockedSlots;
        private int nextUnlockCost;

        private SimpleData(Player player) {
            this.player = player;
            this.unlockedSlots = InfiniteInventoryData.getUnlocked(player);
            this.nextUnlockCost = InfiniteInventoryData.nextUnlockCost(player);
        }

        @Override
        public int get(int index) {
            if (!player.level().isClientSide) {
                unlockedSlots = InfiniteInventoryData.getUnlocked(player);
                nextUnlockCost = InfiniteInventoryData.nextUnlockCost(player);
            }
            return switch (index) {
                case 0 -> unlockedSlots;
                case 1 -> nextUnlockCost;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                unlockedSlots = value;
            } else if (index == 1) {
                nextUnlockCost = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    private static final class ScrollData extends DataSlot {
        private final ScrollableInventoryMenu menu;

        private ScrollData(ScrollableInventoryMenu menu) {
            this.menu = menu;
        }

        @Override
        public int get() {
            return menu.scrollPos;
        }

        @Override
        public void set(int value) {
            menu.setScrollPosition(value);
        }
    }
}
