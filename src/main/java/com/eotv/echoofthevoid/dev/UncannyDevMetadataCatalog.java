package com.eotv.echoofthevoid.dev;

import com.eotv.echoofthevoid.event.paranoia.ParanoiaEventCatalog;
import com.eotv.echoofthevoid.event.paranoia.ParanoiaEventDescriptor;
import com.eotv.echoofthevoid.event.paranoia.ParanoiaEventSeverity;
import com.eotv.echoofthevoid.event.passive.ApprovedVanillaVariantCatalog;
import com.eotv.echoofthevoid.event.special.ApprovedSpecialCatalog;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Read-only QA metadata used by the development menu.
 *
 * <p>This class describes shipped behavior; it never participates in natural selection or
 * runtime execution. Defaults are intentionally explicit so every catalog entry remains
 * searchable and diagnosable even before it receives a bespoke description.</p>
 */
public final class UncannyDevMetadataCatalog {
    private static final Set<String> PRIVATE_EVENT_IDS = Set.of(
            "base_replay",
            "flash_red",
            "void_silence",
            "false_fall",
            "false_injury",
            "corrupt_message",
            "hotbar_wrong_count",
            "corrupt_toast",
            "false_recipe_toast",
            "asphyxia",
            "hunter_fog");

    private static final Set<String> BLOCK_MUTATING_EVENT_IDS = Set.of(
            "door_inversion", "phantom_harvest", "living_ore", "misplaced_light");

