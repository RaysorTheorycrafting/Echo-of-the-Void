package com.eotv.echoofthevoid.event.passive;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Immutable catalog for additive variants that remain their real Vanilla entity type.
 *
 * <p>Every compatible species in this layer has five inspectable variants. The first twenty
 * entries keep the IDs and behavior contracts shipped before the diversity expansion; the
 * other entries are deliberately data-driven so adding a species does not add another entity
 * class or registry ID.</p>
 */
public final class ApprovedVanillaVariantCatalog {
    private static final List<Species> SPECIES = List.of(
            species("bee", "Bee?", Rarity.RARE, 0,
                    b("false_hive", "False Hive", "Inspects an ordinary block face as though it were a hive, without changing honey or occupants.", 1, BehaviorKind.SPECIALIZED, VisualStyle.NORMAL, false, 7),
                    b("borrowed_flower", "Borrowed Flower", "Stops over an empty patch and performs a short, fruitless pollination inspection.", 2, BehaviorKind.EMPTY_FOCUS, VisualStyle.NORMAL, false, 6),
                    b("wrong_return", "Wrong Return", "Approaches an empty point beside its route, then resumes its genuine hive logic.", 2, BehaviorKind.EMPTY_APPROACH, VisualStyle.NORMAL, false, 5),
                    b("rearward_witness", "Rearward Witness", "Tracks the nearest player only while that player is looking elsewhere.", 3, BehaviorKind.REARWARD_GAZE, VisualStyle.PALE, false, 4),
                    b("still_swarm", "Still Swarm", "Freezes for a few beats when directly observed, then yields immediately to anger or hive work.", 4, BehaviorKind.WATCHED_STILLNESS, VisualStyle.NARROW, false, 3)),
            species("bat", "Bat?", Rarity.UNCOMMON, 0,
                    b("wrong_roost", "Wrong Roost", "Leaves its perch after eye contact, circles once and returns to the exact same ceiling block.", 1, BehaviorKind.SPECIALIZED, VisualStyle.NORMAL, false, 7),
                    b("late_wingbeat", "Late Wingbeat", "Leaves a delayed physical trace at a position it has already flown past.", 2, BehaviorKind.DELAYED_TRACE, VisualStyle.NORMAL, false, 6),
                    b("empty_ceiling", "Empty Ceiling", "Faces and studies a nearby point under the ceiling where nothing is hanging.", 2, BehaviorKind.LOOK_ABOVE, VisualStyle.WIDE, false, 5),
                    b("repeated_circle", "Repeated Circle", "Circles a fixed point of empty air before returning to ordinary flight.", 3, BehaviorKind.CIRCLE_EMPTY, VisualStyle.MIRRORED, false, 4),
                    b("lightless", "Lightless", "A completely black, completely silent bat that becomes still whenever it is checked directly.", 4, BehaviorKind.WATCHED_STILLNESS, VisualStyle.PITCH_BLACK, true, 1)),
            species("rabbit", "Rabbit?", Rarity.RARE, 0,
                    b("return_to_cover", "Return to Cover", "Returns briefly to the edge of the exact cover used during a real Vanilla escape.", 1, BehaviorKind.SPECIALIZED, VisualStyle.NORMAL, false, 7),
                    b("wrong_footprint", "Wrong Footprint", "Produces a delayed landing trace at a place it has already left.", 2, BehaviorKind.DELAYED_TRACE, VisualStyle.NORMAL, false, 6),
                    b("empty_listener", "Empty Listener", "Stops and turns one ear toward an empty point beside the player.", 2, BehaviorKind.EMPTY_FOCUS, VisualStyle.NARROW, false, 5),
                    b("remembered_hop", "Remembered Hop", "Briefly returns to an earlier safe position after the player has moved on.", 3, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.NORMAL, false, 4),
                    b("watchstep", "Watchstep", "Advances only in short idle hesitations while it remains in direct view.", 4, BehaviorKind.STAGGERED_PAUSE, VisualStyle.TALL, false, 3)),
            species("goat", "Goat?", Rarity.RARE, 1,
                    b("echo_ram", "Echo Ram", "Repeats the dust and impact of a genuine ram on a nearby wall without a second hit.", 2, BehaviorKind.SPECIALIZED, VisualStyle.NORMAL, false, 7),
                    b("cliff_listener", "Cliff Listener", "Looks above the edge of nearby terrain as if following a second goat out of sight.", 2, BehaviorKind.LOOK_ABOVE, VisualStyle.NORMAL, false, 6),
                    b("empty_herd", "Empty Herd", "Walks toward a short-lived place in an otherwise empty part of the herd.", 2, BehaviorKind.EMPTY_APPROACH, VisualStyle.WIDE, false, 5),
                    b("backward_watch", "Backward Watch", "Keeps its body misaligned with the player while no combat or charge is active.", 3, BehaviorKind.WRONG_FACING, VisualStyle.MIRRORED, false, 4),
                    b("impact_memory", "Impact Memory", "Returns once to an old standing point long after its real charge has ended.", 4, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.ASHEN, false, 3)),
            species("horse", "Horse?", Rarity.RARE, 0,
                    b("empty_rider", "Empty Rider", "A natural, unclaimed horse rears as though an unseen rider mounted or dismounted.", 2, BehaviorKind.SPECIALIZED, VisualStyle.NORMAL, false, 7),
                    b("phantom_reins", "Phantom Reins", "Turns toward an empty point as though its reins were pulled from that side.", 2, BehaviorKind.EMPTY_FOCUS, VisualStyle.NORMAL, false, 6),
                    b("remembered_hitch", "Remembered Hitch", "Returns briefly to an earlier idle position without creating a lead or knot.", 2, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.NARROW, false, 5),
                    b("late_hoof", "Late Hoof", "Leaves one quiet hoof trace behind after moving away.", 3, BehaviorKind.DELAYED_TRACE, VisualStyle.NORMAL, false, 4),
                    b("wrong_canter", "Wrong Canter", "Hesitates in an uneven rhythm only while untamed and free of a rider.", 4, BehaviorKind.STAGGERED_PAUSE, VisualStyle.TALL, false, 3)),
            species("allay", "Allay?", Rarity.VERY_RARE, 0,
                    b("wrong_recipient", "Wrong Recipient", "Offers its carried item toward empty air for a moment, but never releases or loses it.", 2, BehaviorKind.SPECIALIZED, VisualStyle.NORMAL, false, 7),
                    b("empty_noteblock", "Empty Note Block", "Faces a silent point as though a note block had called it from there.", 2, BehaviorKind.EMPTY_FOCUS, VisualStyle.NORMAL, false, 6),
                    b("borrowed_orbit", "Borrowed Orbit", "Circles a point beside its real recipient before completing Vanilla delivery.", 2, BehaviorKind.CIRCLE_EMPTY, VisualStyle.WIDE, false, 5),
                    b("mirrored_helper", "Mirrored Helper", "Copies a nearby player's gaze while it is carrying nothing urgent.", 3, BehaviorKind.MIRROR_PLAYER, VisualStyle.MIRRORED, false, 4),
                    b("unlit_helper", "Unlit Helper", "A completely black, completely silent Allay that watches from an empty delivery point.", 4, BehaviorKind.REARWARD_GAZE, VisualStyle.PITCH_BLACK, true, 1)),
            species("axolotl", "Axolotl?", Rarity.RARE, 0,
                    b("healthy_feign", "Healthy Feign", "Briefly plays dead while healthy and unthreatened, without changing its real defense.", 2, BehaviorKind.SPECIALIZED, VisualStyle.NORMAL, false, 7),
                    b("wrong_surface", "Wrong Surface", "Studies a point above the water as if something surfaced there first.", 2, BehaviorKind.LOOK_ABOVE, VisualStyle.NORMAL, false, 6),
                    b("still_gills", "Still Gills", "Stops moving for a short watched interval while all genuine threats retain priority.", 2, BehaviorKind.WATCHED_STILLNESS, VisualStyle.WIDE, false, 5),
                    b("second_turn", "Second Turn", "Retraces a recent harmless turn and then resumes its swim.", 3, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.MIRRORED, false, 4),
                    b("empty_school", "Empty School", "Circles a common point that contains no fish or player.", 4, BehaviorKind.CIRCLE_EMPTY, VisualStyle.PALE, false, 3)),
            species("dolphin", "Dolphin?", Rarity.RARE, 0,
                    b("blindside_escort", "Blindside Escort", "Keeps a precise place behind a swimmer or boat only while it is not observed.", 2, BehaviorKind.SPECIALIZED, VisualStyle.NORMAL, false, 7),
                    b("empty_treasure", "Empty Treasure", "Turns toward an empty patch of water as though reconsidering a treasure route.", 2, BehaviorKind.EMPTY_FOCUS, VisualStyle.NORMAL, false, 6),
                    b("wake_return", "Wake Return", "Leaves a delayed trace in water at a position it has already crossed.", 2, BehaviorKind.DELAYED_TRACE, VisualStyle.NARROW, false, 5),
                    b("unseen_circle", "Unseen Circle", "Circles an empty point only while nearby swimmers look away.", 3, BehaviorKind.CIRCLE_EMPTY, VisualStyle.MIRRORED, false, 4),
                    b("remembered_escort", "Remembered Escort", "Returns once to an old escort position after its swimmer has moved on.", 4, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.PALE, false, 3)),
            species("frog", "Frog?", Rarity.RARE, 0,
                    b("empty_tongue", "Empty Tongue", "Strikes empty air with its tongue outside any real prey interaction.", 1, BehaviorKind.SPECIALIZED, VisualStyle.NORMAL, false, 7),
                    b("wrong_lily", "Wrong Lily", "Approaches a bare point beside the water as if it were a landing surface.", 2, BehaviorKind.EMPTY_APPROACH, VisualStyle.NORMAL, false, 6),
                    b("below_water", "Below Water", "Looks into an empty block below itself and waits for a response.", 2, BehaviorKind.EMPTY_FOCUS, VisualStyle.WIDE, false, 5),
                    b("backward_hop", "Backward Hop", "Faces away from its harmless movement for a brief, visibly incorrect beat.", 3, BehaviorKind.WRONG_FACING, VisualStyle.MIRRORED, false, 4),
                    b("remembered_bank", "Remembered Bank", "Returns to an earlier bank position after the nearby player stops checking it.", 4, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.TALL, false, 3)),
            species("turtle", "Turtle?", Rarity.RARE, 0,
                    b("false_nest", "False Nest", "Scratches a beach that is not its home and lays no egg or block change.", 2, BehaviorKind.SPECIALIZED, VisualStyle.NORMAL, false, 7),
                    b("wrong_home", "Wrong Home", "Walks a few blocks toward an empty false home, then restores Vanilla navigation.", 2, BehaviorKind.EMPTY_APPROACH, VisualStyle.NORMAL, false, 6),
                    b("empty_hatch", "Empty Hatch", "Faces a patch of sand as though waiting for an egg that is not present.", 2, BehaviorKind.EMPTY_FOCUS, VisualStyle.WIDE, false, 5),
                    b("second_scrape", "Second Scrape", "Leaves a delayed sand trace at a position it has already crossed.", 3, BehaviorKind.DELAYED_TRACE, VisualStyle.ASHEN, false, 4),
                    b("shore_memory", "Shore Memory", "Returns once to an old shoreline point without changing its real home position.", 4, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.NARROW, false, 3)),
            species("sniffer", "Sniffer?", Rarity.VERY_RARE, 0,
                    b("second_dig", "Second Dig", "Repeats a real old excavation animation without loot or consuming its Vanilla cooldown.", 2, BehaviorKind.SPECIALIZED, VisualStyle.NORMAL, false, 7),
                    b("wrong_seed", "Wrong Seed", "Follows a short empty scent trail and stops before digging.", 2, BehaviorKind.EMPTY_APPROACH, VisualStyle.NORMAL, false, 6),
                    b("empty_scent", "Empty Scent", "Studies a point in the air where no diggable block exists.", 2, BehaviorKind.EMPTY_FOCUS, VisualStyle.WIDE, false, 5),
                    b("returning_trail", "Returning Trail", "Walks back to an earlier sniffing position, then abandons it.", 3, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.ASHEN, false, 4),
                    b("shared_pause", "Shared Pause", "Nearby calm Sniffers converge toward the same empty point and separate again.", 4, BehaviorKind.GROUP_CONVERGENCE, VisualStyle.TALL, false, 3)),
            species("armadillo", "Armadillo?", Rarity.RARE, 0,
                    b("empty_threat", "Empty Threat", "Rolls up toward a point with no real threat, while true danger still overrides it.", 1, BehaviorKind.SPECIALIZED, VisualStyle.NORMAL, false, 7),
                    b("wrong_unroll", "Wrong Unroll", "Turns toward an empty point immediately after a harmless pause.", 2, BehaviorKind.EMPTY_FOCUS, VisualStyle.NORMAL, false, 6),
                    b("second_rattle", "Second Rattle", "Leaves a delayed ground trace after it has already moved on.", 2, BehaviorKind.DELAYED_TRACE, VisualStyle.WIDE, false, 5),
                    b("empty_brush", "Empty Brush", "Makes one interaction gesture toward empty ground without creating a scute.", 3, BehaviorKind.FALSE_INTERACTION, VisualStyle.ASHEN, false, 4),
                    b("black_shell", "Black Shell", "A completely black, completely silent Armadillo that closes under direct observation.", 4, BehaviorKind.WATCHED_STILLNESS, VisualStyle.PITCH_BLACK, true, 1)),
            species("glow_squid", "Glow Squid?", Rarity.RARE, 0,
                    b("light_lag", "Light Lag", "Leaves a sub-second luminous afterimage at its previous position after a sharp turn.", 2, BehaviorKind.SPECIALIZED, VisualStyle.NORMAL, false, 7),
                    b("delayed_ink", "Delayed Ink", "Leaves a dark delayed trace behind a harmless change of direction.", 2, BehaviorKind.DELAYED_TRACE, VisualStyle.NORMAL, false, 6),
                    b("wrong_pulse", "Wrong Pulse", "Faces an empty point while its body appears briefly compressed.", 2, BehaviorKind.EMPTY_FOCUS, VisualStyle.COMPRESSED, false, 5),
                    b("dark_return", "Dark Return", "Returns to a previous water position after leaving it outside the player's focus.", 3, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.ASHEN, false, 4),
                    b("extinguished", "Extinguished", "A completely black, completely silent Glow Squid with no visible glow response.", 4, BehaviorKind.REARWARD_GAZE, VisualStyle.PITCH_BLACK, true, 1)),
            species("breeze", "Breeze?", Rarity.RARE, 2,
                    b("returned_wind", "Returned Wind", "Retraces a real wind charge with harmless dust after the projectile ends.", 3, BehaviorKind.SPECIALIZED, VisualStyle.NORMAL, false, 7),
                    b("empty_aim", "Empty Aim", "Makes one attack-like gesture toward empty air while no real target exists.", 3, BehaviorKind.FALSE_INTERACTION, VisualStyle.NORMAL, false, 6),
                    b("backflow", "Backflow", "Leaves a delayed ground trace opposite its recent movement.", 3, BehaviorKind.DELAYED_TRACE, VisualStyle.MIRRORED, false, 5),
                    b("still_rods", "Still Rods", "Becomes briefly motionless under observation only outside combat.", 3, BehaviorKind.WATCHED_STILLNESS, VisualStyle.TALL, false, 4),
                    b("wrong_leap", "Wrong Leap", "Turns away from an empty landing point before resuming Vanilla combat logic.", 4, BehaviorKind.WRONG_FACING, VisualStyle.NARROW, false, 3)),
            species("cave_spider", "Cave Spider?", Rarity.RARE, 3,
                    b("ceiling_wait", "Ceiling Wait", "Waits audibly near a ceiling and descends before its ordinary attack.", 3, BehaviorKind.SPECIALIZED, VisualStyle.NORMAL, false, 7),
                    b("false_retreat", "False Retreat", "Returns to a recent wall position only while it has no active target.", 3, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.NORMAL, false, 6),
                    b("empty_web", "Empty Web", "Looks toward a bare corner as though a web line continued through it.", 3, BehaviorKind.EMPTY_FOCUS, VisualStyle.WIDE, false, 5),
                    b("stutter_climb", "Stutter Climb", "Interrupts idle climbing in a short irregular rhythm without accelerating an attack.", 3, BehaviorKind.STAGGERED_PAUSE, VisualStyle.COMPRESSED, false, 4),
                    b("wrong_descent", "Wrong Descent", "Faces away from its idle descent for one visibly incorrect interval.", 4, BehaviorKind.WRONG_FACING, VisualStyle.MIRRORED, false, 3)),
            species("shulker", "Shulker?", Rarity.RARE, 2,
                    b("empty_aim", "Empty Aim", "Opens and tracks empty air without creating a projectile.", 3, BehaviorKind.SPECIALIZED, VisualStyle.NORMAL, false, 7),
                    b("delayed_peek", "Delayed Peek", "Performs an interaction-like opening gesture after nearby activity has ended.", 3, BehaviorKind.FALSE_INTERACTION, VisualStyle.NORMAL, false, 6),
                    b("wrong_face", "Wrong Face", "Looks toward a different exposed face while it has no real target.", 3, BehaviorKind.EMPTY_FOCUS, VisualStyle.MIRRORED, false, 5),
                    b("shared_close", "Shared Close", "Calm nearby Shulkers briefly orient around one empty point.", 3, BehaviorKind.GROUP_CONVERGENCE, VisualStyle.COMPRESSED, false, 4),
                    b("false_recoil", "False Recoil", "Hesitates as if a shot had just left, while no bullet is spawned.", 4, BehaviorKind.STAGGERED_PAUSE, VisualStyle.ASHEN, false, 3)),
            species("guardian", "Guardian?", Rarity.RARE, 2,
                    b("false_beam", "False Beam", "Shows a short beam toward empty water without a target, damage or charge.", 3, BehaviorKind.SPECIALIZED, VisualStyle.NORMAL, false, 7),
                    b("empty_guard", "Empty Guard", "Turns toward an unoccupied point of water and holds it briefly.", 3, BehaviorKind.EMPTY_FOCUS, VisualStyle.NORMAL, false, 6),
                    b("delayed_spikes", "Delayed Spikes", "Leaves one delayed water trace after changing direction.", 3, BehaviorKind.DELAYED_TRACE, VisualStyle.WIDE, false, 5),
                    b("wrong_patrol", "Wrong Patrol", "Approaches an empty point before yielding to its real patrol or target.", 3, BehaviorKind.EMPTY_APPROACH, VisualStyle.MIRRORED, false, 4),
                    b("beam_memory", "Beam Memory", "Returns once to an old guard position where no target remains.", 4, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.PALE, false, 3)),
            species("vex", "Vex?", Rarity.VERY_RARE, 3,
                    b("caught_between", "Caught Between", "Remains partly visible in a wall for less than a second and delays its attack.", 3, BehaviorKind.SPECIALIZED, VisualStyle.NORMAL, false, 7),
                    b("empty_charge", "Empty Charge", "Makes one harmless attack gesture when it has no genuine target.", 3, BehaviorKind.FALSE_INTERACTION, VisualStyle.NORMAL, false, 6),
                    b("late_pass", "Late Pass", "Leaves a delayed trace after it has already crossed a wall or room.", 3, BehaviorKind.DELAYED_TRACE, VisualStyle.NARROW, false, 5),
                    b("wrong_exit", "Wrong Exit", "Approaches an empty point on the wrong side of its idle route.", 3, BehaviorKind.EMPTY_APPROACH, VisualStyle.MIRRORED, false, 4),
                    b("frozen_pass", "Frozen Pass", "Stops in direct view for a few beats, never while charging a real target.", 4, BehaviorKind.WATCHED_STILLNESS, VisualStyle.PALE, false, 3)),
            species("silverfish", "Silverfish?", Rarity.RARE, 1,
                    b("wrong_stone", "Wrong Stone", "Attempts to enter a non-infested stone block and then resumes Vanilla behavior.", 2, BehaviorKind.SPECIALIZED, VisualStyle.NORMAL, false, 7),
                    b("returned_crack", "Returned Crack", "Returns briefly to an earlier stone edge without infesting it.", 2, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.NORMAL, false, 6),
                    b("empty_swarm", "Empty Swarm", "Several calm individuals orient toward one empty point and disperse.", 2, BehaviorKind.GROUP_CONVERGENCE, VisualStyle.WIDE, false, 5),
                    b("delayed_scuttle", "Delayed Scuttle", "Leaves a tiny delayed block trace behind its path.", 3, BehaviorKind.DELAYED_TRACE, VisualStyle.COMPRESSED, false, 4),
                    b("repeated_entry", "Repeated Entry", "Approaches a second ordinary stone face but never changes the block.", 4, BehaviorKind.EMPTY_APPROACH, VisualStyle.ASHEN, false, 3)),
            species("zombified_piglin", "Zombified Piglin?", Rarity.VERY_RARE, 1,
                    b("procession", "Procession", "A calm group forms a short line toward empty air; any anger cancels it.", 2, BehaviorKind.SPECIALIZED, VisualStyle.NORMAL, false, 7),
                    b("empty_barter", "Empty Barter", "Makes one idle offering gesture toward an absent recipient.", 2, BehaviorKind.FALSE_INTERACTION, VisualStyle.NORMAL, false, 6),
                    b("wrong_formation", "Wrong Formation", "Calm nearby Piglins converge around an empty place and separate on anger.", 2, BehaviorKind.GROUP_CONVERGENCE, VisualStyle.WIDE, false, 5),
                    b("last_in_line", "Last in Line", "Returns to a recently abandoned position at the rear of a group.", 3, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.NARROW, false, 4),
                    b("backward_guard", "Backward Guard", "Faces away from the group while calm, with combat always taking priority.", 4, BehaviorKind.WRONG_FACING, VisualStyle.ASHEN, false, 3)),

            // Vanilla mobs not covered by the historical passive or replacement matrices.
            species("bogged", "Bogged?", Rarity.RARE, 2,
                    b("dry_rattle", "Dry Rattle", "Studies a dry block as though it were still part of a swamp.", 2, BehaviorKind.EMPTY_FOCUS, VisualStyle.NORMAL, false, 7),
                    b("empty_draw", "Empty Draw", "Makes one harmless bow-like gesture only while no target exists.", 2, BehaviorKind.FALSE_INTERACTION, VisualStyle.NORMAL, false, 6),
                    b("wrong_mire", "Wrong Mire", "Walks toward an empty patch beside its idle route and then gives it up.", 2, BehaviorKind.EMPTY_APPROACH, VisualStyle.WIDE, false, 5),
                    b("returned_arrow", "Returned Arrow", "Leaves a delayed ground trace where no fired arrow landed.", 3, BehaviorKind.DELAYED_TRACE, VisualStyle.ASHEN, false, 4),
                    b("still_moss", "Still Moss", "Freezes briefly when watched, but never while aiming or fighting.", 4, BehaviorKind.WATCHED_STILLNESS, VisualStyle.NARROW, false, 3)),
            species("camel", "Camel?", Rarity.RARE, 0,
                    b("empty_passenger", "Empty Passenger", "Turns toward its saddle as though a passenger were present when none is mounted.", 1, BehaviorKind.EMPTY_FOCUS, VisualStyle.NORMAL, false, 7),
                    b("wrong_horizon", "Wrong Horizon", "Keeps watching an empty horizon behind the nearby player.", 2, BehaviorKind.REARWARD_GAZE, VisualStyle.NORMAL, false, 6),
                    b("late_stride", "Late Stride", "Leaves one delayed sand or stone trace after moving on.", 2, BehaviorKind.DELAYED_TRACE, VisualStyle.TALL, false, 5),
                    b("remembered_caravan", "Remembered Caravan", "Returns briefly to an earlier caravan position while unridden.", 3, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.NARROW, false, 4),
                    b("black_caravan", "Black Caravan", "A completely black, completely silent Camel that watches only while unridden.", 4, BehaviorKind.REARWARD_GAZE, VisualStyle.PITCH_BLACK, true, 1)),
            species("donkey", "Donkey?", Rarity.RARE, 0,
                    b("phantom_chest", "Phantom Chest", "Looks toward its own empty flank as though a missing chest made a sound.", 1, BehaviorKind.EMPTY_FOCUS, VisualStyle.NORMAL, false, 7),
                    b("empty_lead", "Empty Lead", "Approaches a short-lived point where no fence knot or lead exists.", 2, BehaviorKind.EMPTY_APPROACH, VisualStyle.NORMAL, false, 6),
                    b("late_hoof", "Late Hoof", "Leaves one delayed hoof trace at a position already crossed.", 2, BehaviorKind.DELAYED_TRACE, VisualStyle.WIDE, false, 5),
                    b("returned_trail", "Returned Trail", "Retraces a few blocks of an old idle path, then stops.", 3, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.ASHEN, false, 4),
                    b("burden_watch", "Burden Watch", "Copies the player's gaze while free of rider, lead and panic.", 4, BehaviorKind.MIRROR_PLAYER, VisualStyle.NARROW, false, 3)),
            species("elder_guardian", "Elder Guardian?", Rarity.VERY_RARE, 3,
                    b("false_pulse", "False Pulse", "Focuses on empty water as though a distant pulse had returned from it.", 3, BehaviorKind.EMPTY_FOCUS, VisualStyle.NORMAL, false, 7),
                    b("wrong_chamber", "Wrong Chamber", "Approaches an unoccupied point of its chamber only while targetless.", 3, BehaviorKind.EMPTY_APPROACH, VisualStyle.WIDE, false, 6),
                    b("delayed_spines", "Delayed Spines", "Leaves one delayed water trace behind a harmless turn.", 3, BehaviorKind.DELAYED_TRACE, VisualStyle.ASHEN, false, 5),
                    b("monument_memory", "Monument Memory", "Returns once to a previous guard point without acquiring a target.", 3, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.PALE, false, 4),
                    b("ancient_stillness", "Ancient Stillness", "Becomes briefly still under observation, never during a real beam or attack.", 4, BehaviorKind.WATCHED_STILLNESS, VisualStyle.TALL, false, 3)),
            species("mooshroom", "Mooshroom?", Rarity.RARE, 0,
                    b("wrong_mycelium", "Wrong Mycelium", "Studies ordinary ground as though it were a missing patch of mycelium.", 1, BehaviorKind.EMPTY_FOCUS, VisualStyle.NORMAL, false, 7),
                    b("spore_delay", "Spore Delay", "Leaves a delayed block trace where it stood several moments ago.", 2, BehaviorKind.DELAYED_TRACE, VisualStyle.NORMAL, false, 6),
                    b("empty_ring", "Empty Ring", "Walks a short circle around a patch containing no mushroom.", 2, BehaviorKind.CIRCLE_EMPTY, VisualStyle.WIDE, false, 5),
                    b("remembered_field", "Remembered Field", "Returns briefly to an old grazing point without changing blocks.", 3, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.PALE, false, 4),
                    b("black_mycelium", "Black Mycelium", "A completely black, completely silent Mooshroom that stops when verified directly.", 4, BehaviorKind.WATCHED_STILLNESS, VisualStyle.PITCH_BLACK, true, 1)),
            species("mule", "Mule?", Rarity.RARE, 0,
                    b("empty_pack", "Empty Pack", "Turns toward an empty side as if cargo shifted in a chest it does not carry.", 1, BehaviorKind.EMPTY_FOCUS, VisualStyle.NORMAL, false, 7),
                    b("wrong_trail", "Wrong Trail", "Approaches an empty trail branch and abandons it after a few steps.", 2, BehaviorKind.EMPTY_APPROACH, VisualStyle.NORMAL, false, 6),
                    b("late_hoof", "Late Hoof", "Leaves a delayed hoof trace behind its real movement.", 2, BehaviorKind.DELAYED_TRACE, VisualStyle.WIDE, false, 5),
                    b("returned_hitch", "Returned Hitch", "Returns to an earlier idle position without creating a lead.", 3, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.ASHEN, false, 4),
                    b("burdened_stillness", "Burdened Stillness", "Pauses in an irregular rhythm only while free and unthreatened.", 4, BehaviorKind.STAGGERED_PAUSE, VisualStyle.NARROW, false, 3)),
            species("ocelot", "Ocelot?", Rarity.RARE, 0,
                    b("blindside_stalk", "Blindside Stalk", "Tracks a player from the side only while that player looks elsewhere.", 1, BehaviorKind.REARWARD_GAZE, VisualStyle.NORMAL, false, 7),
                    b("empty_pounce", "Empty Pounce", "Approaches an empty grass point and stops before any pounce.", 2, BehaviorKind.EMPTY_APPROACH, VisualStyle.NORMAL, false, 6),
                    b("wrong_trust", "Wrong Trust", "Mirrors a nearby player's gaze without changing trust or temptation.", 2, BehaviorKind.MIRROR_PLAYER, VisualStyle.WIDE, false, 5),
                    b("repeated_track", "Repeated Track", "Returns once to an earlier stalking position after leaving it.", 3, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.MIRRORED, false, 4),
                    b("dark_brush", "Dark Brush", "A completely black, completely silent Ocelot that freezes when caught looking.", 4, BehaviorKind.WATCHED_STILLNESS, VisualStyle.PITCH_BLACK, true, 1)),
            species("panda", "Panda?", Rarity.RARE, 0,
                    b("empty_bamboo", "Empty Bamboo", "Studies empty ground as if a dropped bamboo item remained there.", 1, BehaviorKind.EMPTY_FOCUS, VisualStyle.NORMAL, false, 7),
                    b("wrong_roll", "Wrong Roll", "Turns its body away from an idle movement without changing its Vanilla genes.", 2, BehaviorKind.WRONG_FACING, VisualStyle.MIRRORED, false, 6),
                    b("late_tumble", "Late Tumble", "Leaves a delayed block trace behind a harmless movement.", 2, BehaviorKind.DELAYED_TRACE, VisualStyle.WIDE, false, 5),
                    b("remembered_sit", "Remembered Sit", "Returns briefly to an earlier resting place.", 3, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.COMPRESSED, false, 4),
                    b("still_face", "Still Face", "Copies the player's gaze while calm, then resumes its own idle animation.", 4, BehaviorKind.MIRROR_PLAYER, VisualStyle.PALE, false, 3)),
            species("piglin", "Piglin?", Rarity.RARE, 1,
                    b("empty_barter", "Empty Barter", "Offers one empty-handed gesture toward an absent barter partner.", 2, BehaviorKind.FALSE_INTERACTION, VisualStyle.NORMAL, false, 7),
                    b("wrong_gold", "Wrong Gold", "Studies an empty point as though a gold item lay there.", 2, BehaviorKind.EMPTY_FOCUS, VisualStyle.NORMAL, false, 6),
                    b("late_admiration", "Late Admiration", "Leaves a delayed ground trace after abandoning an idle inspection.", 2, BehaviorKind.DELAYED_TRACE, VisualStyle.WIDE, false, 5),
                    b("procession_break", "Procession Break", "Calm nearby Piglins converge briefly, then separate before forming a useful line.", 3, BehaviorKind.GROUP_CONVERGENCE, VisualStyle.ASHEN, false, 4),
                    b("door_listener", "Door Listener", "Keeps looking above and beyond an empty doorway while not angered.", 4, BehaviorKind.LOOK_ABOVE, VisualStyle.NARROW, false, 3)),
            species("polar_bear", "Polar Bear?", Rarity.RARE, 1,
                    b("empty_cub", "Empty Cub", "Watches a point beside itself as though a cub stood there.", 1, BehaviorKind.EMPTY_FOCUS, VisualStyle.NORMAL, false, 7),
                    b("wrong_track", "Wrong Track", "Approaches a short empty snow trail only while calm.", 2, BehaviorKind.EMPTY_APPROACH, VisualStyle.NORMAL, false, 6),
                    b("late_print", "Late Print", "Leaves one delayed snow or ice trace behind its path.", 2, BehaviorKind.DELAYED_TRACE, VisualStyle.WIDE, false, 5),
                    b("ice_boundary", "Ice Boundary", "Returns to an earlier position at the edge of water or ice.", 3, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.PALE, false, 4),
                    b("still_pursuit", "Still Pursuit", "Freezes only while calm and watched; cub defense and aggression override it.", 4, BehaviorKind.WATCHED_STILLNESS, VisualStyle.TALL, false, 3)),
            species("pufferfish", "Pufferfish?", Rarity.RARE, 0,
                    b("empty_threat", "Empty Threat", "Faces a point with no threat while its real inflation and poison remain Vanilla.", 1, BehaviorKind.EMPTY_FOCUS, VisualStyle.NORMAL, false, 7),
                    b("wrong_depth", "Wrong Depth", "Approaches an empty water level and abandons it before contact.", 2, BehaviorKind.EMPTY_APPROACH, VisualStyle.NORMAL, false, 6),
                    b("late_bubble", "Late Bubble", "Leaves a delayed water trace behind a harmless turn.", 2, BehaviorKind.DELAYED_TRACE, VisualStyle.WIDE, false, 5),
                    b("repeated_circle", "Repeated Circle", "Circles a point containing no predator or player.", 3, BehaviorKind.CIRCLE_EMPTY, VisualStyle.MIRRORED, false, 4),
                    b("watched_spines", "Watched Spines", "Becomes still under direct view but never suppresses real inflation.", 4, BehaviorKind.WATCHED_STILLNESS, VisualStyle.ASHEN, false, 3)),
            species("skeleton_horse", "Skeleton Horse?", Rarity.VERY_RARE, 1,
                    b("empty_rider", "Empty Rider", "Turns toward its back as though an absent rider shifted position.", 2, BehaviorKind.EMPTY_FOCUS, VisualStyle.NORMAL, false, 7),
                    b("late_hoof", "Late Hoof", "Leaves one delayed hoof trace behind a movement.", 2, BehaviorKind.DELAYED_TRACE, VisualStyle.NORMAL, false, 6),
                    b("wrong_herd", "Wrong Herd", "Approaches an unoccupied place between nearby horses.", 2, BehaviorKind.GROUP_CONVERGENCE, VisualStyle.WIDE, false, 5),
                    b("returned_grave", "Returned Grave", "Returns briefly to an old idle point after the player looks away.", 3, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.ASHEN, false, 4),
                    b("still_saddle", "Still Saddle", "Copies a player's gaze while riderless and out of combat.", 4, BehaviorKind.MIRROR_PLAYER, VisualStyle.NARROW, false, 3)),
            species("snow_golem", "Snow Golem?", Rarity.RARE, 0,
                    b("wrong_trail", "Wrong Trail", "Studies a place where its snow trail should continue but does not.", 1, BehaviorKind.EMPTY_FOCUS, VisualStyle.NORMAL, false, 7),
                    b("empty_throw", "Empty Throw", "Makes one harmless throwing gesture while no hostile target exists.", 2, BehaviorKind.FALSE_INTERACTION, VisualStyle.NORMAL, false, 6),
                    b("late_snow", "Late Snow", "Leaves a delayed block trace after it has moved away.", 2, BehaviorKind.DELAYED_TRACE, VisualStyle.WIDE, false, 5),
                    b("meltless_pause", "Meltless Pause", "Stops under observation for a few beats without changing heat damage or snow placement.", 3, BehaviorKind.WATCHED_STILLNESS, VisualStyle.PALE, false, 4),
                    b("soot", "Soot", "A completely black, completely silent Snow Golem whose harmless projectiles remain visible.", 4, BehaviorKind.REARWARD_GAZE, VisualStyle.PITCH_BLACK, true, 1)),
            species("strider", "Strider?", Rarity.RARE, 0,
                    b("empty_saddle", "Empty Saddle", "Turns toward its back as though an absent rider moved.", 1, BehaviorKind.EMPTY_FOCUS, VisualStyle.NORMAL, false, 7),
                    b("wrong_warmth", "Wrong Warmth", "Approaches an empty point over lava as if it were warmer.", 2, BehaviorKind.EMPTY_APPROACH, VisualStyle.NORMAL, false, 6),
                    b("late_step", "Late Step", "Leaves a delayed lava-side trace behind its movement.", 2, BehaviorKind.DELAYED_TRACE, VisualStyle.TALL, false, 5),
                    b("shore_watch", "Shore Watch", "Looks above an empty shoreline point and waits.", 3, BehaviorKind.LOOK_ABOVE, VisualStyle.WIDE, false, 4),
                    b("returned_path", "Returned Path", "Returns briefly to a recent safe lava position while unridden.", 4, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.ASHEN, false, 3)),
            species("tadpole", "Tadpole?", Rarity.RARE, 0,
                    b("wrong_school", "Wrong School", "Moves toward an empty place in a nearby school.", 1, BehaviorKind.GROUP_CONVERGENCE, VisualStyle.NORMAL, false, 7),
                    b("empty_surface", "Empty Surface", "Looks toward a surface point where nothing appeared.", 2, BehaviorKind.LOOK_ABOVE, VisualStyle.NORMAL, false, 6),
                    b("late_turn", "Late Turn", "Leaves a delayed water trace after changing direction.", 2, BehaviorKind.DELAYED_TRACE, VisualStyle.WIDE, false, 5),
                    b("repeated_circle", "Repeated Circle", "Circles a fixed empty point and then rejoins ordinary movement.", 3, BehaviorKind.CIRCLE_EMPTY, VisualStyle.MIRRORED, false, 4),
                    b("ink_tadpole", "Ink Tadpole", "A completely black, completely silent Tadpole that stops when directly checked.", 4, BehaviorKind.WATCHED_STILLNESS, VisualStyle.PITCH_BLACK, true, 1)),
            species("trader_llama", "Trader Llama?", Rarity.VERY_RARE, 0,
                    b("empty_caravan", "Empty Caravan", "Looks toward an absent place in its trader's caravan.", 1, BehaviorKind.EMPTY_FOCUS, VisualStyle.NORMAL, false, 7),
                    b("wrong_trader", "Wrong Trader", "Tracks a nearby player only when that player is not looking.", 2, BehaviorKind.REARWARD_GAZE, VisualStyle.NORMAL, false, 6),
                    b("late_spit", "Late Spit", "Makes one harmless interaction gesture after danger has passed.", 2, BehaviorKind.FALSE_INTERACTION, VisualStyle.WIDE, false, 5),
                    b("returned_lead", "Returned Lead", "Returns to an earlier caravan position without changing its real lead.", 3, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.ASHEN, false, 4),
                    b("blanket_watch", "Blanket Watch", "Mirrors the player's gaze while its trader and lead remain authoritative.", 4, BehaviorKind.MIRROR_PLAYER, VisualStyle.NARROW, false, 3)),
            species("tropical_fish", "Tropical Fish?", Rarity.RARE, 0,
                    b("wrong_school", "Wrong School", "Converges toward an empty place among nearby fish.", 1, BehaviorKind.GROUP_CONVERGENCE, VisualStyle.NORMAL, false, 7),
                    b("late_turn", "Late Turn", "Leaves a delayed water trace behind a sharp turn.", 2, BehaviorKind.DELAYED_TRACE, VisualStyle.NORMAL, false, 6),
                    b("empty_reef", "Empty Reef", "Studies an empty water block as if coral remained there.", 2, BehaviorKind.EMPTY_FOCUS, VisualStyle.WIDE, false, 5),
                    b("returned_pattern", "Returned Pattern", "Returns to a previous point in the school after leaving it.", 3, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.MIRRORED, false, 4),
                    b("ink_fish", "Ink Fish", "A completely black, completely silent Tropical Fish that holds still when inspected.", 4, BehaviorKind.WATCHED_STILLNESS, VisualStyle.PITCH_BLACK, true, 1)),
            species("warden", "Warden?", Rarity.VERY_RARE, 3,
                    b("empty_vibration", "Empty Vibration", "Turns toward an empty point only while it has no anger target.", 3, BehaviorKind.EMPTY_FOCUS, VisualStyle.NORMAL, false, 7),
                    b("late_listening", "Late Listening", "Leaves a delayed ground trace after moving away from a calm inspection.", 3, BehaviorKind.DELAYED_TRACE, VisualStyle.NORMAL, false, 6),
                    b("wrong_heartbeat", "Wrong Heartbeat", "Faces away from its calm route for one brief interval without muting any cue.", 3, BehaviorKind.WRONG_FACING, VisualStyle.WIDE, false, 5),
                    b("still_search", "Still Search", "Pauses only while targetless and directly watched; anger always overrides it.", 3, BehaviorKind.WATCHED_STILLNESS, VisualStyle.ASHEN, false, 4),
                    b("returned_footprint", "Returned Footprint", "Returns to an earlier calm search position and then abandons it.", 4, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.NARROW, false, 3)),
            species("witch", "Witch?", Rarity.RARE, 2,
                    b("empty_drink", "Empty Drink", "Makes one item-use gesture toward an empty hand while no target exists.", 2, BehaviorKind.FALSE_INTERACTION, VisualStyle.NORMAL, false, 7),
                    b("wrong_ingredient", "Wrong Ingredient", "Studies an empty point as though a potion ingredient lay there.", 2, BehaviorKind.EMPTY_FOCUS, VisualStyle.NORMAL, false, 6),
                    b("late_bottle", "Late Bottle", "Leaves a delayed ground trace after moving away from an idle brew.", 2, BehaviorKind.DELAYED_TRACE, VisualStyle.WIDE, false, 5),
                    b("returned_hut", "Returned Hut", "Returns briefly to a previous idle position without changing patrol logic.", 3, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.ASHEN, false, 4),
                    b("still_brew", "Still Brew", "Copies the player's gaze only while targetless and not drinking a real potion.", 4, BehaviorKind.MIRROR_PLAYER, VisualStyle.NARROW, false, 3)),
            species("zoglin", "Zoglin?", Rarity.RARE, 2,
                    b("empty_charge", "Empty Charge", "Approaches an empty point but never turns it into a free attack.", 2, BehaviorKind.EMPTY_APPROACH, VisualStyle.NORMAL, false, 7),
                    b("wrong_herd", "Wrong Herd", "Calm nearby Zoglins orient around one empty place.", 2, BehaviorKind.GROUP_CONVERGENCE, VisualStyle.NORMAL, false, 6),
                    b("late_snort", "Late Snort", "Leaves a delayed block trace behind its movement.", 2, BehaviorKind.DELAYED_TRACE, VisualStyle.WIDE, false, 5),
                    b("returned_path", "Returned Path", "Retraces a recent idle path only while no target exists.", 3, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.ASHEN, false, 4),
                    b("still_rage", "Still Rage", "Becomes still only outside combat; any real target cancels it immediately.", 4, BehaviorKind.WATCHED_STILLNESS, VisualStyle.TALL, false, 3)),
            species("zombie_horse", "Zombie Horse?", Rarity.VERY_RARE, 0,
                    b("empty_rider", "Empty Rider", "Turns toward its back as though a rider were present.", 2, BehaviorKind.EMPTY_FOCUS, VisualStyle.NORMAL, false, 7),
                    b("wrong_grave", "Wrong Grave", "Approaches a short-lived empty point and then stops.", 2, BehaviorKind.EMPTY_APPROACH, VisualStyle.NORMAL, false, 6),
                    b("late_hoof", "Late Hoof", "Leaves one delayed hoof trace behind a movement.", 2, BehaviorKind.DELAYED_TRACE, VisualStyle.WIDE, false, 5),
                    b("returned_herd", "Returned Herd", "Returns briefly to an earlier idle position.", 3, BehaviorKind.RETURN_TO_MEMORY, VisualStyle.ASHEN, false, 4),
                    b("grave_witness", "Grave Witness", "Tracks a nearby player only while that player looks away.", 4, BehaviorKind.REARWARD_GAZE, VisualStyle.NARROW, false, 3)));

