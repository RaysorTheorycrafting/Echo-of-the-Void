# Echo Of The Void - Changelog v1.0.6.3

Date: 2026-03-26

## Event Pacing & Ambient Audio

### Ambient/paranoia sound events were too frequent

- Increased ambient event cooldown baseline across profiles.
- Reduced ambient trigger chance to avoid repetitive low-impact spam.
- Increased explicit cooldowns for light audio events:
  - `false_container_open`
  - `bucket_drip`
  - `furnace_breath`
  - `lever_answer`
  - `pressure_plate_reply`
  - `campfire_cough`
  - `tool_answer`

### Tool Answer (3x mining reply sound) pacing

- Increased `tool_answer` manual/event cooldown to reduce repetition.
- Event remains functional, but now appears less aggressively in normal play.

## Weather System Fixes

### Weather not triggering naturally

- Fixed weather timer sanitization logic that could repeatedly postpone expired checks.
- Expired `nextCheck` and `cooldown` are now treated as immediately eligible instead of being pushed forward again.
- This restores natural weather triggering behavior closer to intended impact.

## Specials Balancing

### Requested spawn distribution tuning

- Increased practical scheduler presence for:
  - `Follower?`
  - `Stalker?`
  - `Attacker?`
- Reduced practical scheduler presence for:
  - `Pulse?`

This was done with combined weight/cooldown tuning in the shared special scheduler.

## Pulse? Fixes

### Spawn distance

- Natural `Pulse?` spawns were moved farther from the player.
- Updated distance windows and fallback ranges to reduce close surprise spawns.

### Frequency reduction

- Reduced `Pulse?` weighted selection in special pools.
- Increased `Pulse?` per-entity cooldown.
- Removed `Pulse?` from close/guaranteed fallback special spawns.
- Special pool trigger path for `pulse` now uses far spawn logic (no forced close-first roll).

## Follower? Behavior Fix

### Attack-state sink bug

- Fixed case where `Follower?` could sink into the ground after being hit while in attack mode.
- Player hits no longer force sink transition during active attack behavior.

## Watcher? Behavior Fix

### Direct-look flee reliability (including close range)

- Improved direct-look detection so Watcher flee triggers reliably even when very close.
- Relaxed overly strict look dot threshold and improved eye-vector target computation.