    private static final Map<String, Text> EVENT_TEXT = Map.ofEntries(
            Map.entry("blackout", text(
                    "Temporarily removes the target player's usable vision and layers a bounded mental cue.",
                    "Requires the event system to be enabled; natural selection starts in phase 3.",
                    "Client cleanup, relog and a second player must be checked.",
                    "Targeted perception; nearby players should not inherit the mental layer.",
                    "Darkness effect, Disc 11 cue and client event state.",
                    "Visual intensity still requires human judgement.")),
            Map.entry("footsteps", text(
                    "Plays one of the vanilla-footstep patterns behind or around the target.",
                    "Needs a valid player and enough surrounding space for a plausible source.",
                    "No entity or block is created; variants have different timing.",
                    "Normally private to the targeted player to create uncertainty.",
                    "Vanilla step sounds; basic, echo, sprint, heavy and ladder patterns.",
                    "Subtlety and perceived direction cannot be unit-tested.")),
            Map.entry("bell", text(
                    "Runs the dangerous Bell event and may create an Uncanny entity wave.",
                    "Natural use starts in phase 2 and is affected by profile and danger.",
                    "A real nearby bell changes the audio route; Tension Builder blocks natural use.",
                    "Physical bell and spawned danger are shared; a bell without a source becomes private mental audio.",
                    "Bell block, entity-wave scheduler and mental sound delivery.",
                    "Wave size, fairness and routing need a two-player test.")),
            Map.entry("asphyxia", text(
                    "Runs one of three bounded breathing/drowning illusions selected by danger.",
                    "Natural use is phase 4; terrain and health guards decide available variants.",
                    "Must not create an arbitrary death or persist after disconnect.",
                    "Personal physiological manifestation; other players should not hear its mental cue.",
                    "Breathing sound, air/effects and terrain checks.",
                    "Each danger level and low-health case must be exercised manually.")),
            Map.entry("door_inversion", text(
                    "Makes a nearby door respond incorrectly; dangerous variants can lock, intrude or cascade.",
                    "Requires a compatible nearby door and phase 3 for natural selection.",
                    "Iron/double doors, drops and protected block data are sensitive.",
                    "The physical door result is shared with nearby players.",
                    "Door blockstates, block drops, sounds and delayed tasks.",
                    "Some variants mutate blocks and require a disposable test world.")),
            Map.entry("phantom_harvest", text(
                    "Corrupts a small crop patch using black, rotten-soil or infestation variants.",
                    "Requires mature compatible crops; natural use is phase 4.",
                    "Can change blocks or spawn mobs; never test beside valuable farms without a copy.",
                    "World mutations and spawned entities are shared.",
                    "Crops, farmland, Silverfish/Spiderling and block drops.",
                    "NBT/drop and chunk reload behavior require manual verification.")),
            Map.entry("living_ore", text(
                    "Arms a mined ore and resolves one of five delayed reactions.",
                    "Requires a compatible ore context; natural use starts in phase 3.",
                    "Tension Builder blocks it; delayed tasks and block state are sensitive.",
                    "The ore and its physical consequences are shared.",
                    "Ore block, particles, effects, knock and delayed scheduler.",
                    "Forced selection does not prove natural mining integration.")),
            Map.entry("orphan_shadow", text(
                    "Renders a real vanilla entity shadow on empty ground without spawning an entity.",
                    "Needs a full lit support block outside every observer's direct gaze.",
                    "Aborts when entity shadows are disabled; there is no substitute overlay.",
                    "Server chooses one shared position; one observer looking at it ends it for everyone.",
                    "Vanilla entity-shadow RenderType and one clientbound presentation payload.",
                    "Shaders, camera modes and two observers still need visual testing.")),
            Map.entry("ghost_breaking", text(
                    "Shows vanilla mining cracks and quiet impacts on an ordinary block without breaking it.",
                    "Needs unobserved stone, dirt, log or planks with no block entity.",
                    "Never targets ores or valuable data-bearing blocks and always clears the break progress.",
                    "Cracks and physical impacts are shared with nearby observers.",
                    "Clientbound block-destruction progress and vanilla block sounds.",
                    "Cleanup on chunk/dimension changes must be checked in game.")),
            Map.entry("cold_furnace", text(
                    "Temporarily presents a genuinely cold furnace with the vanilla lit blockstate.",
                    "Needs an unlit furnace outside direct observation.",
                    "Fuel, inventory, cook progress and server blockstate never change.",
                    "All observers receive the same false blockstate and common restoration.",
                    "Vanilla furnace blockstate packets and a quiet physical crackle.",
                    "Opening, relogging and chunk resync require a client test.")),
            Map.entry("empty_teleport", text(
                    "Plays a sparse Enderman teleport trace without creating or moving an entity.",
                    "Needs a clear visible position around the target.",
                    "The sound is deliberately absent in most variants.",
                    "Particles and any physical sound are shared around the chosen point.",
                    "Vanilla portal particles and occasional Enderman teleport audio.",
                    "Human judgement is needed to ensure it remains sparse rather than decorative.")),
            Map.entry("false_animal_hurt", text(
                    "Makes one calm adult animal appear hurt and briefly face an empty point without damage.",
                    "Needs a healthy untamed adult outside breeding and hostile contexts.",
                    "Health and server AI remain unchanged; the same animal cannot be reused for 30 minutes.",
                    "Nearby observers see the same animation and target point.",
                    "Vanilla entity event packet and temporary look control.",
                    "Pet exclusions and AI recovery require live verification.")),
            Map.entry("stolen_pose", text(
                    "Changes one visible armor-stand limb only after the stand leaves every observer's view.",
                    "Needs an ordinary visible armor stand with a safe client snapshot.",
                    "Invisible, marker and no-gravity stands are excluded; equipment and server pose are untouched.",
                    "All observers receive one common pose and one common restoration.",
                    "ArmorStand pose payload and client snapshot restoration.",
                    "Interaction, resource reload and modded poses remain compatibility risks.")),
            Map.entry("fishing_tug", text(
                    "Visually pulls a real fishing hook sideways for less than a second.",
                    "The target must have a hook in water with no entity caught.",
                    "Loot, bite timing, rod durability and authoritative hook motion remain vanilla.",
                    "Nearby observers receive the same visual impulse and splashes.",
                    "FishingHook presentation payload and vanilla water particles.",
                    "A live catch during the effect is the essential race-condition test.")),
            Map.entry("leaf_reply", text(
                    "Makes three intact leaves answer a recent leaf break with a delayed, imperfect rhythm.",
                    "Needs enough nearby leaves and no active special weather.",
                    "No decay, block removal or drop is produced.",
                    "The physical reply is shared around the selected leaves.",
                    "Vanilla leaf particles and block sounds.",
                    "The natural 5–40 second context path needs manual timing.")),
            Map.entry("silent_bell", text(
                    "Animates a real bell without sound, then may answer later from a different position.",
                    "Needs a calm village bell; natural use starts in phase 3.",
                    "Does not call bell ringing logic, vibration, raid or villager memory; a real ring cancels the answer.",
                    "Animation and delayed physical answer are shared.",
                    "Vanilla bell block event, delayed task and bell sound.",
                    "Raid/village side effects and cancellation need a live test.")),
            Map.entry("empty_congregation", text(
                    "Three calm villagers stop in sequence and look at the same empty block of air.",
                    "Needs three adults outside raid, sleep, trade and hostile contexts.",
                    "Never sets NoAI; entering the point or ringing a bell ends the scene.",
                    "The selected villagers and empty focus are common to all observers.",
                    "Villager look/navigation controls and a bounded server task.",
                    "Schedule recovery and two-player interruption need live testing.")),
            Map.entry("empty_lead", text(
                    "Shows a vanilla lead tied to a real fence while its far end has no entity.",
                    "Natural use needs an unobserved fence and a clear line; QA accepts the fence currently aimed at.",
                    "No knot or leashed entity is created; approach and close inspection end the illusion.",
                    "Nearby players receive the same endpoints and disappearance.",
                    "Vanilla leash RenderType and a bounded shared presentation payload.",
                    "Fence geometry, shaders and two viewpoints require visual QA.")),
            Map.entry("borrowed_painting", text(
                    "Temporarily presents an existing painting with another same-sized vanilla motive.",
                    "Needs an unobserved painting with at least one placeable motive of identical dimensions.",
                    "The server variant, hitbox, item drop and saved entity data never change; interaction restores it.",
                    "All nearby observers receive one borrowed motive and common restoration.",
                    "Painting holder payload and client-side snapshot restoration.",
                    "Resource packs and chunk resync need live comparison.")),
            Map.entry("returned_drop", text(
                    "Re-renders the item type from a recent successful pickup at its former ground position.",
                    "Needs an ordinary block pickup from the last three minutes, a loaded empty position and no real matching item.",
                    "The image has no entity, hitbox, pickup or item components and disappears on approach.",
                    "Nearby players see the same non-interactive item image.",
                    "Sanitized ItemStack payload and vanilla ground-item renderer.",
                    "Partial pickups and crowded item piles require manual QA.")),
            Map.entry("ghost_cart", text(
                    "Moves a physical minecart sound along a real, currently unused rail line; an unpowered powered rail may glow briefly.",
                    "Needs at least seven connected loaded rails and no real minecart near the route.",
                    "No entity is spawned, no chunk is loaded and any apparent powered state is restored from the server.",
                    "The same spatialized route and optional rail cue are shared with nearby players.",
                    "Vanilla rail shapes, minecart sound and presentation-only block packets.",
                    "Curves, slopes and long resource-pack sounds need live QA.")),
            Map.entry("misdirected_enchantment", text(
                    "Makes real bookshelves send vanilla enchantment motes toward empty air beside the table.",
                    "Needs an enchanting table with at least one valid power-transmitting shelf.",
                    "Enchanting power, inventories, blocks and the table entity remain untouched.",
                    "Particles and the occasional physical table cue are shared.",
                    "Vanilla enchant particles, shelf validity and a short server task.",
                    "Particle direction and readability require human judgement.")),
            Map.entry("orphan_signal", text(
                    "Lets a recently powered redstone wire appear to relight for roughly one second after its real signal ended.",
                    "Needs a genuine recent wire impulse, no remaining neighbor power and an unobserved segment.",
                    "The server wire stays at power zero; mechanisms and comparator outputs never receive a signal.",
                    "Nearby witnesses receive the same false blockstate and authoritative restoration.",
                    "Bounded redstone context plus presentation-only block update packets.",
                    "Dense machines and resource-pack emissive rendering need live QA.")),
            Map.entry("cauldron_echo", text(
                    "Briefly shows one less liquid layer in a filled layered cauldron.",
                    "Needs an unobserved water, snow or powder-snow cauldron above its minimum level.",
                    "The authoritative level and comparator output never change; interaction restores immediately.",
                    "Nearby observers receive the same false blockstate and restoration.",
                    "Vanilla blockstate packet and a quiet physical splash.",
                    "Comparator visibility and simultaneous bucket use need live QA.")),
            Map.entry("map_intruder", text(
                    "Adds a neutral marker to an already explored pixel of the map currently held by the targeted player; it moves one pixel and vanishes when lowered.",
                    "Needs a filled map in either hand and an explored interior pixel; natural use starts in phase 3.",
                    "No server map data, item component, banner or saved decoration is changed.",
                    "Intentionally private: only the selected player receives this discordant evidence.",
                    "Temporary client-map decoration with a server-selected map id, pixel and duration.",
                    "Both hands, map resync and resource-pack marker readability need live QA.")),
            Map.entry("empty_wake", text(
                    "Runs a short line of vanilla splashes across a real water surface without a swimmer.",
                    "Needs at least four consecutive exposed water blocks near the player.",
                    "No entity, fluid update, collision or fishing state is created.",
                    "The server emits one shared physical wake for nearby players.",
                    "Vanilla splash particles and an optional quiet bobber splash.",
                    "Flowing water, boats and visibility at distance require manual QA.")),
            Map.entry("countercurrent_column", text(
                    "Briefly renders bubble-column particles travelling opposite to the column's real direction.",
                    "Needs a real bubble column with no player about to enter it; natural use starts in phase 2.",
                    "Movement, oxygen, drag direction and blocks remain authoritative and unchanged.",
                    "The contradictory particles are emitted once by the server for nearby witnesses.",
                    "Vanilla upward-bubble and downward-current particles with a bounded task.",
                    "Readability in tall columns and exact stop-before-entry need live QA.")),
            Map.entry("false_sculk_vibration", text(
                    "Shows one vanilla vibration converging on an isolated inactive sculk sensor, which appears active without producing power.",
                    "Needs an unobserved sensor away from wardens, shriekers and nearby redstone machinery; phase 3.",
                    "No GameEvent is emitted and the authoritative sensor stays inactive at power zero.",
                    "The vibration, click and apparent sensor state are common to nearby witnesses.",
                    "Vanilla vibration particle plus presentation-only blockstate restoration.",
                    "Ancient City boundaries and calibrated sensors need manual QA.")),
            Map.entry("watching_arrow", text(
                    "Turns one old arrow embedded in a block slightly toward the player while nobody is looking, then restores it on approach.",
                    "Needs an arrow motionless for at least five seconds, outside combat and outside every nearby player's view; phase 2.",
                    "The real projectile position, ownership, pickup state and lifetime never change.",
                    "Every nearby witness receives the same temporary rotation.",
                    "Presentation-only entity rotation payload applied to the existing vanilla arrow.",
                    "Embedded-arrow orientation, despawn and simultaneous observers require live QA.")),
            Map.entry("suspended_fall", text(
                    "Shows sand or gravel beginning to fall, hanging for under a second and returning to its exact block.",
                    "Needs a loaded, unobserved gravity block on partial stable support with nobody beneath it; phase 3.",
                    "The authoritative block never moves and no collision, suffocation or falling-block entity is created.",
                    "Nearby witnesses see the same bounded presentation and authoritative restoration.",
                    "Temporary block concealment plus a client-rendered vanilla falling-block model.",
                    "Resource packs, partial supports and mid-animation chunk unload require live QA.")),
            Map.entry("beacon_fragment", text(
                    "Shows a short, genuine beacon-beam segment above a distant roof or opening, without a beacon or pyramid.",
                    "Needs a loaded clear sky column away from real beacons; natural use starts in phase 3 and succeeds at most once per world campaign.",
                    "No block, block entity, effect, light level or chunk ticket is created; devmenu runs do not consume the campaign occurrence.",
                    "Every player close enough to the site sees the same beam, which vanishes when its apparent origin is clearly checked.",
                    "Minecraft's BeaconRenderer and beam texture, driven by a bounded shared payload.",
                    "Long-distance visibility, fog, roofs and resource packs require live QA.")),
            Map.entry("stray_experience", text(
                    "After a completed hostile fight, two or three apparent experience orbs converge on an empty point and go out.",
                    "Needs a recent player kill, no remaining hostile or real experience orb nearby, and the player still close to the fight; phase 2.",
                    "No entity is inserted into the level and no experience, score, Mending repair or pickup delay can change.",
                    "Nearby witnesses see the same orbs and empty destination.",
                    "Short-lived client ExperienceOrb instances rendered directly with Minecraft's entity renderer.",
                    "Projectile kills, modded hostiles, Mending and multiplayer kill attribution require live QA.")),
            Map.entry("extra_in_the_herd", text(
                    "Adds one common adult animal image beside a real herd; it follows a real member and vanishes when counted up close or when the herd separates.",
                    "Needs at least three nearby adult cows, pigs, sheep or chickens that are unnamed, not breeding, not leashed and not mounted; phase 2.",
                    "The copy is never inserted into the level and therefore has no AI, hitbox, drops, breeding, pathfinding or interaction.",
                    "All nearby witnesses see the same species, anchor and offset.",
                    "A client-only Vanilla entity instance rendered directly beside an authoritative herd member.",
                    "Animation cadence, fences, dense pens and resource-pack variants require live QA.")),
            Map.entry("lava_wake", text(
                    "Moves Strider-like lava footsteps and particles across a real Nether lava surface, with one change of direction.",
                    "Needs a loaded surface route, no real Strider nearby and a safe distance from the player.",
                    "No entity, fluid update, collision or chunk load is created.",
                    "The physical sound path and particles are shared.",
                    "Vanilla lava particles and Strider lava-step sounds.",
                    "Large lava seas, resource-pack sounds and distant occlusion need live QA.")),
            Map.entry("false_lid", text(
                    "Slightly opens a real single chest or shulker box, then closes it when checked.",
                    "Needs a closed, unobserved container with no player close enough to be using it; phase 2.",
                    "No menu, lock, inventory, opener count or comparator output changes on the server.",
                    "Nearby witnesses receive the same vanilla block-entity animation.",
                    "Presentation-only block-event packets followed by an explicit close.",
                    "Double chests are excluded; shulker collision and resource packs need live QA.")));