    private static final List<Variant> VARIANTS;
    private static final Map<String, Variant> BY_ID;
    private static final Map<String, Species> BY_TYPE;

    static {
        List<Variant> flattened = new ArrayList<>();
        Map<String, Variant> byId = new LinkedHashMap<>();
        Map<String, Species> byType = new LinkedHashMap<>();
        for (Species species : SPECIES) {
            if (byType.put(species.typeKey(), species) != null) {
                throw new IllegalStateException("Duplicate approved variant species: " + species.typeKey());
            }
            if (species.variants().size() != 5) {
                throw new IllegalStateException("Every approved species must have five variants: " + species.typeKey());
            }
            for (int index = 0; index < species.variants().size(); index++) {
                Variant variant = species.variants().get(index);
                if (variant.index() != index + 1) {
                    throw new IllegalStateException("Non-contiguous variant index for " + species.typeKey());
                }
                if (byId.put(variant.id(), variant) != null) {
                    throw new IllegalStateException("Duplicate approved variant id: " + variant.id());
                }
                flattened.add(variant);
            }
        }
        VARIANTS = List.copyOf(flattened);
        BY_ID = Map.copyOf(byId);
        BY_TYPE = Map.copyOf(byType);
    }

    private ApprovedVanillaVariantCatalog() {
    }

