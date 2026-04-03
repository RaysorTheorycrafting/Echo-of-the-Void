# Echo Of The Void - Complete CurseForge Changelog

Target version: NeoForge 1.21.1  
Mod id: `echoofthevoid`  
Language status: EN-only (current build)

## Identity and core direction

- Horror pillar: uncanny/near-normal distortions in Minecraft behavior.
- Horror pillar: sound-first design (wrong-position sounds, silence as threat).
- Global corruption progression with world phase escalation.
- Dedicated server + singleplayer support.
- EN naming convention locked:
  - Entities displayed as vanilla name + `?` (example: `Zombie?`).
  - Non-entity systems/items prefixed with `Uncanny`.

## Global phase and progression system

- 5 global states:
  - Phase 0 (purged/disabled)
  - Phase 1
  - Phase 2
  - Phase 3
  - Phase 4
- Default timing:
  - P1 -> P2: 30 min
  - P2 -> P3: 45 min
  - P3 -> P4: 60 min
- Death acceleration:
  - +15% phase progress per valid death
  - 5 min death-boost cooldown
  - clamp: no multi-phase skip in one death
- Replacement rates by phase:
  - P1: 1%
  - P2: 20%
  - P3: 60%
  - P4: 100%
- Supports phase lock and purge mode from altar.
- Phase 4 message uses glitched style text.

## World state persistence

- Full world SavedData persistence (`UncannyWorldState`):
  - phase/progress/purge/lock
  - event cooldown timelines
  - weather active state
  - per-player state for Mimic/Watcher/base absence/death/respawn
  - structure markers
  - lore tome progression tracking
  - world-persistent debug toggle

## Spawn replacement and anti-frustration

- Natural spawn replacement only.
- No replacement for spawner/egg/command spawns in V0.1+ rules.
- Spawn distance safety gate from players.
- Dedicated anti-cheap-death grace after respawn.
- Event global cooldown + category cooldowns.

## Registered uncanny hostile entities

- Core uncanny hostiles:
  - `Zombie?`, `Husk?`, `Drowned?`, `Zombie Villager?`
  - `Skeleton?`, `Stray?`, `Creeper?`
  - `Spider?`, `Spiderling?`
  - `Enderman?`, `Endermite?`
  - `Ghast?`, `Phantom?`
- Additional uncanny hostiles:
  - `Iron Golem?`
  - `Pillager?`, `Vindicator?`, `Evoker?`, `Ravager?`
  - `Blaze?`, `Wither Skeleton?`
  - `Piglin Brute?`, `Hoglin?`
  - `Slime?`, `Magma Cube?`

## Special entities (custom threats)

- `Mimic` (registry id `uncanny_double_dormant`, renamed behaviorally from Double Dormant).
- `Watcher?`
- `Attacker?` (registry id `uncanny_stalker`)
- `Hurler?`
- `Shadow?`
- `Knocker?`
- `Presence?` (Pulse)
- `Terror?`
- New rule: all special entities are immune to fall damage.

## High-priority uncanny AI and behavior suite

- Silent/uncanny behavior packs for early core mobs.
- Expanded raid/nether hostile reworks:
  - Pillager stare/aim + burst/reposition loop.
  - Vindicator glitch behavior.
  - Evoker delayed cast cadence.
  - Ravager jittered head/body glitching.
  - Blaze vertical agitation pulses.
  - Wither Skeleton dual archetypes (ranged/melee).
  - Piglin Brute stare/trigger and damage reaction behavior.
  - Hoglin/Zoglin inverted presentation with hidden tag handling.
  - Slime/Magma movement/ground behavior rework.

## Variant systems for major hostile families

- 5-phase variant systems implemented for:
  - Zombie?
  - Skeleton?
  - Creeper?
  - Spider?
  - Enderman?
- Includes per-variant AI/audio/trigger logic and phase-weighted rollout.
- Debug support integrated via commands/dev menu.

## Passive uncanny variant system (5 variants each)

- Species covered:
  - Chicken?
  - Pig?
  - Cow?
  - Sheep?
  - Wolf?
  - Cat?
  - Fox?
  - Squid?
  - Cod?
  - Salmon?
  - Parrot?
  - Llama?
  - Villager?
  - Wandering Trader?
- Hook coverage:
  - spawn
  - tick
  - damage incoming
  - death
  - player interaction
  - trading/taming hooks
