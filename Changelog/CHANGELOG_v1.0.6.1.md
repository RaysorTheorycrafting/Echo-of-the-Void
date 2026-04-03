# Echo Of The Void - Changelog v1.0.6.1 (Hotfix)

Date: 2026-03-26

## Hotfix Summary

This hotfix focuses on behavior corrections for Sheep?, Watcher?, fleeing Specials, and Follower?.

## Fixes

### Sheep? - Glitched Chameleon visibility rule

- Fixed the color-shift condition for the sheep variant that changes wool color.
- The variant now changes color only when the sheep is not broadly visible to the player.
- This is no longer limited to strict direct crosshair focus checks.

### Watcher? - spawn distance and visibility

- Reworked watcher spawn selection to avoid close/visible spawns.
- Increased practical spawn distance window so watcher arrivals are farther from the player.
- Added strict out-of-view validation during spawn selection.
- Removed unsafe close fallback behavior; if no valid out-of-view location exists, watcher spawn now aborts cleanly.
- Kept the existing hidden/approach sequence behavior after spawn.

### Fleeing Specials in water

- Added anti-stuck behavior for fleeing specials when they enter water:
  - they now sink into the ground and despawn, instead of getting stuck pathing in water.
- Applied to fleeing states:
  - `Watcher?`
  - `Knocker?`
  - `Hurler?`
  - `Shadow?`
  - `Follower?` when in flee mode
- Explicitly not applied to non-fleeing behavior such as `Usher?` guidance and `Attacker?` combat behavior.

### Follower? behavior update

- Added new stealth approach phase:
  - if the player does not see the follower, it approaches very slowly.
- If the player sees the follower (broad visibility, not only direct look), follower returns to its normal distance-preserving behavior.
- Added contact-triggered escalation:
  - on contact while in stealth approach, follower plays a Hurler-style scream and switches to attack behavior.
