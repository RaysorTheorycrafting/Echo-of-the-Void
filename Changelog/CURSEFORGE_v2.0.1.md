# Echo of the Void 2.0.1

Version 2.0.1 is a compatibility hotfix for existing worlds.

It fixes a critical issue where restarting a world during a Tension Builder sequence could preserve a session-relative deadline. Because Minecraft's server tick counter restarts with each session, this could leave events, weather and Special encounters silently paused for an excessive duration, repeatedly, on older saves.

The hotfix safely migrates those timers when a world loads:

- valid remaining delays continue from the correct duration;
- impossible or stale locks are removed;
- related pending Grand Warden state is cleaned up consistently;
- the repair is saved and does not recur on the next restart.

No content, balancing, public identifier, configuration key or network protocol has changed.

Back up important modded worlds before updating.

## File information

- Game version: Minecraft 1.21.1
- Mod loader: NeoForge 21.1.219 or newer in the 21.1 line
- Java: 21
- File type: Release
- Environment: Client and dedicated server
- Required dependencies: NeoForge only; no external content mod is required

Install the same `EchoOfTheVoid-2.0.1.jar` on the server and every connecting client.
