package com.eotv.echoofthevoid.gametest;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.campaign.CampaignBeat;
import com.eotv.echoofthevoid.campaign.CampaignCulminationState;
import com.eotv.echoofthevoid.campaign.CampaignDirectorRules;
import com.eotv.echoofthevoid.campaign.UncannyCampaignDirector;
import com.eotv.echoofthevoid.state.UncannyWorldState;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real Minecraft NBT checks kept out of the lightweight JUnit source set. */
@GameTestHolder(EchoOfTheVoid.MODID)
@PrefixGameTestTemplate(false)
public final class CampaignGameTests {
    private static final String TEMPLATE = "special_test_room";

    private CampaignGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void campaignStateIsAdditiveAndRoundTripsThroughMinecraftNbt(GameTestHelper helper) {
        UncannyWorldState legacy = UncannyWorldState.load(new CompoundTag(), helper.getLevel().registryAccess());
        helper.assertTrue(!legacy.isCampaignDirectorInitialized(),
                "A 1.1.1-style tag without campaign fields must remain uninitialized");
        helper.assertTrue(legacy.getCampaignElapsedTicks() == 0L,
                "A legacy world must not invent elapsed campaign time");
        helper.assertTrue(legacy.getCampaignRecentFamilies().isEmpty(),
                "A legacy world must begin with no anti-repetition memory");
        helper.assertTrue(legacy.getCampaignCulminationState() == CampaignCulminationState.UNINITIALIZED,
                "A legacy world must defer culmination scheduling to the director");

        UncannyWorldState original = UncannyWorldState.create();
        original.initializeCampaignDirector(240_000L, 258_000L, 0x454F5456L,
                CampaignBeat.PRESSURE.name(), 8_000L);
        original.advanceCampaignDirector(1_200L, 259_200L);
        original.startCampaignBeat(CampaignBeat.RELEASE.name(), 4_000L, 7);
        original.setCampaignLastStrongEventTick(239_000L);
        original.rememberCampaignFamily("PRESENCE", 6);
        original.rememberCampaignFamily("SOUND_TRAIL", 6);
        original.scheduleCampaignCulmination(1_050_000L);
        original.postponeCampaignCulmination(1_056_000L);

        CompoundTag saved = original.save(new CompoundTag(), helper.getLevel().registryAccess());
        UncannyWorldState restored = UncannyWorldState.load(saved, helper.getLevel().registryAccess());

        helper.assertTrue(restored.isCampaignDirectorInitialized(), "Campaign initialization must survive NBT");
        helper.assertTrue(restored.getCampaignElapsedTicks() == 241_200L, "Elapsed campaign ticks must survive NBT");
        helper.assertTrue(restored.getCampaignLastObservedDayTime() == 259_200L, "Observed dayTime must survive NBT");
        helper.assertTrue(restored.getCampaignDirectorSeed() == 0x454F5456L, "Director seed must survive NBT");
        helper.assertTrue(CampaignBeat.RELEASE.name().equals(restored.getCampaignBeat()), "Beat must survive NBT");
        helper.assertTrue(restored.getCampaignBeatSequence() == 7, "Beat sequence must survive NBT");
        helper.assertTrue(restored.getCampaignLastStrongEventTick() == 239_000L,
                "Strong-event spacing memory must survive NBT");
        helper.assertTrue(restored.getCampaignRecentFamilies().equals(java.util.List.of("SOUND_TRAIL", "PRESENCE")),
                "Recent families must preserve bounded order");
        helper.assertTrue(restored.getCampaignCulminationState() == CampaignCulminationState.PENDING,
                "Culmination state must survive NBT");
        helper.assertTrue(restored.getCampaignCulminationScheduledTick() == 1_050_000L,
                "Culmination schedule must survive NBT");
        helper.assertTrue(restored.getCampaignCulminationRetryTick() == 1_056_000L,
                "Culmination retry must survive NBT");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void lateNaturalMajorSatisfiesPendingCulmination(GameTestHelper helper) {
        UncannyWorldState state = UncannyWorldState.create();
        long dayForty = 40L * CampaignDirectorRules.TICKS_PER_DAY;
        state.initializeCampaignDirector(dayForty, dayForty, 17L, CampaignBeat.PRESSURE.name(), 4_000L);
        state.scheduleCampaignCulmination(44L * CampaignDirectorRules.TICKS_PER_DAY);

        UncannyCampaignDirector.recordNaturalMajorEventStarted(state);

        helper.assertTrue(state.getCampaignCulminationState() == CampaignCulminationState.SATISFIED,
                "A natural Tension Builder or Grand Warden at 80% must satisfy the hidden culmination");
        helper.assertTrue(state.getCampaignLastStrongEventTick() == dayForty,
                "The same major start must feed strong-event spacing memory");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void earlyNaturalMajorDoesNotConsumePendingCulmination(GameTestHelper helper) {
        UncannyWorldState state = UncannyWorldState.create();
        long dayThirtyNine = 39L * CampaignDirectorRules.TICKS_PER_DAY;
        state.initializeCampaignDirector(
                dayThirtyNine, dayThirtyNine, 18L, CampaignBeat.PRESSURE.name(), 4_000L);
        state.scheduleCampaignCulmination(44L * CampaignDirectorRules.TICKS_PER_DAY);

        UncannyCampaignDirector.recordNaturalMajorEventStarted(state);

        helper.assertTrue(state.getCampaignCulminationState() == CampaignCulminationState.PENDING,
                "A major event before 80% must not consume the one campaign culmination");
        helper.assertTrue(state.getCampaignLastStrongEventTick() == dayThirtyNine,
                "Early major events must still feed strong-event spacing memory");
        helper.succeed();
    }
}
