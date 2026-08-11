package infiniteinvo;

import com.mojang.logging.LogUtils;
import infiniteinvo.api.InfiniteInvoCapabilities;
import infiniteinvo.inventory.ScrollableInventoryMenu;
import infiniteinvo.inventory.InfiniteInventoryState;
import infiniteinvo.inventory.VirtualInventoryItemHandler;
import infiniteinvo.item.LockedSlotItem;
import infiniteinvo.item.UnlockSlotItem;
import infiniteinvo.network.OpenInfiniteInventoryPayload;
import infiniteinvo.network.CloseCreativeInventoryPagingPayload;
import infiniteinvo.network.CreativeInventoryPageDataPayload;
import infiniteinvo.network.CreativeInventoryPageRequestPayload;
import infiniteinvo.network.ClearInfiniteInventoryPayload;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.wrapper.PlayerInvWrapper;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.function.Supplier;
import org.slf4j.Logger;

@Mod(InfiniteInvo.MODID)
public final class InfiniteInvo {
    public static final String MODID = "infiniteinvo";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    // Registered solely to migrate data written by development builds before SavedData was introduced.
    public static final DeferredRegister<AttachmentType<?>> LEGACY_ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);
    public static final Supplier<AttachmentType<InfiniteInventoryState>> LEGACY_INVENTORY_STATE = LEGACY_ATTACHMENTS.register(
            "inventory_state", () -> AttachmentType.serializable(InfiniteInventoryState::new).build());

    public static final DeferredItem<Item> LOCKED_SLOT = ITEMS.register("locked_slot", () -> new LockedSlotItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> UNLOCK_SLOT = ITEMS.register("unlock_slot", () -> new UnlockSlotItem(new Item.Properties().stacksTo(64)));

    public static final DeferredHolder<MenuType<?>, MenuType<ScrollableInventoryMenu>> INFINITE_INVENTORY_MENU = MENUS.register(
            "infinite_inventory",
            () -> new MenuType<>(ScrollableInventoryMenu::new, FeatureFlags.DEFAULT_FLAGS)
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.infiniteinvo"))
            .withTabsBefore(CreativeModeTabs.TOOLS_AND_UTILITIES)
            .icon(() -> UNLOCK_SLOT.get().getDefaultInstance())
            .displayItems((parameters, output) -> output.accept(UNLOCK_SLOT.get()))
            .build());

    public InfiniteInvo(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);
        MENUS.register(modEventBus);
        TABS.register(modEventBus);
        LEGACY_ATTACHMENTS.register(modEventBus);

        modEventBus.addListener(this::addCreativeTabItems);
        modEventBus.addListener(this::addBuiltInResourcePacks);
        modEventBus.addListener(this::registerPayloadHandlers);
        modEventBus.addListener(this::registerCapabilities);
        NeoForge.EVENT_BUS.register(InfiniteInvoEvents.class);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerEntity(InfiniteInvoCapabilities.VIRTUAL_INVENTORY, EntityType.PLAYER,
                (player, context) -> new VirtualInventoryItemHandler(player.getInventory()));
        event.registerEntity(Capabilities.ItemHandler.ENTITY_AUTOMATION, EntityType.PLAYER,
                (player, context) -> new PlayerInvWrapper(player.getInventory()));
    }

    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(UNLOCK_SLOT);
        }
    }

    private void addBuiltInResourcePacks(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            event.addPackFinders(
                    ResourceLocation.fromNamespaceAndPath(MODID, "resourcepacks/recolored_gui"),
                    PackType.CLIENT_RESOURCES,
                    Component.translatable("pack.infiniteinvo.recolored_gui"),
                    PackSource.BUILT_IN,
                    false,
                    Pack.Position.TOP);
        }
    }

    private void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                OpenInfiniteInventoryPayload.TYPE,
                OpenInfiniteInventoryPayload.STREAM_CODEC,
                OpenInfiniteInventoryPayload::handle);
        event.registrar("1").playToServer(
                CreativeInventoryPageRequestPayload.TYPE,
                CreativeInventoryPageRequestPayload.STREAM_CODEC,
                CreativeInventoryPageRequestPayload::handle);
        event.registrar("1").playToServer(
                CloseCreativeInventoryPagingPayload.TYPE,
                CloseCreativeInventoryPagingPayload.STREAM_CODEC,
                CloseCreativeInventoryPagingPayload::handle);
        event.registrar("1").playToServer(
                ClearInfiniteInventoryPayload.TYPE,
                ClearInfiniteInventoryPayload.STREAM_CODEC,
                ClearInfiniteInventoryPayload::handle);
        event.registrar("1").playToClient(
                CreativeInventoryPageDataPayload.TYPE,
                CreativeInventoryPageDataPayload.STREAM_CODEC,
                CreativeInventoryPageDataPayload::handle);
    }
}