    private static final Map<String, Text> SPECIAL_TEXT = Map.ofEntries(
            Map.entry("watcher", text("Observes from long range and flees after sustained recognition.", "Natural: outdoor Overworld night from phase 2, plus the first-night guarantee; never while the target sleeps.", "Invulnerable encounter; no boats, water traps or sleeping gaze progress.", "One server entity shared by all nearby players.", "Watcher renderer, awake gaze timer and observation advancement.", "Night/sky placement remains context-sensitive.")),
            Map.entry("shadow", text("Destroys nearby light sources until observed, then flees and sinks.", "Natural: dark context from phase 3 in the current pool.", "Real light drops must remain correct; no boats or water traps.", "Shared physical entity and block effects.", "Light scan, shard drop and silhouette renderer.", "Block protection compatibility is unresolved.")),
            Map.entry("hurler", text("Keeps an uneasy distance, then may attack after direct observation.", "Natural: phase 3+, or an exceptionally rare phase-2 release after recent natural underground mining.", "Attack is telegraphed; water/boat escape remains valid; phase 2 has low weight and a 20-minute per-key cooldown.", "Shared server entity with spatial audio.", "Three scream resources, gaze state, mining context and attack window.", "Long scream files and perceived volume need listening tests.")),
            Map.entry("stalker", text("Stalker alias: copies the player's movement and weapon pressure before committing.", "Natural Stalker requires phase 3 and danger above 1.", "Must preserve hiding distance and a readable combat transition.", "Shared server entity; target choice matters in multiplayer.", "Stalker AI, copied equipment pressure and silhouette renderer.", "Alias is historical; displayed behavior is Stalker.")),
            Map.entry("pulse", text("Pulse alias: an invisible pressure source located by heartbeat and Wither aura.", "Natural: phase 2+ with a low special weight.", "The heartbeat is spatial and shared; no arbitrary invisible burst damage.", "One shared server entity heard according to distance.", "Heartbeat, particles, aura and Pulse drops.", "The historical menu label differs from the internal Pulse name.")),
            Map.entry("terror", text("Debug-only gaze encounter that constrains camera and closes distance without normal damage.", "No natural pool entry; explicit QA spawn only.", "Must release camera, audio and state on disconnect or loss of target.", "Targeted state with a real shared entity; the five-second lock sound is private.", "Gaze timer, camera effect and one continuous mental sound matched to the lock duration.", "Fear impact, mix level and motion-sickness impact require human testing.")),
            Map.entry("usher", text("Guides toward a lore marker, then usually disappears when understood.", "Natural: phase 3 in current code with a nearby lore marker.", "Marker fallback and the known weight/cooldown conflict are unresolved.", "Shared entity anchored to one target route.", "Lore markers, compass fallback and rare attack branch.", "Catalog documentation historically disagrees about its numbers.")),
            Map.entry("keeper", text("Occupies the outside of a base and manipulates a nearby container role.", "Natural: phase 3, outside near a known base and container.", "Container contents must never be changed by the encounter.", "Shared entity; observation outcome is common.", "Base memory, container search and silhouette renderer.", "Private audio delivery still needs a dedicated audit.")),
            Map.entry("tenant", text("Appears inside a base after a long absence and withdraws after discovery.", "Natural: phase 3 with base memory and at least 180 seconds away.", "Never traps a player inside; bounded linger and water exit.", "Shared entity tied to a world-level base context.", "Door/interior checks and absence memory.", "Restart behavior of active timers is not persisted.")),
            Map.entry("follower", text("Tracks the player for several minutes, approaching only when unobserved.", "Natural: phase 2+ with a long explicit cooldown.", "Observation, distance, water and bounded attack provide exits.", "Shared entity with one primary target.", "Long-lived follow state, footsteps and sink cleanup.", "Long duration, unload and remaining loud audio need manual validation.")),
            Map.entry("knocker", text("Announces itself at a real door and reacts differently to open, wood and iron doors.", "Natural: phase 2 near a base, under cover and beside a door.", "Door context is mandatory and attack chance is danger-bounded.", "Shared physical entity and door audio.", "Door scanner, knocks, attack branch and shard drop.", "Door changes during the sequence are a race-condition risk.")));

