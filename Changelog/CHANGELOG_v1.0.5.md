# Echo Of The Void - Changelog v1.0.5

Date: 2026-03-22

## Worldgen & Structure Pipeline

- Continued use of native structure pipeline (registered structure type + piece + `worldgen/structure` and `worldgen/structure_set` JSONs).
- Added/maintained dedicated worldgen entries for all uncanny structure families, so they are discoverable with vanilla locate data and generated at chunk creation.
- Added per-feature generation checks to keep structures terrain-coherent (especially dry-surface checks for surface families and vertical constraints for spiral families).
- Added stronger feature spacing controls in world markers:
  - minimum distance between same feature type,
  - minimum distance between any two uncanny features.
- Added distance-based spawn-rate scaling in structure worldgen (lower near origin, higher farther away).

## New/Expanded Structure Families

Implemented and integrated these families in generation + debug catalog:

- `Anechoic Cube`
- `Mimic Shelter` (+ multiple variants)
- `Glitched Shelter`
- `Patterned Grove`
- `Barren Grid`
- `False Descent`
- `False Ascent`
- `Isolation Cube`
- `Bell Shrine` (multiple variants)
- `Watching Tower` (multiple variants)
- `False Camp` (multiple variants)
- `Wrong Village House` (multiple variants)
- `Wrong Village Utility` (multiple variants)
- `Sinkhole / Missing Chunk` (multiple variants)
- `Observation Platform` (multiple variants)
- `Wrong Road Segment` (multiple variants)
- `False Entrance` (multiple variants)
- `Storage Shed` (multiple variants)
- `Secret House` debug generation entry

## Spiral Structures & Secret House Integration

- Reworked false spiral math and orientation logic for both ascent/descent patterns.
- Added stable "with house" variants:
  - `false_ascent_with_house`
  - `false_descent_with_house`
- Fixed house alignment at spiral endpoints so entry flow is coherent (door aligned at end of stair route).
- Maintained dedicated marker support for house-attached spiral variants for locate/debug workflows.

## Mimic Shelter & Related Structure Quality

- Expanded `Mimic Shelter` into multiple layout variants.
- Added dedicated variants with lore-supporting chest contexts (where relevant).
- Improved camp/entrance quality passes:
  - better bed placement in false camps,
  - removed repetitive/illogical central-door patterns in camp variants,
  - fixed stone stair entrance behavior to mirror proper false spiral logic.

## Secret House, Lore Chest & Anti-Repeat Logic

- Secret house chest interaction now includes robust server-side state flags to prevent duplicate reward loops in the same chest resolution path.
- Lore roll behavior includes:
  - chance-based roll for lore outcome,
  - per-player tracking of missing volumes,
  - fallback logic once all volumes are completed.
- Behind-you fallback remains intentionally rare when all tomes are collected.

## Lore System (Books)

- `Uncanny Lore Piece` is now a functional written-book style item.
- Added and wired full lore library through code-backed content generation.
- Total lore volumes available: **6**.
- Added debug support to grant all lore tomes quickly for QA.
- Added/kept lore integration in structure systems that are intended to feed narrative progression.

## New/Updated Blocks & Items (Post-1.0.4 Pass)

- Added/kept `Uncanny Block` usage for black visual language in structure content.
- Added/kept `Uncanny Void Door` integration for secret-house pathways.
- Added/kept:
  - `Uncanny Reality Shard`
  - `Uncanny Reality Shard Piece`
  - `Reality Cube`
  - `Uncanny Lore Piece`
- Updated black texture usage to use the project's custom black texture pipeline (`color_black` assets) where configured.

## Custom Structure Villagers

- Added/maintained dedicated entity: `uncanny_structure_villager`.
- Added 5 sound-profile variants tied to distorted villager audio sets:
  - Flat
  - Huge Long Wide
  - Huge Thin
  - Very Wide
  - Very Long
- Added profile-based visual scaling + matching collision resizing.
- Added spawn integration in wrong-village-house family with profile-to-variant mapping and spawn chance gating.
- Added random behavior-mode assignment at spawn:
  - Normal
  - Aggressive
  - Neutral
  - No AI
  - Follow
- Naming/display fix:
  - structure villagers now display as `Villager?` instead of internal/modded full naming when profession data updates.

## Devmenu / QA Workflow Improvements

- Devmenu structure category expanded with hierarchical family -> variant coverage.
- Added direct trigger entries for all major structure families and sub-variants.
- Added explicit entries for:
  - false spiral with house variants,
  - secret house generation.
- Added `Structure Villager?` devmenu group:
  - Spawn (Random Profile)
  - Forced profile spawns (Flat/Gigantic/Tall Thin/Very Wide/Very Long)
- Continued compatibility with QA status tracking workflow in devmenu.

## Locate & Debug Commands

- Maintained custom locate command path for uncanny structure markers:
  - `/uncanny locate structure <id>`
- Added/maintained canonical ids for spiral house variants:
  - `false_ascent_house`
  - `false_descent_house`
- Improved structure marker usage for testing generated outcomes with and without attached secret house branches.

## Terror (Special Entity) Follow-up

- Kept/iterated behavior as part of lore fallback flow:
  - player-facing event hook from lore fallback,
  - short oppressive interaction window,
  - movement/trigger flow linked to confrontation moment.
- Maintained associated eerie audio behavior integration during encounter flow.

## Weather Balancing (Heavy Visual Events)

- Added anti-spam duration governor for highly oppressive visual weather events:
  - `fog_breathing`
  - `fog_black`
  - `fog_static_wall`
  - `thunder_stroboscopic`
- New heavy-visual duration rules:
  - normal heavy-visual range: **30s to 3m**,
  - if previous heavy visual weather lasted **>= 2m**, next heavy visual weather is forced short (**10s to 20s**, capped at 20s).
- Added persistent world-state memory for the previous heavy-visual weather duration to keep behavior stable across reloads.

## Stability & Persistence

- Expanded saved world-state fields for newly added systems (weather anti-spam memory + ongoing structure marker/state support).
- Preserved debug/state consistency through server saves and reload cycles for major systems touched in this pass.

## Drops & Rewards

- `Pulse?` now has dedicated shard drop chances on death:
  - **10%** chance to drop `Uncanny Reality Shard`
  - **50%** chance to drop `Uncanny Reality Shard Piece`
- The two rolls are independent.
