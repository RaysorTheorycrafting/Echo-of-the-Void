# Echo of the Void - Changelog v2.2.0

Date: 2026-10-09

Details are kept vague on purpose, most of this is better found in game.

## New content

- A few new creatures. One of them is very hard to look at, one of them waits for you to fall asleep, and one of them only ever sits up high and watches.
- New events. Your world will start rearranging itself a little when you're not looking, and some things won't be where you left them.
- Your other worlds might matter more than you think.

## Combat

- A pair of hunters was reworked from scratch. They are no stronger than before, they just work together now.
- Some creatures fight a lot more like a player would.
- New sounds for a few fights that were too quiet.

## Balancing and polish

- Some creatures were faster or hit harder than they were supposed to. That's fixed.
- A few things you could exploit with water no longer work as well.

## Fixes

- A hunter could sometimes follow you around without ever attacking.
- Some far away spawns were silently thrown away.
- Lots of small AI, water and pathfinding fixes.

## Note

- One of the new events looks at which players appear in your other local worlds. Nothing is uploaded. If a name isn't known locally, the game asks Mojang for it, the same way it already does for skins. You can turn this event off with `oldFriendEnabled` in the common config.

## Compatibility

- Minecraft 1.21.1
- NeoForge 21.1.219+
- Java 21
- Client and dedicated server, same file on both.
- Worlds from 2.1.0 and earlier stay compatible. No ID, command, config key, network protocol or save data key was removed or renamed.
- Back up your worlds before updating.
