# Echo of the Void — Changelog v2.0.1

Date: 2026-08-26

## Fixed

- Fixed an old-world migration bug that could leave the Tension Builder permanently active after restarting an integrated or dedicated server.
- Events, ambient anomalies, weather and Special encounters can no longer remain silently paused by a deadline saved against a previous server session's tick counter.
- Valid active Tension Builder and Grand Warden delays are rebased onto the new server session; impossible or stale locks are safely cleared.
- Clearing an invalid delayed Grand Warden state now also clears its associated dimension, warning and forced-trigger flags.

## Compatibility

- Minecraft 1.21.1
- NeoForge 21.1.219+
- Java 21
- Client and dedicated server
- Existing 1.1.1 and 2.0.0 worlds remain compatible.
- No registry ID, command, configuration, network protocol, payload, NBT key or SavedData key was changed.

This hotfix contains no gameplay rebalance or new content.
