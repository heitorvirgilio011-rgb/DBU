# NOVA — HD texture preview (Minecraft Java 1.20.1 / Forge)

Standalone **experimental** resource pack, intentionally isolated under the preview namespace `dbu_nova_preview`. It must remain **external and optional**; never place it in the DBU JAR. No runtime texture IDs have been approved yet.

## Delivered

- `previews/DBU_NOVA_HD_Preview_1.20.1.zip`: installable ZIP, `pack.mcmeta` format 15, separate from the mod.
- Three original classic-wide-arm HD 128×128 skin textures (`aurora`, `ember`, `verdant`).
- One original 128×128 4×4 textile swatch atlas, 32×32 cells, and a 64×64 pack icon.
- `uv_contract_draft.json`: draft mapping only, no established renderer or model contracts.
- `STATIC_QA_REPORT.md`: verified static checks and explicitly untested runtime scenarios.

Install the ZIP in `.minecraft/resourcepacks/` and optionally enable it. Preview resources are *not wired* to existing DBU entity renderers; enabling the pack alone is not proof of an in-game visible change. The pack does not override any `minecraft:` textures.

## Coordination and blockers

- **ULTRON:** approve visual direction and texture paths.
- **ECHO:** agree on model UV maps; classic skin UV in this preview is only a proposal.
- **JARVIS/VISION:** define custom-client renderer lookup and any supported HD textures.
- **Codex:** verify enabled / disabled / absent pack, Forge 1.20.1, multiplayer, existing saves, and DBC controls.
- No mod JAR, gameplay, HUD, network, save, race, transformation, or input code changed.

Generated textures are original procedural artwork. The matching generator and test source bundle is provided in the NOVA handoff artifact; it can be moved into this directory after ULTRON reviews the draft.
