# NOVA static QA — 2026-11-21

**Result: PASS (static checks only).**

\`previews/DBU_NOVA_HD_Preview_1.20.1.zip\`

SHA-256: \`3b998a9950b446031ce29c2913e615d1f2f74e7237524bcc9e6f71e746b4c143\`

- ZIP entries intact, no duplicate or traversing paths.
- Exactly six files: \`pack.mcmeta\`, \`pack.png\`, three skin textures, one cloth atlas.
- \`pack.mcmeta\` has \`pack_format = 15\` (Minecraft 1.20.1).
- Five PNGs have valid signature, chunk CRCs, RGBA8, and expected dimensions.
- Skin texture base-face UV regions are fully opaque.
- No overrides of vanilla assets or other agents' runtime namespaces.

**Not tested; must not be claimed as validated:** Forge startup, in-game rendering, enabling/disabling/absence of the pack, server/client multiplayer, existing saves, DBUH1/DBUS1 or DBC controls.

Local deterministic generator and static test scripts are included in the separate source handoff ZIP provided to the user. This GitHub branch intentionally contains only preview assets and review documentation until source inclusion is approved.
