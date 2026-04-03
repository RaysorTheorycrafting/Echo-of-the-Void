# Echo Of The Void - Changelog v1.1.0.1

Date: 2026-04-03

## Fixed: Specials and Grand Warden can no longer use boats

- Added a hard mount guard that blocks boat mounting for:
  - all entities in `UncannyEntityRegistry.isSpecialEntity(...)`,
  - Grand Warden identified by `eotv_grand_warden`.
- Added a defensive tick fallback that immediately dismounts restricted entities if they are forced into a boat by external systems.

