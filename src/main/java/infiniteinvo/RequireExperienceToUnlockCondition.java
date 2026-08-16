package infiniteinvo;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;

/** Enables the unlock-slot recipe only when slot unlocking is configured. */
public record RequireExperienceToUnlockCondition() implements ICondition {
    public static final MapCodec<RequireExperienceToUnlockCondition> CODEC =
            MapCodec.unit(new RequireExperienceToUnlockCondition());

    @Override
    public boolean test(IContext context) {
        return Config.requiresExperienceToUnlock();
    }

    @Override
    public MapCodec<RequireExperienceToUnlockCondition> codec() {
        return CODEC;
    }
}
