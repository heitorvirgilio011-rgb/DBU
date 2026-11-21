# FRIDAY — HUD Ki/Liberação: proposta independente

**Para revisão de JARVIS e integração por Codex.** Não é um JAR instalável.

Base consultada: fonte experimental `DragonBallUltimate-1.5.4-r4-animacoes-fonte-experimental.zip` (Library), SHA-256 `2d9afe00412300234b476eed257cb4f24dfd2ec46be2031d90352e19952b6500`, e issue #1.

O método `DBUHud.drawUnifiedKi` da fonte r4 abrevia a liberação uma vez, mas não garante que a legenda restante caiba ao lado de valores extremos de Ki. A proposta adiciona `HudTextLayout`, um utilitário puro Java 17 para medir textos com a fonte do cliente e selecionar uma versão sem sobreposição. **Não altera Ki real, BP, gameplay, saves, controles DBC, rede ou texturas.**

Arquivos:
- `src/HudTextLayout.java`: helper sem dependências Forge; exige o `HudMath` preexistente.
- `tests/HudTextLayoutSmokeTest.java`: 38.350 verificações de encaixe e formatação.
- `patches/0001-friday-hud-ki-labels-r4.patch`: patch aplicável à fonte r4; também adiciona o teste ao diretório `tools/` e muda apenas o desenho dos dois rótulos em `DBUHud`.
- `QA-2026-11-21.md`: execução verificável e pendências.

## Aplicação por JARVIS/Codex

Extrair uma cópia limpa da fonte 1.5.4-r4 e, **após aprovação**, executar na pasta `DragonBallUltimate/`:

```bash
patch --dry-run -p1 -i /caminho/0001-friday-hud-ki-labels-r4.patch
patch -p1 -i /caminho/0001-friday-hud-ki-labels-r4.patch
javac --release 17 -d /tmp/dbu-smoke src/main/java/com/dragonballultimate/client/HudMath.java src/main/java/com/dragonballultimate/client/HudTextLayout.java tools/HudMathSmokeTest.java tools/HudTextLayoutSmokeTest.java
java -cp /tmp/dbu-smoke HudTextLayoutSmokeTest
java -cp /tmp/dbu-smoke HudMathSmokeTest
```

**Não mesclar sem revisão JARVIS/Codex e build Forge real.** O Resource Pack HD de ULTRON continua externo e opcional, em `.minecraft/resourcepacks/`.
