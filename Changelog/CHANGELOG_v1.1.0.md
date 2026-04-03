# Echo Of The Void - Changelog v1.1.0

Date: 2026-04-02

## New: Grand Event system

## First Grand Event: Grand Warden

### Runtime authority and diagnostics

- Added structured runtime instrumentation for the Grand Warden event:
  - per-event runtime ID,
  - build signature,
  - flow entry tracing (`tickActiveGrandEvent`, `tickGrandEventApproach`, `tickGrandEventExit`),
  - authority traces for move/look/activity/path/attack/exit/cave-break,
  - intent lifecycle logs (`issued`, `consumed`, `not-consumed`, `overwritten`).
- Added advanced debug traces for:
  - `node_churn`,
  - `path_churn`,
  - `spin_in_place`,
  - `replan_window`,
  - `local_orbiting`,
  - `mobility.progress`,
  - `coverage60s`,
  - exit ownership lock (`search_suppressed_in_exit`, `exit_nav_owner=retreat_only`).

### Spawn, visual identity, and naming

- Added dedicated custom texture for Grand Warden (`uncanny_grand_warden_special.png`).
- Grand Warden kill name is set to `Warden?`.
- Added context-aware spawn rules:
  - strict vertical non-air scan for covered/open-sky detection,
  - covered areas: closer spawn (10-20 blocks), back-angle preference, near-player Y preference,
  - open-sky areas: spawn only at fully open vertical positions.
- Added emergence phase on initial spawn and recovery spawn.

### Scope and targeting constraints

- Added strict event scope around the event anchor (64-block radius).
- Scope rules now apply consistently to:
  - player tracking,
  - focus candidate selection,
  - movement trigger validation,
  - sound trigger validation,
  - target validity,
  - cleanup decisions.
- Added explicit out-of-scope rejection logs with anchor distance.

### Non-aggro behavior: clueless but active

- Enforced non-aggro invariant:
  - non-aggro investigation is anchor/search/navigation/environment-driven,
  - no player-position-driven steering for investigation decisions.
- Added path-first, sectorized 360 investigation:
  - coverage-weighted scoring,
  - recent overlap control,
  - recent sector/microzone penalties,
  - strict `chosen -> issued -> consumed` lifecycle handling.
- Added anti-sticking safeguards:
  - hard-contact guard,
  - anchor-based forced separation,
  - guard lockout to avoid destructive reset loops.
- Added mobility recovery logic:
  - stagnation detection,
  - local-orbiting detection,
  - bounded recovery sweep,
  - controlled history reset under cooldown.
- Added occasional non-aggro search sniff flavor:
  - rare cadence,
  - never contact-range,
  - never explicitly player-facing,
  - brief, then normal investigation resumes.
- Added non-aggro activity budget gate (time + nodes + sectors + spatial radius) with safety timeout.

### Aggro, chase, and combat reliability

- Added movement-based aggro trigger reliability rules:
  - standing intentional movement can trigger,
  - crouching movement is ignored,
  - server-side accumulation and bounded exclusions retained.
- Added strict player-audible validation for vanilla-sound aggro channel:
  - prevents false aggro from external entities (for example zombies) without recent player-audible proof.
- Added dynamic chase speed by distance and frequent chase path refresh.
- Added controlled sonic assist pressure with anti-spam safeguards (LOS + cooldown gating).
- Added no-block water chase handling.
- Water chase speed is configured with a 40% scale reduction (`GRAND_EVENT_WATER_SPEED_SCALE=0.60`).

### Cave traversal and break assist

- Added cave path-front block break assist for Grand Warden when event starts covered:
  - natural-block whitelist only,
  - break above foot level only,
  - bounded per-tick caps,
  - temporary stuck boost when no-intent stalls persist.
- Goal: reduce cave deadlocks while avoiding support-block destruction at feet.

### Exit, sink, and cleanup stability

