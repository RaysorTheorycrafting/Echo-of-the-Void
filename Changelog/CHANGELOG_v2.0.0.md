# Echo Of The Void — Changelog v2.0.0

Date: 2026-08-26

## Added

- A campaign director for a complete standard adventure of approximately 50 Minecraft days.
- An optional extra-long 100-day campaign using the same normalized progression rather than a simple frequency multiplier.
- A complete late-campaign pacing arc with no intrusive victory counter or explanatory screen.
- Six discoverable journals with distinct voices, non-sequential discovery and time-weighted placement in lore chests.
- A fully rebuilt development menu with search, filters, metadata, favorites, history, repeat testing and entity previews.
- New Minecraft-native ambient anomalies, localized weather phenomena, Vanilla-derived variants and approved Special encounters.
- Deterministic campaign and scheduler simulation covering all phases, five intensity profiles and six danger levels.
- Automated lifecycle, persistence, loot, sound, resource, dedicated-server and block-mutation safeguards.

## Changed

- The standard experience now targets 50 days; the former 100-day target remains available as an explicit extra-long option.
- Phase 2 pacing has more variety and less dependence on bell-related events while preserving the validated four-phase progression.
- Event selection now uses bounded anti-repetition memory and prevents independent scheduler lanes from producing accidental simultaneous strong scares.
- Journals have a 50% chance to appear in an eligible lore chest. A journal is recorded as discovered only after successful insertion.
- Grand Warden warnings now allow 5–7 seconds of reaction time, and a player-killed Grand Warden grants an important but bounded Reality Shard reward.
- Special rewards were normalized so every Special can rarely yield Reality Shard Pieces or Reality Shards without flooding progression.
- Physical sounds remain spatialized at their source; personal or mental sounds are delivered only to their intended player.
- Numerous Special behaviors, animations, pathfinding rules, despawn sequences and counter-play conditions were refined.
- Vanilla-derived `?` entities preserve their Vanilla loot, core sounds and baseline behavior unless a variant explicitly changes that behavior.
- Structure and village-house testing now exposes every intended variant through the same production builders.

## Fixed

- Dedicated servers no longer load client darkness Mixins through the common Mixin list.
- Uncanny Compass identity and structure tracking remain stable across inventory ticks and reloads.
- Altar blocks, block entities and coupled double blocks are protected from generic single-block mutation effects.
- Multiple event, Special, weather, audio and development-menu trigger paths which could fail silently or use the wrong delivery scope.
- Several entity lifecycle, loot-table path, hit reaction, navigation, animation and cleanup regressions found during the 2.0 test passes.

## Removed

- Forced Drop, Armor Break and Giant Sun, whose penalties or presentation did not fit the mod's fairness rules.
- Climber?, whose movement could not meet the required quality and counter-play standard.
- False Piston and Jukebox Afterbeat remain intentionally excluded rather than represented by misleading approximations.

## Compatibility

- Minecraft 1.21.1
- NeoForge 21.1.219+
- Java 21
- Client and dedicated server
- No external mod dependency
- Existing mod ID, registry IDs, commands, network protocol `"1"`, configuration keys, NBT keys and SavedData key remain compatible with 1.1.1 worlds.
