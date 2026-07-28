package infiniteinvo.inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** World-owned storage keeps extended inventories out of each player's entity NBT. */
public final class InfiniteInventorySavedData extends SavedData {
    private static final String DATA_ID = "infiniteinvo_inventory";
    private final Map<UUID, InfiniteInventoryState> inventories = new HashMap<>();

    public static InfiniteInventorySavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(InfiniteInventorySavedData::new, InfiniteInventorySavedData::load), DATA_ID);
    }

    private static InfiniteInventorySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        InfiniteInventorySavedData data = new InfiniteInventorySavedData();
        for (String key : tag.getAllKeys()) {
            try {
                UUID playerId = UUID.fromString(key);
                InfiniteInventoryState state = new InfiniteInventoryState();
                state.deserializeNBT(registries, tag.getCompound(key));
                data.inventories.put(playerId, state);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return data;
    }

    public InfiniteInventoryState getOrCreate(UUID playerId) {
        InfiniteInventoryState state = inventories.get(playerId);
        if (state == null) {
            state = new InfiniteInventoryState();
            inventories.put(playerId, state);
            setDirty();
        }
        return state;
    }

    public void replace(UUID playerId, InfiniteInventoryState state) {
        inventories.put(playerId, state);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        for (Map.Entry<UUID, InfiniteInventoryState> entry : inventories.entrySet()) {
            tag.put(entry.getKey().toString(), entry.getValue().serializeNBT(registries));
        }
        return tag;
    }
}
