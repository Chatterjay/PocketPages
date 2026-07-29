package infiniteinvo;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue TOTAL_EXTRA_SLOTS;
    public static final ModConfigSpec.IntValue START_UNLOCKED_SLOTS;
    public static final ModConfigSpec.BooleanValue REQUIRE_EXPERIENCE_TO_UNLOCK;
    public static final ModConfigSpec.IntValue UNLOCK_COST;
    public static final ModConfigSpec.IntValue UNLOCK_COST_INCREASE;
    public static final ModConfigSpec.BooleanValue USE_EXPERIENCE_POINTS;
    public static final ModConfigSpec.BooleanValue KEEP_UNLOCKS_ON_DEATH;
    public static final ModConfigSpec.IntValue LOCKED_ITEM_DROP_CHECK_INTERVAL_TICKS;
    public static final ModConfigSpec.BooleanValue REMEMBER_CONTAINER_PAGE;
    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("server");
        TOTAL_EXTRA_SLOTS = BUILDER
                .comment("Total inventory slots managed by InfiniteInvo, excluding the hotbar. Must match between client and server.")
                .translation("infiniteinvo.configuration.total_extra_slots")
                .defineInRange("totalExtraSlots", 54, 36, 2160);
        START_UNLOCKED_SLOTS = BUILDER
                .comment("Number of InfiniteInvo slots unlocked for new players. Set this to totalExtraSlots to start with every slot unlocked.")
                .translation("infiniteinvo.configuration.start_unlocked_slots")
                .defineInRange("startUnlockedSlots", 54, 0, 2160);
        REQUIRE_EXPERIENCE_TO_UNLOCK = BUILDER
                .comment("Whether non-creative players must spend experience to unlock slots. When false, every configured slot is unlocked.")
                .translation("infiniteinvo.configuration.require_experience_to_unlock")
                .define("requireExperienceToUnlock", false);
        UNLOCK_COST = BUILDER
                .comment("Base cost to unlock one slot. This is levels by default, or experience points when useExperiencePoints is enabled.")
                .translation("infiniteinvo.configuration.unlock_cost")
                .defineInRange("unlockCost", 10, 0, 10000);
        UNLOCK_COST_INCREASE = BUILDER
                .comment("Additional unlock cost for each slot already unlocked beyond the initial amount.")
                .translation("infiniteinvo.configuration.unlock_cost_increase")
                .defineInRange("unlockCostIncrease", 1, 0, 10000);
        USE_EXPERIENCE_POINTS = BUILDER
                .comment("Charge raw experience points instead of experience levels when unlocking slots.")
                .translation("infiniteinvo.configuration.use_experience_points")
                .define("useExperiencePoints", false);
        KEEP_UNLOCKS_ON_DEATH = BUILDER
                .comment("Whether unlocked extra slots survive death when keepInventory is false.")
                .translation("infiniteinvo.configuration.keep_unlocks_on_death")
                .define("keepUnlocksOnDeath", true);
        LOCKED_ITEM_DROP_CHECK_INTERVAL_TICKS = BUILDER
                .comment("Server ticks between locked-slot safety checks. One tick gives immediate cleanup; increase only to reduce work for very large player counts.")
                .translation("infiniteinvo.configuration.locked_item_drop_check_interval_ticks")
                .defineInRange("lockedItemDropCheckIntervalTicks", 1, 1, 1200);
        BUILDER.pop();

        BUILDER.push("client");
        REMEMBER_CONTAINER_PAGE = BUILDER
                .comment("Whether regular container screens reopen on the last extended inventory page.")
                .translation("infiniteinvo.configuration.remember_container_page")
                .define("rememberContainerPage", true);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private Config() {
    }

    public static int totalExtraSlots() {
        return Math.max(36, TOTAL_EXTRA_SLOTS.get());
    }

    public static int startUnlockedSlots() {
        return Math.min(Math.max(36, START_UNLOCKED_SLOTS.get()), totalExtraSlots());
    }

    public static int unlockCost(int alreadyUnlocked) {
        return Math.max(0, UNLOCK_COST.get() + Math.max(0, alreadyUnlocked - startUnlockedSlots()) * UNLOCK_COST_INCREASE.get());
    }

    /**
     * Returns the number of experience points represented by an unlock cost.
     * The level mode intentionally matches the original mod: a cost of N levels
     * removes the points needed to reach level N from level zero.
     */
    public static int unlockCostInExperiencePoints(int alreadyUnlocked) {
        int cost = unlockCost(alreadyUnlocked);
        return usesExperiencePoints() ? cost : experiencePointsForLevel(cost);
    }

    public static boolean requiresExperienceToUnlock() {
        return REQUIRE_EXPERIENCE_TO_UNLOCK.get();
    }

    public static boolean usesExperiencePoints() {
        return USE_EXPERIENCE_POINTS.get();
    }

    private static int experiencePointsForLevel(int level) {
        if (level <= 16) {
            return level * level + 6 * level;
        }
        if (level <= 31) {
            return (int) (2.5D * level * level - 40.5D * level + 360.0D);
        }
        return (int) (4.5D * level * level - 162.5D * level + 2220.0D);
    }
}