    public static List<Species> species() {
        return SPECIES;
    }

    public static List<Variant> variants() {
        return VARIANTS;
    }

    public static Variant byId(String id) {
        return id == null ? null : BY_ID.get(id.trim().toLowerCase(Locale.ROOT));
    }

    /** Retained for old callers; returns the stable first variant of the species. */
    public static Variant byTypeKey(String typeKey) {
        Species species = speciesByTypeKey(typeKey);
        return species == null ? null : species.variants().getFirst();
    }

    public static Species speciesByTypeKey(String typeKey) {
        return typeKey == null ? null : BY_TYPE.get(typeKey.trim().toLowerCase(Locale.ROOT));
    }

    public static List<Variant> variantsForTypeKey(String typeKey) {
        Species species = speciesByTypeKey(typeKey);
        return species == null ? List.of() : species.variants();
    }

    /** Per-species natural-spawn chance; adding variants never multiplies anomaly frequency. */
    public static double naturalChanceForType(String typeKey, int phase) {
        Variant primary = byTypeKey(typeKey);
        return naturalChance(primary, phase);
    }

    /** Retains the historical rarity table for metadata and compatibility tests. */
    public static double naturalChance(Variant variant, int phase) {
        int clampedPhase = Math.max(1, Math.min(4, phase));
        if (variant == null || clampedPhase < variant.minimumPhase()) {
            return 0.0D;
        }
        return switch (variant.rarity()) {
            case UNCOMMON -> switch (clampedPhase) {
                case 1 -> 0.006D;
                case 2 -> 0.014D;
                case 3 -> 0.028D;
                default -> 0.045D;
            };
            case RARE -> switch (clampedPhase) {
                case 1 -> 0.0025D;
                case 2 -> 0.008D;
                case 3 -> 0.018D;
                default -> 0.035D;
            };
            case VERY_RARE -> switch (clampedPhase) {
                case 1 -> 0.0006D;
                case 2 -> 0.0025D;
                case 3 -> 0.006D;
                default -> 0.012D;
            };
        };
    }

