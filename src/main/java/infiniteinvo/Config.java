package infiniteinvo;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue TOTAL_EXTRA_SLOTS = BUILDER
            .comment("Total extra inventory slots provided by InfiniteInvo.")
            .defineInRange("totalExtraSlots", 54, 72, 2160);

    public static final ModConfigSpec.IntValue START_UNLOCKED_SLOTS = BUILDER
            .comment("Extra slots unlocked for every non-creative player by default.")
            .defineInRange("startUnlockedSlots", 36, 0, 2160);

    public static final ModConfigSpec.IntValue UNLOCK_COST = BUILDER
            .comment("Base experience level cost when unlocking a slot from the menu.")
            .defineInRange("unlockCost", 10, 0, 10000);

    public static final ModConfigSpec.IntValue UNLOCK_COST_INCREASE = BUILDER
            .comment("Additional experience level cost for each already unlocked extra slot.")
            .defineInRange("unlockCostIncrease", 1, 0, 10000);

    public static final ModConfigSpec.BooleanValue KEEP_UNLOCKS_ON_DEATH = BUILDER
            .comment("Whether unlocked extra slots survive death when keepInventory is false.")
            .define("keepUnlocksOnDeath", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }

    public static int totalExtraSlots() {
        return Math.max(72, TOTAL_EXTRA_SLOTS.get());
    }

    public static int startUnlockedSlots() {
        return Math.min(Math.max(36, START_UNLOCKED_SLOTS.get()), totalExtraSlots());
    }

    public static int unlockCost(int alreadyUnlocked) {
        return UNLOCK_COST.get() + Math.max(0, alreadyUnlocked) * UNLOCK_COST_INCREASE.get();
    }
}
