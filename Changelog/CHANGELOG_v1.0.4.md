# Echo Of The Void - Changelog v1.0.4

Date: 2026-03-11

## Structure Generation Pipeline (Major Rework)

- Reworked custom structure generation to follow a native Minecraft worldgen flow.
- Removed unstable dynamic structure pop-in behavior from runtime/tick-style generation.
- Migrated placement logic to the worldgen registry/data pipeline (`worldgen/structure` + `worldgen/structure_set`) so structures are generated with chunk generation like vanilla structures.
- Improved compatibility with vanilla `/locate structure` for custom structures through registered worldgen definitions.

## Stability Fixes

- Fixed critical world/server instability caused by structure generation timing in previous generation hooks.
- Prevented heavy structure placement work from blocking unsafe execution paths during generation.
- Resolved cases where structures concentrated near spawn and did not repeat naturally across the world.

## Locate & Debug Improvements

- Added custom debug locate command:
  - `/uncanny locate structure <id>`
- Added specific sub-variant locate targets for house-attached spiral structures:
  - `false_ascent_house`
  - `false_descent_house`
- Locate behavior now distinguishes generated house variants via dedicated markers instead of generic spiral markers.

## False Ascent / False Descent House Variant

- Increased house attachment chance from **15%** to **40%** for testing.
- Added dedicated world markers when a house is actually attached to:
  - False Ascent
  - False Descent
- This allows direct validation of the house-containing variants with debug locate.