    /** Deterministic weighted selector shared by natural runtime and statistical tests. */
    public static Variant selectVariant(String typeKey, int phase, long ticket) {
        List<Variant> eligible = variantsForTypeKey(typeKey).stream()
                .filter(variant -> phase >= variant.minimumPhase())
                .toList();
        int totalWeight = eligible.stream().mapToInt(Variant::naturalWeight).sum();
        if (totalWeight <= 0) {
            return null;
        }
        int roll = (int) Math.floorMod(ticket, totalWeight);
        for (Variant variant : eligible) {
            roll -= variant.naturalWeight();
            if (roll < 0) {
                return variant;
            }
        }
        throw new IllegalStateException("Weighted approved variant selection exhausted unexpectedly");
    }

    public static List<Variant> pitchBlackSilentVariants() {
        return VARIANTS.stream()
                .filter(variant -> variant.visualStyle() == VisualStyle.PITCH_BLACK && variant.silent())
                .toList();
    }

    /**
     * Presentation-only proportions that never extend beyond the authoritative Vanilla hitbox.
     * A style may make the hitbox slightly more generous than the silhouette, but never makes a
     * visible limb or head impossible to hit.
     */
    public static VisualScale visualScale(VisualStyle style) {
        return switch (style) {
            case TALL -> new VisualScale(0.86F, 1.0F, 0.86F);
            case NARROW -> new VisualScale(0.78F, 1.0F, 0.78F);
            case WIDE -> new VisualScale(1.0F, 0.88F, 1.0F);
            case COMPRESSED -> new VisualScale(0.98F, 0.76F, 0.98F);
            case MIRRORED -> new VisualScale(0.90F, 1.0F, 0.98F);
            default -> VisualScale.IDENTITY;
        };
    }