    private UncannyDevMetadataCatalog() {
    }

    public static Info describe(UncannyDevCatalog.Entry entry) {
        if (entry == null) {
            return Info.unknown();
        }
        return switch (entry.category()) {
            case EVENTS -> describeEvent(entry);
            case ENTITIES -> describeEntity(entry);
            case WEATHER -> describeWeather(entry);
            case STRUCTURES -> describeStructure(entry);
            case AUDIO -> describeAudio(entry);
            case TOOLS -> describeTool(entry);
            case ALL -> Info.unknown();
        };
    }

    private static Info describeEvent(UncannyDevCatalog.Entry entry) {
        String eventId = firstPart(entry.actionArg());
        if ("flash".equals(eventId)) {
            eventId = "flash";
        }
        ParanoiaEventDescriptor descriptor = ParanoiaEventCatalog.byId().get(eventId);
        Text detail = EVENT_TEXT.getOrDefault(eventId, text(
                "Runs the exact debug route for " + entry.groupLabel() + " (" + entry.label() + ").",
                "The event system must be enabled; forced execution may still require its real world context.",
                "A failed context check is reported without marking the QA entry as tested.",
                PRIVATE_EVENT_IDS.contains(eventId)
                        ? "Targeted/private unless its physical consequence is explicitly shared."
                        : "Server-authoritative and visible to relevant nearby players.",
                "See the event catalog for its exact sound, effect, block or entity family.",
                "Forced execution validates presentation, not natural probability or cooldown behavior."));

        int phase = descriptor == null ? eventFallbackPhase(eventId) : descriptor.minimumPhase();
        Danger danger = descriptor == null ? Danger.VARIABLE : danger(descriptor.severity());
        Rarity rarity = descriptor == null ? Rarity.CONTEXTUAL : rarity(descriptor);
        String phaseText = descriptor == null ? "Context-dependent" : "Phase " + phase + "+";
        String schedule = descriptor == null
                ? "No extracted scheduler metadata"
                : scheduleSummary(descriptor);
        Authority authority = PRIVATE_EVENT_IDS.contains(eventId)
                ? Authority.TARGETED
                : Authority.SHARED;
        ImplementationStatus implementation = ParanoiaEventCatalog.post111EventIds().contains(eventId)
                ? ImplementationStatus.WORKING_BUILD
                : (ParanoiaEventCatalog.isRetired(eventId)
                        ? ImplementationStatus.RETIRED
                        : ImplementationStatus.SHIPPED_111);
        ValidationNeed validation = BLOCK_MUTATING_EVENT_IDS.contains(eventId)
                ? ValidationNeed.MANUAL_REQUIRED
                : ValidationNeed.MANUAL_RECOMMENDED;

        return new Info(
                ContentType.EVENT,
                phase,
                phaseText,
                rarity,
                danger,
                authority,
                implementation,
                validation,
                detail.description(),
                detail.conditions() + " " + schedule,
                detail.restrictions(),
                detail.multiplayer(),
                detail.associated(),
                detail.limitations(),
                false,
                !BLOCK_MUTATING_EVENT_IDS.contains(eventId));
    }

