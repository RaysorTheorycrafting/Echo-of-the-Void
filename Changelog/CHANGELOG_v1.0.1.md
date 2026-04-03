# Echo Of The Void - Changelog v1.0.1

Date: 2026-03-10

## Gameplay / Balance Fixes

### 1) Vanilla Music / Ambience
- Stabilized early-game audio logic:
  - clears lingering silence states in Phase 1 (`deafness` / `void_silence`) to avoid inconsistent cutoffs.
- Intentional music suppression remains active in Phase 2+ with no unintended impact expected in Phase 1.

### 2) Blackout (Progression)
- `Blackout` is no longer selected by the auto-event pool in Phase 2.
- Automatic `Blackout` now starts in Phase 3 (dramatic impact preserved).
- Internal fallbacks were updated to prevent too-early blackouts.
- `Thunder Stroboscopic` no longer triggers `Blackout` before Phase 3.
- The `Don't turn around` trap no longer chains into blackout at low phases (falls back to `Flash Error` before P3).

### 3) Early Game: More Event Variety
- Rebalanced Phase 1 auto-event pool:
  - added lighter variety (`flash_red`, `false_fall`, context-based `base_replay`, and underground `ghost_miner` / `cave_collapse`).
- Reduced dependence on the `footsteps` + `corrupt_message` pair in early gameplay.

### 4) Footsteps Too Repetitive
- Lowered base weight for `footsteps` in automatic rolls.
- Reduced profile multiplier for `footsteps`.
- Significantly increased `footsteps`-specific cooldown (less spam, stronger impact).

### 5) Ash Weather
- Removed the annoying `rain_ash` sound.
- Visual/atmospheric ash effect remains, without the problematic audio loop.

### 6) Specials: Perception Reliability
- Adjusted Special pacing:
  - reduced global cooldowns,
  - increased trigger chances.
- Added a close-range spawn fallback when a Special roll succeeds but all candidate spawns fail (improves real in-game perception).

### 7) ArmorBreak
- `ArmorBreak` now checks that at least one armor piece is equipped.
- If no armor is worn, the event will not trigger.

### 8) Debug Tool for Random Special Spawns
- New command:
  - `/uncanny debugSpecialRoll [target]`
- This command forces a roll through the real random Special system pipeline, instead of spawning a fixed entity.
- Detailed text feedback is returned for QA (success/failure + phase/profile/danger context).

## Changelog Maintenance
- Renamed previous changelog:
  - `CURSEFORGE_CHANGELOG_FULL.md` -> `CHANGELOG_v1.0.0.md`
- Added new file:
  - `CHANGELOG_v1.0.1.md`

## Additional 1.0.1 Hotfix
- Bed disturbance event (`There is something in your bed`) now spawns the entity at/around the clicked bed location instead of falling back to the player position.