- Includes many unique mechanics:
  - fake death patterns
  - false eggs
  - sound displacement
  - mimic/fake trade logic
  - fish-based ambush hooks
  - cat special revive/sense behavior

## Mimic event system

- Base-return threat event based on:
  - bed-derived base location (fallback world spawn)
  - out-of-base duration
  - phase gating
  - cooldown and block conditions
- Mimic appearance/targeting/equipment-copy behavior integrated.
- Dedicated debug command and report.

## Paranoia event system (random + weighted + profile/danger aware)

- Event scheduler with:
  - profile-based intensity (1..5)
  - danger level (0..5)
  - phase-aware weighting
  - global and per-event cooldown tuning
  - fallback forcing logic
- Event list:
  - `blackout`
  - `footsteps`
  - `flash` (Flash Error)
  - `baseReplay`
  - `bell`
  - `flashRed`
  - `voidSilence`
  - `falseFall`
  - `ghostMiner`
  - `caveCollapse`
  - `falseInjury`
  - `forceDrop`
  - `corruptMessage`
  - `bed`
  - `asphyxia`
  - `armorBreak`
  - `aquaticSteps`
  - `doorInversion`
  - `phantomHarvest`
  - `livingOre`
  - `projectedShadow`
  - `giantSun`
  - `hunterFog`
- Variant-capable event families:
  - Footsteps: `basic`, `echo`, `sprint`, `heavy`
  - Asphyxia: `false_alert`, `terrain_drowning`, `heavy_lungs`
  - ArmorBreak: `ghost_sound`, `drop_gear`, `cracked_defense`
  - AquaticSteps: `follower`, `slippery_ambush`, `invisible_bite`
  - DoorInversion: `poltergeist`, `lockdown`, `intrusion`
  - PhantomHarvest: `black_harvest`, `rotten_soil`, `infestation`
  - LivingOre: `bleeding`, `toxic_blood`, `vicious_fall`
  - ProjectedShadow: `mime`, `shadow_assault`, `ghost_shot`

## Special encounter orchestration

- Special entities now run on their own weighted spawn logic and cooldown model.
- Spawn weighting varies by phase/profile/danger.
- Spawn constraints improved:
  - avoid water/boat context where required
  - avoid impossible/invalid placements
  - better Y-level and nearby-air checks
  - dedicated base-only gating for Knocker

## Weather corruption system

- Independent global weather corruption runtime:
  - rain family:
    - `rain_silent`
    - `rain_dry_storm`
    - `rain_ash`
    - `rain_sobbing`
  - thunder family:
    - `thunder_silent`
    - `thunder_artificial`
    - `thunder_target_strike`
    - `thunder_stroboscopic`
  - fog family:
    - `fog_breathing`
    - `fog_black`
    - `fog_static_wall`
  - sky family:
    - `sky_fake_morning`
    - `sky_empty`
    - `sky_pressure`
- Durations expanded to multi-minute atmosphere windows (except short-impact events).
- Network-synced across players.

## True Darkness and advanced client atmosphere

- Progressive darkness by phase.
- Phase 4 true-darkness behavior implemented.
- Dynamic fog handling tied to weather/event tags.
- Giant sun rendering moved to world sky rendering pipeline.
- Hunter Fog movement-reactive behavior implemented.
- Vanilla music suppression in later phases.

## Structures and world features

- Procedural/feature generation system with phase/profile-aware rarity.
- Core structures:
  - `anechoic_cube` (Chambre Sourde)
  - `mimic_shelter`
  - `glitched_shelter`
  - `patterned_grove`
  - `barren_grid`
  - `false_descent`
  - `false_ascent`
  - `isolation_cube`
- Spiral structures:
  - mathematical clockwise pattern stepping
  - corrected stair orientation
  - corrected vertical pathing and headroom
  - descent/ascent variants
- Secret-house integration:
  - black door integration on eligible structures
  - attached secret house generation logic
  - forced-with-house variants for testing:
    - `false_descent_with_house`
    - `false_ascent_with_house`
- `locate structure uncanny <feature>` support via custom marker system.

## Black Door + secret house + lore chest system

- Secret house contains a dedicated chest with curated junk loot.
- Lore roll system:
  - 50% roll on secret chest interaction.
  - gives missing tome first (player-tracked).
  - once all tomes found, fallback can generate renamed "Behind you" book + `Terror?` spawn.