    private static Info describeEntity(UncannyDevCatalog.Entry entry) {
        if (entry.actionArg().startsWith("approved|")) {
            return describeApprovedVanillaVariant(entry);
        }
        ApprovedSpecialCatalog.Definition approvedSpecial = ApprovedSpecialCatalog.byId(entry.groupKey());
        if (approvedSpecial != null) {
            return describeApprovedSpecial(approvedSpecial);
        }
        String group = entry.groupKey();
        String specialId = switch (group) {
            case "presence" -> "pulse";
            case "attacker" -> "stalker";
            default -> group;
        };
        ParanoiaEventDescriptor descriptor = ParanoiaEventCatalog.byId().get(specialId);
        Text special = SPECIAL_TEXT.get(specialId);
        int phase = descriptor == null ? variantMinimumPhase(entry) : descriptor.minimumPhase();
        Danger danger = descriptor == null ? Danger.VARIABLE : danger(descriptor.severity());
        Rarity rarity = descriptor == null ? Rarity.REPLACEMENT : rarity(descriptor);
        String behavior = special == null
                ? "Spawns " + entry.groupLabel() + " using the exact QA action “" + entry.label() + "”."
                : special.description();
        String conditions = special == null
                ? "Natural replacement and variant distribution depend on phase; this action bypasses the random roll."
                : special.conditions();
        String restrictions = special == null
                ? "A safe spawn point is searched at the requested distance; the action fails cleanly if none exists."
                : special.restrictions();
        String multiplayer = special == null
                ? "A real server entity is shared; target selection only chooses its anchor or initial target."
                : special.multiplayer();
        String associated = special == null
                ? "Registered entity renderer, AI, sounds, drops and the selected variant tag."
                : special.associated();
        String limitations = special == null
                ? "The 3D preview proves renderer availability, not AI, sounds, collisions or persistence."
                : special.limitations();
        boolean attackerAnimationStudy = "attacker_crawl".equals(entry.actionArg())
                || "attacker_outstretched".equals(entry.actionArg());
        if (attackerAnimationStudy) {
            behavior = "Spawns Attacker? with the selected movement-pose study while preserving its real chase and combat AI.";
            conditions = "Developer-only comparison route; natural Attacker? spawns keep the standard pose until one study is approved.";
            restrictions = "The study changes rendering only. Hitbox, speed, pathfinding, reach, damage and sound timing are unchanged.";
            associated = "Synced animation-style byte, dedicated silhouette model and the existing Attacker? renderer texture.";
            limitations = "Judge ground contact, silhouette readability, limb clipping and whether the unchanged standing hitbox remains acceptable.";
        }
        ImplementationStatus status = attackerAnimationStudy
                ? ImplementationStatus.WORKING_BUILD
                : "terror".equals(group)
                ? ImplementationStatus.DEBUG_ONLY
                : ImplementationStatus.SHIPPED_111;

        return new Info(
                ContentType.ENTITY,
                phase,
                "Phase " + phase + "+ (natural minimum or variant introduction)",
                rarity,
                danger,
                Authority.SHARED,
                status,
                ValidationNeed.MANUAL_REQUIRED,
                behavior,
                conditions,
                restrictions,
                multiplayer,
                associated,
                limitations,
                true,
                true);
    }

    private static Info describeApprovedVanillaVariant(UncannyDevCatalog.Entry entry) {
        ApprovedVanillaVariantCatalog.Variant variant =
                ApprovedVanillaVariantCatalog.byId(entry.actionArg().substring("approved|".length()));
        if (variant == null) {
            return Info.unknown();
        }
        Rarity rarity = switch (variant.rarity()) {
            case UNCOMMON -> Rarity.UNCOMMON;
            case RARE -> Rarity.RARE;
            case VERY_RARE -> Rarity.VERY_RARE;
        };
        Danger danger = switch (variant.danger()) {
            case 0 -> Danger.NONE;
            case 1 -> Danger.LOW;
            case 2 -> Danger.MEDIUM;
            default -> Danger.HIGH;
        };
        return new Info(
                ContentType.ENTITY,
                variant.minimumPhase(),
                "Phase " + variant.minimumPhase() + "+ naturally",
                rarity,
                danger,
                Authority.SHARED,
                ImplementationStatus.WORKING_BUILD,
                ValidationNeed.MANUAL_REQUIRED,
                variant.displayName() + " — " + variant.behavior() + ".",
                "The actual Vanilla " + variant.typeKey() + " must spawn naturally; the QA action creates the same tagged Vanilla entity.",
                "The added cue yields to combat, flight, breeding, taming, riding and other genuine Vanilla priorities.",
                "One real server entity and every presentation cue are shared with nearby players.",
                "Vanilla entity type, its original AI/attributes/loot and a bounded behavioral tag.",
                "Animation timing, interruption and multiplayer agreement still require live validation.",
                true,
                true);
    }