- Added atomic exit authority lock:
  - single transition into exit ownership,
  - pending search state cleared once,
  - search issue/reissue fully suppressed during exit,
  - retreat/sink remains the only navigation owner in exit.
- Added cave vs surface exit behavior:
  - cave: short retreat with minimum distance guard, then sink,
  - surface: retreat out-of-sight, then sink.
- Added visible sink dig pose handling with single-trigger dig sound behavior.
- Added explicit cleanup reason logging:
  - `no_valid_players`,
  - `event_finished`,
  - `warden_missing`,
  - `forced_stop`.
- Added solo cleanup behavior for no-valid-player conditions.

## New: TensionBuilder integration

- TensionBuilder was added as part of this release line and is now fully integrated with event pacing.
- While TensionBuilder is active, all auto events are blocked (including minor/light events).
- TensionBuilder is also used as the post-window driver for Grand Warden frequency calibration.

## Global pause policy integration

### During active Grand Warden event

- Added dimension-wide auto-event pause during active Grand Warden.
- Added local special-entity pause around the event anchor:
  - temporary AI halt,
  - runtime pause tag,
  - shake visual while paused,
  - safe state restore when event ends.

### During active TensionBuilder

- Added full auto-event suppression while TensionBuilder is active, including minor/light events.
- Suppression now covers:
  - random auto events,
  - ambient auto events,
  - independent living ore auto trigger,
  - related auto interaction paths in sleep/right-click/block-break event flow.
- Added dedicated suppression logs:
  - `TENSION pause_all_events=true`,
  - `AUTO_EVENT/AUTO_AMBIENT/LIVING_ORE suppressed reason=tension_builder`.

## Grand Warden frequency tuning (post-TensionBuilder)

- Configured roll chances for a target average cadence around ~1h15 (RNG-dependent):
  - `GRAND_EVENT_BASE_CHANCE`: `0.00025 -> 0.0`
  - `GRAND_EVENT_POST_TENSION_CHANCE`: `0.012 -> 0.22`
- Event status output now includes:
  - `grandBaseChance`,
  - `grandBoostChance`,
  - `nextGrandRoll`,
  - `grandBoost` time.
- Roll logs now include `nextRollIn`.

## Full Progressive Darkness Rework

- Progressive Darkness was fully reworked in this release line.
- This release replaces the old threshold-heavy fog/overlay darkness driver with a lightmap-driven pipeline.
- Added a dedicated darkness engine:
  - `UncannyProgressiveDarknessEngine`.
- Switched darkness authority to lightmap-driven attenuation:
  - 16x16 target/smoothed luminance tables,
  - per-pixel progressive attenuation during lightmap upload.
- Added client darkness mixins:
  - `MixinLightTexture`,
  - `MixinGameRenderer`,
  - `MixinDynamicTexture`,
  - client mixin config integration.
- Added and connected full `uncanny.darkness.*` config surface:
  - dimension toggles,
  - phase strengths,
  - curve exponent,
  - light floor/ceiling,
  - darken/brighten speeds,
  - overlay cap,
  - `lightmapStrength`,
  - `maxPixelAttenuation`.
- Added runtime observability:
  - `DARKNESS_REWORK` logs,
  - `mixin_inactive` warning path.

## Visual/audio and item updates

- Updated item icons:
  - `Uncanny Reality Shard`,
  - `Uncanny Reality Shard Piece`.
- Added stable phase 2+ vanilla music suppression handling in active event flows.
- Grand Warden gameplay darkness is configured as short-duration pulses for clean post-event clearance.

## Entity adjustments

- `Follower?` now uses `maxUpStep = 1.0F`.
- Grand Warden step-up enforcement during event remains active (`3.0`) with periodic re-apply safeguards.

## Compatibility notes

- No registry, ID, ResourceLocation gameplay, NBT, or world-save schema changes in this release.
- Commands and debug tooling remain available.
- Added debug log volume is gated behind `SetDebugLog true`.