    private static Species species(
            String typeKey,
            String displayName,
            Rarity rarity,
            int danger,
            Blueprint... blueprints) {
        List<Variant> variants = new ArrayList<>(blueprints.length);
        for (int index = 0; index < blueprints.length; index++) {
            Blueprint blueprint = blueprints[index];
            variants.add(new Variant(
                    typeKey + "_" + blueprint.idSuffix(),
                    typeKey,
                    displayName,
                    blueprint.label(),
                    blueprint.description(),
                    index + 1,
                    blueprint.minimumPhase(),
                    rarity,
                    danger,
                    blueprint.behaviorKind(),
                    blueprint.visualStyle(),
                    blueprint.silent(),
                    blueprint.naturalWeight()));
        }
        return new Species(typeKey, displayName, rarity, danger, List.copyOf(variants));
    }

    private static Blueprint b(
            String idSuffix,
            String label,
            String description,
            int minimumPhase,
            BehaviorKind behaviorKind,
            VisualStyle visualStyle,
            boolean silent,
            int naturalWeight) {
        return new Blueprint(
                idSuffix, label, description, minimumPhase, behaviorKind, visualStyle, silent, naturalWeight);
    }

    public enum Rarity {
        UNCOMMON,
        RARE,
        VERY_RARE
    }