    private static Info describeApprovedSpecial(ApprovedSpecialCatalog.Definition definition) {
        Danger danger = switch (definition.danger()) {
            case 0 -> Danger.NONE;
            case 1 -> Danger.LOW;
            case 2 -> Danger.MEDIUM;
            default -> Danger.HIGH;
        };
        Rarity rarity = definition.weight() >= 4 ? Rarity.RARE
                : definition.weight() >= 2 ? Rarity.VERY_RARE : Rarity.EXCEPTIONAL;
        boolean prototype = definition.status() == ApprovedSpecialCatalog.Status.PROTOTYPE;
        return new Info(
                ContentType.ENTITY,
                definition.minimumPhase(),
                "Phase " + definition.minimumPhase() + "+ naturally",
                rarity,
                danger,
                Authority.SHARED,
                prototype ? ImplementationStatus.PARTIAL : ImplementationStatus.WORKING_BUILD,
                ValidationNeed.MANUAL_REQUIRED,
                definition.description(),
                "The Special uses its real contextual spawn gate; QA keeps every concept-critical context and reports a precise failure when it is absent.",
                prototype
                        ? "Prototype remains collision-bound: no generic noPhysics wall traversal and no natural success without a wall/ceiling context."
                        : "Attack, approach, enclosure, boat, water and obstruction responses are bounded by the entity state machine.",
                "One server-authoritative entity and one focus player; every observer sees the same actions.",
                "Dedicated registry type, shared renderer, persisted focus/state and the common Special scheduler lane.",
                prototype
                        ? "Ceiling traversal is limited to one verified continuous wall, a stationary upside-down latch and a visible descent; full ceiling navigation is not claimed."
                        : "Timing, path interruption, projectiles and two-player target behavior require live QA.",
                true,
                true);
    }

    private static Info describeWeather(UncannyDevCatalog.Entry entry) {
        String id = entry.actionArg();
        if (entry.actionKind() == UncannyDevCatalog.ActionKind.STOP_WEATHER) {
            return new Info(ContentType.WEATHER, 1, "Any phase (debug control)", Rarity.DEBUG, Danger.NONE,
                    Authority.SHARED, ImplementationStatus.DEBUG_ONLY, ValidationNeed.AUTOMATED_SUFFICIENT,
                    "Stops the active horror-weather state and asks clients to restore normal presentation.",
                    "A server and loaded world are required.", "Does not change vanilla long-term weather configuration.",
                    "Global server control; all connected clients resynchronize.", "Weather SavedData and client sync payload.",
                    "A client still needs to confirm fog/audio cleanup after a real effect.", false, true);
        }
        Info localized = describeLocalizedWeather(id);
        if (localized != null) {
            return localized;
        }
        int phase = id.startsWith("rain_") || id.startsWith("fog_breathing") ? 2 : 3;
        Danger danger = id.contains("target") || id.contains("stroboscopic") || id.contains("pressure")
                ? Danger.HIGH : Danger.MEDIUM;
        return new Info(ContentType.WEATHER, phase, "Phase " + phase + "+ naturally", Rarity.RARE, danger,
                Authority.SHARED, ImplementationStatus.SHIPPED_111, ValidationNeed.MANUAL_REQUIRED,
                "Forces the “" + entry.label() + "” weather presentation and effect family.",
                "Natural use requires phase 2+, a compatible dimension and no Tension/Grand lock; debug forcing bypasses phase.",
                "Fog, particles, lightmap and audio must stop cleanly on replacement, logout and dimension changes.",
                "Weather is world-shared and synchronized to every relevant client.",
                "Weather SavedData, client atmosphere renderer, sounds, particles and darkness bonus.",
                "Ash Rain performance and visual intensity cannot be proven by unit tests.", false, false);
    }

    private static Info describeLocalizedWeather(String id) {
        int phase;
        Rarity rarity;
        String description;
        String conditions;
        String restrictions;
        String associated;
        String limitations;
        switch (id) {
            case "rain_front" -> {
                phase = 2;
                rarity = Rarity.RARE;
                description = "Makes the visible edge of real rain unnaturally straight and lets that front approach, retreat or wait at a fixed point.";
                conditions = "Requires genuine rain, an exposed loaded area and phase 2+ for natural selection.";
                restrictions = "Only precipitation presentation changes; biome, weather state, wetness and chunks remain untouched.";
                associated = "Localized-weather payload and vanilla rain renderer filtering.";
                limitations = "The moving boundary and agreement between two viewpoints require visual QA.";
            }
            case "suspended_rain" -> {
                phase = 3;
                rarity = Rarity.VERY_RARE;
                description = "Holds visible rain drops inside a small common volume for one second, then releases them together.";
                conditions = "Requires genuine rain, exposed sky and phase 3+ for natural selection.";
                restrictions = "Audio continues and gameplay weather is unchanged; the effect is tightly bounded to avoid a screen-freeze appearance.";
                associated = "Vanilla rain texture, localized filtering and synchronized custom drops.";
                limitations = "Must be rejected in game if it resembles a generic frozen frame rather than suspended precipitation.";
            }
            case "dry_eye" -> {
                phase = 2;
                rarity = Rarity.RARE;
                description = "Makes real rain avoid a fixed empty disc in the world instead of following a player.";
                conditions = "Requires genuine rain, exposed terrain and phase 2+ for natural selection.";
                restrictions = "The dry area never suppresses weather mechanics and cannot become a player-controlled shelter.";
                associated = "Server-selected world anchor and localized vanilla-rain filtering.";
                limitations = "Boundary shape, sound continuity and movement around the fixed point need live validation.";
            }
            case "clear_downpour" -> {
                phase = 1;
                rarity = Rarity.VERY_RARE;
                description = "Creates a short shared downpour under a bright clear sky, using rain drops and ground impacts without starting world rain.";
                conditions = "Requires clear weather, an exposed loaded area and phase 1+.";
                restrictions = "It causes no thunder, darkening, wetness, cauldron fill or weather-state mutation.";
                associated = "Vanilla rain texture, splash particles and localized-weather payload.";
                limitations = "Brightness and particle density must remain convincing across graphics settings.";
            }
            case "wrong_snowline" -> {
                phase = 2;
                rarity = Rarity.RARE;
                description = "Moves a narrow band through real precipitation where rain is rendered as snow, or snow as rain, against the biome's rules.";
                conditions = "Requires genuine precipitation, exposed terrain and phase 2+.";
                restrictions = "No snow layer, freezing, hydration or biome data changes.";
                associated = "Biome precipitation interception and a shared moving band.";
                limitations = "The transition must be checked in both rainy and snowy biomes.";
            }
            case "light_avoiding_rain" -> {
                phase = 3;
                rarity = Rarity.VERY_RARE;
                description = "Lets a small, irregular subset of player-placed torches or lanterns repel visible rain in narrow columns.";
                conditions = "Requires genuine rain, phase 3+ and a recently recorded player-placed light still present in a loaded chunk.";
                restrictions = "Not every light qualifies; no stable protection rule or block/weather mutation is created.";
                associated = "Bounded SavedData positions and localized rain filtering.";
                limitations = "Placement ownership, removed-light cleanup and multiplayer agreement need live testing.";
            }
            case "converging_rain" -> {
                phase = 3;
                rarity = Rarity.VERY_RARE;
                description = "Tilts drops in a small volume toward one empty point while leaving their ordinary impacts on the ground.";
                conditions = "Requires genuine rain, an exposed loaded area and phase 3+.";
                restrictions = "The convergence point has no entity, hitbox, damage or persistent marker.";
                associated = "Synchronized oriented rain quads and ordinary splash particles.";
                limitations = "Perspective, orientation and shader compatibility require visual QA.";
            }
            case "leaking_sky" -> {
                phase = 2;
                rarity = Rarity.RARE;
                description = "Lets a narrow column of rain appear beneath an intact roof and stop at the interior floor.";
                conditions = "Requires genuine rain and a loaded two-to-four-block interior below a roof that is itself exposed to sky.";
                restrictions = "No block is traversed, wetted, replaced or resynchronized falsely.";
                associated = "Roof validation, localized vanilla rain quads and ground impacts.";
                limitations = "Slabs, unusual roofs and two observers inside the room need manual QA.";
            }
            default -> {
                return null;
            }
        }
        return new Info(ContentType.WEATHER, phase, "Phase " + phase + "+ naturally", rarity, Danger.NONE,
                Authority.SHARED, ImplementationStatus.WORKING_BUILD, ValidationNeed.MANUAL_REQUIRED,
                description, conditions, restrictions,
                "The server selects one common volume and synchronizes identical parameters to nearby players.",
                associated, limitations, false, true);
    }

