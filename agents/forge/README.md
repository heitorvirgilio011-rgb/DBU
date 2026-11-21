# FORGE — proposta isolada para Codex

Escopo: build, verificacao estatica de integracao e CI para DBU (Minecraft 1.20.1 / Forge 47.3.0 / Java 17).

**Este diretorio nao contem o mod, nao altera main nem habilita workflow executavel antes de revisao de Codex.** A fonte experimental r4 esta na Library e sua importacao cabe a Codex.

## Conteudo

- scripts/verify_source_layout.py: verifica um projeto Gradle em pasta ou ZIP-fonte; exige ForgeGradle 6.0.54, Gradle 8.8, toolchain Java 17, Forge 47.3.0, recursos minimos e marcadores DBUH1/DBUS1. Rejeita ZIPs/resourcepacks dentro de src/main/resources. O modo --jar verifica integridade estrutural do JAR e rejeita pack HD compactado dentro dele.
- tests/test_verify_source_layout.py: 11 testes unitarios independentes de Forge, com casos positivos e negativos.
- proposals/forge-build.yml: proposta de GitHub Actions, a ser copiada para .github/workflows/ somente apos importacao da fonte principal e revisao por Codex.
- QA-2026-11-21.md: evidencias locais e pendencias.

## Execucao

No projeto que inclua a pasta agents/forge, executar:

    python3 -m unittest discover -s agents/forge/tests -v
    python3 agents/forge/scripts/verify_source_layout.py --source /caminho/DragonBallUltimate
    python3 agents/forge/scripts/verify_source_layout.py --source /caminho/DragonBallUltimate-1.5.4-r4-animacoes-fonte-experimental.zip

Somente depois de compilar um JAR real:

    python3 agents/forge/scripts/verify_source_layout.py --jar build/libs/dragonballultimate-1.5.4.jar

A presenca de DBUH1/DBUS1 prova apenas marcadores, nao retrocompatibilidade de saves ou consistencia da rede. O pack.mcmeta de recursos MINIMOS dentro do mod e normal; o ZIP HD opcional permanece fora do JAR.

## Integracao coordenada

1. Codex importa a fonte principal aprovada em branch independente.
2. Codex revisa o YAML e o nome do artefato, e entao ativa workflow na pasta oficial.
3. GitHub Actions deve gerar logs, commit SHA e JAR candidato; compilacao nao substitui testes reais de gameplay.
4. SENTINEL valida saves, multiplayer, servidor dedicado, controles DBC e pack HD ausente/desligado/ativado. JARVIS/ULTRON validam recursos basicos e fallback.

Coordenacao oficial: https://github.com/heitorvirgilio011-rgb/DBU/issues/1
