# Echo Of The Void - Changelog v1.1.1

Date: 2026-04-04

## Added / Balanced: Gameplay pacing pass (phase gates, weather, rarity)

- Weather events are now phase-gated to **Phase 2+**.
- Added anti-repeat weather selection: the same weather event is no longer selected twice in a row unless no valid alternative exists.
- Increased spacing/cooldowns between weather events for a less spammy pacing.
- Rain-family mod sounds are now rain-biome aware:
  - in biomes without rain (including snow precipitation), rain-like weather audio is blocked.
- Reduced intensity/frequency for weather loops that were too aggressive (including creak/tinnitus-style loops and false-rain loudness pressure).
- Reduced frequency of whisper/psss rain-side sounds.

- Bell mob-wave behavior is now restricted to **Phase 2+** and remains very rare in phases 2 and 3.
- Specials are now constrained to **Phase 2+** auto-spawn flow (with the dedicated first-night watcher exception below).
- Specials were further tuned to be significantly rarer overall.
- Specials are blocked when player context is invalid for spawn (including water-related invalid contexts already enforced in runtime checks).
- `Attacker?` (stalker channel) was made rarer and receives stronger cooldown pressure.

- Triple block-break echo events (tool-answer style) were made much rarer.
- Furnace `psss` event was made much rarer, less frequent, and quieter.
- Small ambient/noise events were slightly reduced globally.

- Bed disturbance (`There is something in your bed.`) now has click debounce between attempts to prevent right-click spam from instantly consuming the sequence.

- Added a first-night guarantee policy for `Watcher?`:
  - one guaranteed watcher appearance at first-night start window per player.

- Grand Warden automatic start policy now includes strict pacing gates:
  - event roll/start is constrained to Phase 4,
  - spawn warning delay is applied after the first warning line to avoid instant panic/instant trigger windows.

- `Ghost Weaver` cobweb cadence is now randomized to **5-8 seconds**.

## Fixed: Grand Warden sound hunt flow

- Sound investigation now starts from recent captured audible events and uses the real sound position, including projectile impact locations.
- Enforced strict investigation order:
  - travel to source,
  - local sweep around the source,
  - sniff on-site,
  - target decision,
  - roar/aggro only after sniff when a valid local target exists.
- Removed bypass paths that could lead to pre-sniff roar or aggro.
- If no valid target is found after sniff, the sound investigation now clears cleanly and the Warden returns to normal search behavior.

## Improved: Sound-combat handling and recovery

- Added a dedicated sound-combat flow for both players and non-player living entities selected near the sound source.
- After kill, target loss, or invalid target, the Warden exits sound-combat and resumes normal investigation/search instead of forcing event end.
- Kept movement-trigger aggro behavior unchanged (unsneak movement trigger remains as before).

## Improved: Grand Event freeze coverage

- During Grand Warden event runtime, freeze behavior is applied to non-player mobs in the event zone, with state restoration at event end.

## Debug / observability

- Added and refined debug traces for:
  - sound source capture,
  - sound probe phases,
  - local sweep progress,
  - sniff fire,
  - post-sniff target selection,
  - escalation/clear reasons.
