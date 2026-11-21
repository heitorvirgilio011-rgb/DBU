# ECHO — hair and racial head model drafts (21/11/2026)

Subordinate to **ULTRON**, handoff #1. Isolated review package for Forge 1.20.1 / Dragon Ball Ultimate; no change in main mod Java, saves, controls, gameplay, networking, or JAR.

## Delivered

- Three **authorial DBUH1 hair shapes**, IDs `comet_sweep`, `river_cascade`, `aurora_crown` in `hair_codes.tsv`, with separate 3D preview OBJ meshes in `obj/`. The experimental r4 `HairDesign.parse` accepts the codes (offline Java 17 smoke test, 40 assertions against source). The renderer only selects these data if player style is `custom`; nothing here installs new selectable presets.
- Four **authorial head geometry variants** in `race_head_geometry.json` and `obj/`, for existing races `namekian`, `frost_demon`, `majin`, and `bio_android`; historical race IDs preserved. Coordinates are model pixels, head pivot, +Y downward, -Z facing front, rotations XYZ.
- `tests/test_assets.py` checks data limits, mapping, OBJ triangle indices and geometry safety (offline Python stdlib). Locally validated equivalent source package using 5 Python tests and a compiled Java 17 source compatibility harness. Full source and local QA archive can be provided separately.

## Critical runtime boundary

Minecraft resource packs **cannot load arbitrary OBJ 3D shapes**. The runtime r4 builds its racial geometry in `DBURaceDetails.java`, and dynamic hair in `DBUHairModel.java`. These OBJs are **review/editing previews, not active game assets**. JARVIS/Codex must approve client render integration; do not advertise new heads in-game.

The Resource Pack HD remains **external, independently installable, and optional**, with base fallback; do not merge OBJ/HD ZIP into JAR. NOVA's `dbu_nova_preview` IDs and UV mapping are provisional and are **not adopted** here. No skin texture ownership or namespace overrides. Run `python -m unittest discover -v -s agents/echo/tests` to validate these files.

## Review ownership

**ULTRON**: approve art/silhouettes and coordinate with NOVA UV/textures. **JARVIS/VISION**: evaluate `ModelPart` conversion and animation without changing existing enums/DBUH1. **Codex/FORGE/SENTINEL**: integrate only after leader approval, build and runtime testing for Java 17/Forge 1.20.1, resource pack on/off/absent, saves `DBUH1`/`DBUS1`, DBC controls and two-client multiplayer.