    public enum BehaviorKind {
        SPECIALIZED,
        EMPTY_FOCUS,
        EMPTY_APPROACH,
        REARWARD_GAZE,
        WATCHED_STILLNESS,
        RETURN_TO_MEMORY,
        DELAYED_TRACE,
        WRONG_FACING,
        CIRCLE_EMPTY,
        LOOK_ABOVE,
        MIRROR_PLAYER,
        STAGGERED_PAUSE,
        GROUP_CONVERGENCE,
        FALSE_INTERACTION
    }

    public enum VisualStyle {
        NORMAL,
        PALE,
        ASHEN,
        TALL,
        NARROW,
        WIDE,
        COMPRESSED,
        MIRRORED,
        PITCH_BLACK
    }

    public record Species(
            String typeKey,
            String displayName,
            Rarity rarity,
            int danger,
            List<Variant> variants) {

        public Species {
            variants = List.copyOf(variants);
        }
    }

    public record Variant(
            String id,
            String typeKey,
            String displayName,
            String behavior,
            String description,
            int index,
            int minimumPhase,
            Rarity rarity,
            int danger,
            BehaviorKind behaviorKind,
            VisualStyle visualStyle,
            boolean silent,
            int naturalWeight) {
    }

    public record VisualScale(float x, float y, float z) {
        public static final VisualScale IDENTITY = new VisualScale(1.0F, 1.0F, 1.0F);
    }

    private record Blueprint(
            String idSuffix,
            String label,
            String description,
            int minimumPhase,
            BehaviorKind behaviorKind,
            VisualStyle visualStyle,
            boolean silent,
            int naturalWeight) {
    }
}
