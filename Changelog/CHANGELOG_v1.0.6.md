# Echo Of The Void - Changelog v1.0.6

Date: 2026-03-26

## Major Content Expansion

### New paranoia events added

Added and integrated into scheduler + manual triggers + devmenu:

- `animal_stare_lock`
- `bedside_open`
- `compass_liar`
- `furnace_breath`
- `misplaced_light`
- `pet_refusal`
- `workbench_reject`
- `false_container_open`
- `lever_answer`
- `pressure_plate_reply`
- `campfire_cough`
- `bucket_drip`
- `hotbar_wrong_count`
- `corrupt_toast` (event replacement/extension of false recipe concept)
- `tool_answer`

### Event variant extensions

- `footsteps` -> `ladder_steps`
- `door_inversion` -> `door_trap_cascade`
- `living_ore` -> `vein_retreat`
- `living_ore` -> `inside_knock`
- `phantom_mode` -> `lantern_eater` (testable variant trigger)

### New specials added

Added full entity registration, debug spawn support, and scheduler integration for:

- `Usher?` (`uncanny_usher`)
- `Keeper?` (`uncanny_keeper`)
- `Tenant?` (`uncanny_tenant`)
- `Follower?` (`uncanny_follower`)

### New item

- Added `Uncanny Compass` (`uncanny_compass`).
- Integrated with anomaly/lore-structure guidance logic and cleanup behavior.

## Audio Additions

### New event sounds imported and registered

- `ore_inside_knock`
- `campfire_cough_creepy`
- `follow_me_creature_glitch_v1`
- `psss` (mapped to `psss_1`)

### Sound registry + assets

- Updated `sounds.json` entries and subtitles.
- Registered matching `SoundEvent`s in `UncannySoundRegistry`.

## Corrupt Message / Toast Overhaul

- Reworked corrupt message system into larger contextual pools:
  - global phase-aware pool,
  - deep-cave pool,
  - higher phase escalation pools.
- Added player-context picker (`pickCorruptMessageForPlayer`) to select messages by situation/phase.
- Corrupt toast now uses the same contextual message source.
- Added support for both turn-around trap phrases:
  - `"Don't turn around."`
  - `"Turn around."`
- Glitch/obfuscation probability is now hard-capped at `1/500` effective runtime chance.

## Changes to existent elements

## Pulse?

- Heartbeat handling tightened with alive/removed checks to prevent post-death heartbeat behavior.

## Passive / Mob Variant Tuning

- Wolf imitator-style fake alert pacing significantly reduced (longer delays between fake triggers).
- Creeper absorber/blackout-oriented phase roll reduced in frequency (rarer selection in high-phase roll).
- Additional passive variant stability updates in scheduler and per-entity ticking paths.

## Multiplayer / Client Sync Fixes

### Pet Refusal visual sync fix

- Added new network payload:
  - `UncannyPetRefusalVisualPayload`
- Added server->client explicit sync for start, refresh, and stop states.
- Added client-side timed state storage and expiry cleanup.
- Added global clear handling on player/world state reset.
- This resolves client cases where pet-black visual state was present server-side but not rendered client-side.

### Client rendering hooks

- Added dedicated client renderers for vanilla pet entities in this flow:
  - `UncannyWolfRenderer`
  - `UncannyCatRenderer`
- Added additional render debug traces for diagnosis.

## UI / Client Effects

- Added/extended client UI pipeline for `hotbar_wrong_count` payload rendering.
- Added client event ticking cleanup for temporary visual systems (including pet-refusal visual cache).
- Kept effect rendering local to affected client where required.

## Commands & Devmenu

### New direct debug aliases

- `/uncanny spawnUsher`
- `/uncanny spawnKeeper`
- `/uncanny spawnTenant`
- `/uncanny spawnFollower`

### Variant trigger improvements

- Expanded `eventVariant` and variant suggestion sets for newly added families:
  - `footsteps|ladder_steps`
  - `door_inversion|door_trap_cascade`
  - `living_ore|vein_retreat`
  - `living_ore|inside_knock`
  - `phantom_mode|lantern_eater`

### Devmenu coverage

- Added dedicated entries for all new events listed above.
- Added dedicated entries for all new specials.
- Added Phantom mode test entry (`Lantern Eater`).
- Added `Uncanny Compass` give action entry.

## Localization

- Added/updated English localization entries for:
  - `Usher?`, `Keeper?`, `Tenant?`, `Follower?`
  - `Uncanny Compass`
  - new event subtitles (`ore_inside_knock`, `campfire_cough_creepy`)
  - expanded corrupt-message content pools.

## Internal Stability / Cleanup

- Expanded event-state cleanup coverage for new systems on disconnect/reset/unload flows.
- Added extra debug markers in event and special pipelines for deeper runtime diagnostics.
- Improved guardrails/fallback behavior to avoid hard failures when context is missing.