    private static Info describeStructure(UncannyDevCatalog.Entry entry) {
        boolean phaseThree = Set.of("patterned_grove", "barren_grid", "wrong_village_house", "wrong_village_utility", "sinkhole")
                .contains(entry.groupKey());
        int phase = phaseThree ? 3 : 1;
        return new Info(ContentType.STRUCTURE, phase, "Worldgen phase gate: " + phase + "+", Rarity.VERY_RARE,
                Danger.NONE, Authority.SHARED, ImplementationStatus.SHIPPED_111, ValidationNeed.MANUAL_REQUIRED,
                "Generates the selected “" + entry.groupLabel() + "” QA variant near the target when terrain validation succeeds.",
                "Overworld, loaded chunks, enabled system and a terrain-compatible origin are required.",
                "Generation intentionally refuses unsuitable terrain; it must not flatten the landscape to make the test pass.",
                "A permanent world structure is shared. Use only a disposable QA world or a backup.",
                "Structure generator, palettes, loot/lore hooks, markers and optional villagers.",
                "No automatic cleanup is possible for generated blocks; failure may simply mean no safe origin was found.",
                false, false);
    }

    private static Info describeAudio(UncannyDevCatalog.Entry entry) {
        boolean mental = entry.actionKind() == UncannyDevCatalog.ActionKind.PLAY_SOUND_MENTAL;
        return new Info(ContentType.AUDIO, 1, "Debug only", Rarity.DEBUG, Danger.NONE,
                mental ? Authority.TARGETED : Authority.SHARED,
                ImplementationStatus.DEBUG_ONLY,
                ValidationNeed.MANUAL_REQUIRED,
                "Plays the registered sound “" + entry.actionArg() + "” through the "
                        + (mental ? "private non-spatial" : "shared spatial") + " QA route.",
                "The target must be connected and have the Echo of the Void sound registry loaded.",
                "This raw audition does not execute the event timing, random pitch or surrounding visual sequence.",
                mental ? "Only the selected player should hear it, fixed inside their perspective."
                        : "Players near the target hear it according to vanilla attenuation.",
                "SoundEvent echoofthevoid:" + entry.actionArg() + ".",
                "Volume quality, clipping, direction and duration require headphones/two-player listening.",
                false,
                true);
    }

    private static Info describeTool(UncannyDevCatalog.Entry entry) {
        String description = switch (entry.actionKind()) {
            case CLEAN_TEST_ENTITIES -> "Removes only entities tagged as created by this development menu.";
            case RESET_TEST_ENVIRONMENT -> "Stops transient horror weather, Tension/Grand debug states and removes tagged test entities.";
            case SET_PHASE -> "Sets the persistent world phase to " + entry.actionArg() + " for explicit QA.";
            default -> "Development-only control.";
        };
        String restriction = entry.actionKind() == UncannyDevCatalog.ActionKind.SET_PHASE
                ? "This deliberately mutates world progression and is not automatically reverted. Use a QA copy."
                : "It never removes ordinary entities or structures and does not undo permanent block changes.";
        return new Info(ContentType.TOOL, 1, "Debug only", Rarity.DEBUG, Danger.NONE, Authority.SHARED,
                ImplementationStatus.DEBUG_ONLY, ValidationNeed.AUTOMATED_SUFFICIENT,
                description, "Requires operator permission level 2 and an active server.", restriction,
                "Server-wide state may affect every connected player.", "QA session tracking and existing control APIs.",
                "Generated structures and untagged entities are intentionally outside cleanup scope.", false, true);
    }