- Secret chest and lore behavior fully debug-logged.

## Lore items and tome progression

- `Uncanny Lore Piece` implemented as custom readable book item.
- Custom icon integrated.
- Direct open packet logic implemented for custom written-book behavior.
- Tome tracking avoids duplicates per player progression.
- Lore library now contains **6 full volumes**.
- Debug/admin utility command added:
  - `/uncanny giveAllTomes [target]`

## Endgame foundation: Altar and Cube

- Items:
  - `Uncanny Reality Shard`
  - `Uncanny Reality Shard Piece`
  - `Reality Cube`
  - `Uncanny Lore Piece`
- Blocks:
  - `Uncanny Altar`
  - `Uncanny Egg`
  - `Uncanny Black Web`
  - `Void Door`
- Recipes:
  - 9 pieces -> 1 shard
  - Reality Cube recipe (shards + Nether Star)
  - Altar recipe (shards + Diamond + Enchanting Table)
- Altar GUI:
  - phase lock buttons
  - purge action with confirmation
  - restart corruption confirmation flow after purge

## Dev Debug Menu (QA production tool)

- In-game interactive menu (`/uncanny devmenu`).
- Category-driven flow (Entities / Events / Weather / Structures groups).
- Action triggering from UI (including variants).
- QA status colors:
  - Gray (untested)
  - Orange (session review)
  - Green (validated)
- Persistent QA state file:
  - `config/echoofthevoid/uncanny_qa_state.json`

## Command and debug suite

- Root command: `/uncanny`
- System commands:
  - `setPhase`
  - `addPhaseProgress`
  - `setEventProfile`
  - `setDangerLevel`
  - `setDebugLogs`
- Spawn/debug commands include:
  - `spawnUncanny`
  - `spawnWatcher`
  - `spawnShadow`
  - `spawnHurler`
  - `spawnStalker`
  - `spawnAttacker`
  - `spawnKnocker`
  - `spawnPulse`
  - `triggerEnderman`
  - `forceMimic`
  - `debugMimic`
  - `debugEvents`
  - `forcePassive`
  - `forceFoxCry`
  - `giveAllTomes`
- Event commands:
  - `/uncanny event <...>`
  - `/uncanny eventVariant <event> <variant>`
- Weather commands:
  - `/uncanny weather trigger <id>`
  - `/uncanny weather stop`
- Locate support:
  - `/locate structure uncanny <feature>`

## Audio system

- Placeholder/custom sound registry integrated.
- Sound refs prepared for future custom assets.
- Multiple uncanniness patterns:
  - displaced source
  - near-head playback
  - selective muting
  - persistent custom sounds where required by design

## Networking and sync

- Dedicated payload sync for:
  - phase state
  - paranoia flags/effects
  - weather state/tags
  - dev menu state/actions
  - specific directed effects (example: zombie head-audio behavior)

## Multiplayer and persistence behavior

- World-global phase + weather logic synchronized to all players.
- Profile and danger levels are command-configurable and persist.
- Debug log switch persists world-side and config-side.

## Major bugfix and stability pass (cumulative)

- Invalid mod jar packaging issue fixed.
- Startup soft-lock/hang resolved (thread-unsafe random usage during spawn finalize).
- Multiple no-crash fallback paths for missing placeholder sounds.
- Fixed special spawn no-show edge cases with expanded placement attempts.
- Fixed custom lore chest roll lock caused by tick arithmetic overflow.
- Fixed lore item open path for custom written-book item.
- Fixed duplicate debug command aliasing confusion by keeping only `setDebugLogs`.
- Improved spawn safety constraints (water/boat invalid contexts, base constraints, air checks).
- Fixed many entity behavior regressions across iterative AI tuning.
- Fixed structure spiral math/orientation/headroom issues.
- Fixed false ascent/descent final orientation and door alignment issues.
- Fixed black texture/resource pathing and asset resolution for custom black visuals.

## Final content scope summary

- Global corruption progression with purge/lock.
- Full uncanny hostile + passive ecosystem.
- Special-entity paranoia layer.
- Large event and weather framework with variants.
- True darkness + custom atmospheric rendering.
- Procedural structure suite with secret-house and lore progression.
- Endgame altar/cube foundations.
- Full QA tooling + command/debug infrastructure.

