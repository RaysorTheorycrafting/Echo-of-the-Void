# Echo Of The Void - Changelog v1.0.6.2

Date: 2026-03-26

## Event System Fixes

### Manual interaction cooldown consumption

- Fixed several manual/paranoia interactions that could consume cooldowns even when no visible effect happened.
- Cooldowns are now applied only when the event actually triggers successfully.
- Updated flow for:
  - `bedside_open`
  - `workbench_reject`
  - `false_container_open`
  - `tool_answer`
  - `compass_liar` (manual trigger path)
- This resolves cases where `/uncanny debugEvents` showed cooldown progression while nothing happened in-game.

### Debug report weather timer sanity

- Fixed extreme/invalid weather cooldown display values in debug output (sentinel/overflow-like values).
- Added safe remaining-time formatting for weather timers in `/uncanny debugEvents`.

### Weather timer sanitization

- Added runtime sanitization to weather next-check and weather cooldown timers.
- Invalid or stale far-future values are now clamped/recovered cleanly during server tick updates.
- Prevents “dead” weather scheduling states after bad timer values.

## Special Spawn Reliability

### Cave/surface context-aware special spawns

- Improved special spawn placement to better match player context:
  - if player is on surface, spawn attempts prioritize surface-valid spots,
  - if player is in caves, spawn attempts prioritize cave-valid spots.
- Spawn search now checks downward + upward paths relative to player Y for better underground reliability.
- Reduced excessive far-distance spawn windows when player is underground so specials remain perceptible.
- Applied in the shared special spawn pipeline used by scheduled special appearances.

## Gameplay Adjustments

### Giant Sun - levitation cap

- The levitation pulse behavior tied to the Giant Sun sequence is now capped.
- Maximum levitation applications per event instance: **3**.
- Prevents repeated levitation chaining beyond intended intensity.

### Keeper? - near base but not inside base

- Keeper? spawn eligibility was tightened to avoid invalid in-base appearances.
- Keeper? can now be selected/spawned only when the player is:
  - **near the base**
  - but **not inside** the base interior radius.
- Added explicit guardrails in both special-choice selection and spawn execution so invalid "inside-base" keeper attempts are skipped cleanly.