    private static int variantMinimumPhase(UncannyDevCatalog.Entry entry) {
        String id = entry.id();
        int marker = id.lastIndexOf("_v");
        if (marker >= 0 && marker + 2 < id.length()) {
            try {
                int variant = Integer.parseInt(id.substring(marker + 2));
                return switch (variant) {
                    case 1 -> 1;
                    case 2 -> 2;
                    case 3 -> 3;
                    case 4, 5 -> 4;
                    default -> 1;
                };
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }
        return 1;
    }

    private static int eventFallbackPhase(String eventId) {
        return switch (eventId) {
            case "bed", "grand_event_stop", "tension_builder_start", "tension_builder_stop" -> 1;
            case "grand_event", "grand_event_warden" -> 4;
            default -> 1;
        };
    }

    private static Rarity rarity(ParanoiaEventDescriptor descriptor) {
        int weight = Math.max(descriptor.primaryWeight(), Math.max(descriptor.ambientWeight(), descriptor.specialWeight()));
        if (weight >= 14) {
            return Rarity.COMMON_IN_POOL;
        }
        if (weight >= 8) {
            return Rarity.UNCOMMON;
        }
        if (weight >= 4) {
            return Rarity.RARE;
        }
        if (weight >= 2) {
            return Rarity.VERY_RARE;
        }
        return Rarity.EXCEPTIONAL;
    }

    private static Danger danger(ParanoiaEventSeverity severity) {
        return switch (severity) {
            case LIGHT -> Danger.LOW;
            case MEDIUM -> Danger.MEDIUM;
            case HIGH -> Danger.HIGH;
            case EXTREME -> Danger.EXTREME;
        };
    }

    private static String scheduleSummary(ParanoiaEventDescriptor descriptor) {
        int weight = Math.max(descriptor.primaryWeight(), Math.max(descriptor.ambientWeight(), descriptor.specialWeight()));
        int cooldown = Math.max(descriptor.eventCooldownSeconds(), descriptor.ambientCooldownSeconds());
        return "Lane(s): " + descriptor.lanes() + "; base weight " + weight
                + (cooldown > 0 ? "; explicit cooldown " + cooldown + " s." : "; scheduler cooldown applies.");
    }

    private static String firstPart(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int separator = value.indexOf('|');
        return (separator < 0 ? value : value.substring(0, separator)).trim().toLowerCase(Locale.ROOT);
    }

    private static Text text(
            String description,
            String conditions,
            String restrictions,
            String multiplayer,
            String associated,
            String limitations) {
        return new Text(description, conditions, restrictions, multiplayer, associated, limitations);
    }

    public enum ContentType {
        ENTITY("Entity", 0),
        EVENT("Event", 1),
        WEATHER("Weather", 2),
        STRUCTURE("Structure", 3),
        AUDIO("Audio", 4),
        TOOL("Tool", 5),
        UNKNOWN("Unknown", 6);

        private final String label;
        private final int sortRank;

        ContentType(String label, int sortRank) {
            this.label = label;
            this.sortRank = sortRank;
        }

        public String label() {
            return this.label;
        }

        public int sortRank() {
            return this.sortRank;
        }
    }

    public enum Rarity {
        COMMON_IN_POOL("Common in pool", 0),
        REPLACEMENT("Replacement distribution", 1),
        CONTEXTUAL("Contextual", 2),
        UNCOMMON("Uncommon", 3),
        RARE("Rare", 4),
        VERY_RARE("Very rare", 5),
        EXCEPTIONAL("Exceptional", 6),
        DEBUG("Debug only", 7);

        private final String label;
        private final int sortRank;

        Rarity(String label, int sortRank) {
            this.label = label;
            this.sortRank = sortRank;
        }

        public String label() {
            return this.label;
        }

        public int sortRank() {
            return this.sortRank;
        }
    }

    public enum Danger {
        NONE("None", 0),
        LOW("Low", 1),
        MEDIUM("Medium", 2),
        HIGH("High", 3),
        EXTREME("Extreme", 4),
        VARIABLE("Variable", 5);

        private final String label;
        private final int sortRank;

        Danger(String label, int sortRank) {
            this.label = label;
            this.sortRank = sortRank;
        }

        public String label() {
            return this.label;
        }

        public int sortRank() {
            return this.sortRank;
        }
    }

    public enum Authority {
        TARGETED("Targeted / private"),
        SHARED("Server-authoritative / shared"),
        CLIENT("Client presentation only"),
        DEBUG("Debug control");

        private final String label;

        Authority(String label) {
            this.label = label;
        }

        public String label() {
            return this.label;
        }
    }

    public enum ImplementationStatus {
        SHIPPED_111("Shipped in 1.1.1"),
        WORKING_BUILD("Implemented in working build"),
        DEBUG_ONLY("Debug-only utility"),
        PARTIAL("Partially implemented"),
        RETIRED("Retired by design");

        private final String label;

        ImplementationStatus(String label) {
            this.label = label;
        }

        public String label() {
            return this.label;
        }
    }

    public enum ValidationNeed {
        AUTOMATED_SUFFICIENT("Automated checks sufficient"),
        MANUAL_RECOMMENDED("Manual test recommended"),
        MANUAL_REQUIRED("Manual test required");

        private final String label;

        ValidationNeed(String label) {
            this.label = label;
        }

        public String label() {
            return this.label;
        }
    }

    public record Info(
            ContentType type,
            int minimumPhase,
            String phase,
            Rarity rarity,
            Danger danger,
            Authority authority,
            ImplementationStatus implementation,
            ValidationNeed validation,
            String description,
            String conditions,
            String restrictions,
            String multiplayer,
            String associated,
            String limitations,
            boolean entityPreview,
            boolean repeatSafe) {

        public Info {
            type = type == null ? ContentType.UNKNOWN : type;
            minimumPhase = Math.max(1, Math.min(4, minimumPhase));
            phase = safe(phase);
            rarity = rarity == null ? Rarity.CONTEXTUAL : rarity;
            danger = danger == null ? Danger.VARIABLE : danger;
            authority = authority == null ? Authority.DEBUG : authority;
            implementation = implementation == null ? ImplementationStatus.PARTIAL : implementation;
            validation = validation == null ? ValidationNeed.MANUAL_REQUIRED : validation;
            description = safe(description);
            conditions = safe(conditions);
            restrictions = safe(restrictions);
            multiplayer = safe(multiplayer);
            associated = safe(associated);
            limitations = safe(limitations);
        }

        public String searchText(UncannyDevCatalog.Entry entry) {
            return String.join(" ",
                    entry.id(), entry.groupKey(), entry.groupLabel(), entry.label(), entry.actionArg(),
                    type.label(), phase, rarity.label(), danger.label(), authority.label(),
                    implementation.label(), validation.label(), description, conditions, restrictions,
                    multiplayer, associated, limitations).toLowerCase(Locale.ROOT);
        }

        private static Info unknown() {
            return new Info(ContentType.UNKNOWN, 1, "Unknown", Rarity.CONTEXTUAL, Danger.VARIABLE,
                    Authority.DEBUG, ImplementationStatus.PARTIAL, ValidationNeed.MANUAL_REQUIRED,
                    "No metadata available.", "Unknown.", "Unknown.", "Unknown.", "Unknown.", "Unknown.", false, false);
        }

        private static String safe(String value) {
            return value == null || value.isBlank() ? "Not documented." : value.trim();
        }
    }

    private record Text(
            String description,
            String conditions,
            String restrictions,
            String multiplayer,
            String associated,
            String limitations) {
    }
}
