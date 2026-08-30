package pocketpages.inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

/** World-owned storage keeps extended inventories out of each player's entity NBT. */
public final class PocketPagesInventorySavedData extends SavedData {
    private static final String DATA_ID = "pocketpages_inventory";
    private static final String LEGACY_DATA_ID = "infiniteinvo_inventory";
    private static final SavedData.Factory<PocketPagesInventorySavedData> FACTORY =
            new SavedData.Factory<>(PocketPagesInventorySavedData::new, PocketPagesInventorySavedData::load);
    private final Map<UUID, PocketPagesInventoryState> inventories = new HashMap<>();

    public static PocketPagesInventorySavedData get(ServerLevel level) {
        DimensionDataStorage storage = level.getServer().overworld().getDataStorage();
        PocketPagesInventorySavedData current = storage.get(FACTORY, DATA_ID);
        if (current != null) {
            return current;
        }

        PocketPagesInventorySavedData legacy = storage.get(FACTORY, LEGACY_DATA_ID);
        if (legacy != null) {
            storage.set(DATA_ID, legacy);
            legacy.setDirty();
            return legacy;
        }

        return storage.computeIfAbsent(FACTORY, DATA_ID);
    }

    private static PocketPagesInventorySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PocketPagesInventorySavedData data = new PocketPagesInventorySavedData();
        for (String key : tag.getAllKeys()) {
            try {
                UUID playerId = UUID.fromString(key);
                PocketPagesInventoryState state = new PocketPagesInventoryState();
                state.deserializeNBT(registries, tag.getCompound(key));
                data.inventories.put(playerId, state);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return data;
    }

    public PocketPagesInventoryState getOrCreate(UUID playerId) {
        PocketPagesInventoryState state = inventories.get(playerId);
        if (state == null) {
            state = new PocketPagesInventoryState();
            inventories.put(playerId, state);
            setDirty();
        }
        return state;
    }

    public void replace(UUID playerId, PocketPagesInventoryState state) {
        inventories.put(playerId, state);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        for (Map.Entry<UUID, PocketPagesInventoryState> entry : inventories.entrySet()) {
            tag.put(entry.getKey().toString(), entry.getValue().serializeNBT(registries));
        }
        return tag;
    }
}
