# Echo Of The Void - Changelog v1.0.3

Date: 2026-03-10

## Multiplayer / Audio Consistency

- Reworked event audio playback to be player-targeted for paranoia events (no unintended global broadcast to nearby players).
- Reworked weather-event audio to use local per-player playback where appropriate.
- Fixed Fog Breathing weather audio so players no longer hear another player's breathing track.

## Specials / Entities

- Added anti-powdered-snow behavior for all special entities so they no longer sink/get trapped in Powdered Snow.
- Improved Watcher direct-look logic:
  - flee trigger now requires continuous direct eye contact for more than 3 seconds,
  - look timer resets immediately when look/LOS conditions break.
- Improved Watcher owner/target tracking:
  - safer fallback targeting when the original watched player is invalid/offline/out-of-dimension,
  - controlled orphan despawn instead of erratic behavior.
- Reduced Pulse heartbeat repetition significantly to make it rarer and more impactful.
- Knocker now reacts immediately to being hit, interrupting its current sequence and switching to attack behavior.

## Door / Interaction Logic

- Kept special entities able to operate doors (including iron doors where intended).
- Knocker no longer breaks iron doors:
  - if the target door is iron, it flees instead of breaking it,
  - wooden-door behavior remains unchanged.
- Added/kept robust navigation door support for special entities requiring door traversal.

## Event Logic / Balance

- Hardened Armor Break validation:
  - event now requires actually equipped, breakable armor,
  - event no longer triggers in no-armor situations.
- Bed Disturbance (`There is something in your bed`) fully rebalanced:
  - much rarer base trigger chance with phase/profile scaling,
  - dedicated per-player cooldown after trigger,
  - anti-chain protection to prevent repeated back-to-back bed disturbances,
  - requires a normal sleep before this disturbance can trigger again after a completed sequence.

## Weather System

- Reduced overall weather-event durations across categories for better pacing.
- Reduced weather trigger regularity:
  - lower base trigger pressure,
  - stronger random jitter in check delay and cooldown,
  - less predictable distribution over time.
- Reduced `THUNDER_TARGET_STRIKE` (fake lightning in front of player) duration specifically.
- Kept fake lightning visual-only behavior safe (no damage/fire).

## Structure Generation

- Improved natural structure generation stability to avoid sudden "pop-in" structures in already explored terrain:
  - automatic generation now prioritizes fresh chunk contexts (newly visited/low-inhabited chunks),
  - debug/forced generation commands remain available.
