package com.eotv.echoofthevoid.gametest;

import com.eotv.echoofthevoid.EchoOfTheVoid;
import com.eotv.echoofthevoid.event.paranoia.nativeevent.AnimalFormationRules;
import com.eotv.echoofthevoid.event.paranoia.nativeevent.AnimalFormationSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Animal formations on real cows: frozen in place, released when disturbed, never left frozen. */
@GameTestHolder(EchoOfTheVoid.MODID)
@PrefixGameTestTemplate(false)
public final class AnimalFormationGameTests {
    private static final String TEMPLATE = "special_test_room";

    private AnimalFormationGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void circleFreezesRealAnimalsFacingTheCentreAndReleasesThemWhenOneIsHurt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper);
        Vec3 centre = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(8, 1, 8)));
        List<Animal> cows = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            cows.add(helper.spawn(EntityType.COW, new BlockPos(2 + i * 2, 1, 2)));
        }
        double radius = AnimalFormationRules.circleRadius(cows.size());
        List<Vec3> slots = new ArrayList<>();
        List<Float> yaws = new ArrayList<>();
        for (double[] point : AnimalFormationRules.circlePoints(centre.x, centre.z, radius, cows.size(), 0.0D)) {
            slots.add(new Vec3(point[0], centre.y, point[1]));
            yaws.add(AnimalFormationRules.yawToward(point[0], point[1], centre.x, centre.z));
        }
        AnimalFormationSystem.formForTest(level, AnimalFormationSystem.Variant.CIRCLE, centre, cows, slots, yaws);

        for (Animal cow : cows) {
            helper.assertTrue(cow.isNoAi() && cow.isSilent() && cow.getTags().contains(AnimalFormationSystem.FORMATION_TAG),
                    "Every animal of the formation must be frozen and silent");
            double distance = Math.hypot(cow.getX() - centre.x, cow.getZ() - centre.z);
            helper.assertTrue(Math.abs(distance - radius) < 0.05D, "Each animal must stand on the circle");
            Vec3 look = Vec3.directionFromRotation(0.0F, cow.getYRot());
            Vec3 toCentre = new Vec3(centre.x - cow.getX(), 0.0D, centre.z - cow.getZ()).normalize();
            helper.assertTrue(look.dot(toCentre) > 0.99D, "Each animal must face the empty centre");
        }

        cows.get(0).hurt(level.damageSources().generic(), 0.5F);
        helper.runAfterDelay(30, () -> {
            for (Animal cow : cows) {
                helper.assertTrue(!cow.isNoAi() && !cow.isSilent() && !cow.getTags().contains(AnimalFormationSystem.FORMATION_TAG),
                        "Hurting one animal must give every animal its ordinary behaviour back");
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void animalSavedMidFormationWakesUpWhenItLoadsAgain(GameTestHelper helper) {
        floor(helper);
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(8, 1, 8));
        cow.getPersistentData().putBoolean("eotv_formation_no_ai", false);
        cow.getPersistentData().putBoolean("eotv_formation_silent", false);
        cow.addTag(AnimalFormationSystem.FORMATION_TAG);
        cow.setNoAi(true);
        cow.setSilent(true);

        AnimalFormationSystem.onEntityJoinLevel(new EntityJoinLevelEvent(cow, helper.getLevel()));

        helper.assertTrue(!cow.isNoAi() && !cow.isSilent() && !cow.getTags().contains(AnimalFormationSystem.FORMATION_TAG),
                "An orphaned formation animal must be restored on load");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void gridFillsAFencedPenWithGapsAndOpenFieldsAreRefused(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        floor(helper);
        // Fence ring around a 7 x 5 interior: x 4..10, z 5..9.
        for (int x = 3; x <= 11; x++) {
            helper.setBlock(new BlockPos(x, 1, 4), Blocks.OAK_FENCE);
            helper.setBlock(new BlockPos(x, 1, 10), Blocks.OAK_FENCE);
        }
        for (int z = 4; z <= 10; z++) {
            helper.setBlock(new BlockPos(3, 1, z), Blocks.OAK_FENCE);
            helper.setBlock(new BlockPos(11, 1, z), Blocks.OAK_FENCE);
        }
        List<Vec3> slots = AnimalFormationSystem.planGridSlotsForTest(level, helper.absolutePos(new BlockPos(7, 1, 7)));
        helper.assertTrue(slots.size() == 6, "A 7 x 5 pen must offer six grid cells, found " + slots.size());
        for (Vec3 slot : slots) {
            BlockPos feet = BlockPos.containing(slot);
            for (BlockPos near : List.of(feet.north(), feet.south(), feet.east(), feet.west())) {
                helper.assertTrue(!level.getBlockState(near).is(Blocks.OAK_FENCE), "Grid cells never touch the fence");
            }
        }
        // Open the pen: the flood escapes the size bound and no grid is planned.
        helper.setBlock(new BlockPos(7, 1, 4), Blocks.AIR);
        helper.assertTrue(AnimalFormationSystem.planGridSlotsForTest(level, helper.absolutePos(new BlockPos(7, 1, 7))).isEmpty(),
                "An open pen must not be treated as an enclosure");
        helper.succeed();
    }

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.setBlock(x, 0, z, Blocks.GRASS_BLOCK);
            }
        }
    }
}
