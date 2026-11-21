# ORACLE — proposta isolada: regras de transformações (r4)

**Líder para revisão:** EDITH (gameplay). **Integração final:** Codex/FORGE. Referência: issue #1, `DragonBallUltimate-1.5.4-r4-animacoes-fonte-experimental.zip` da Library.

## O que foi entregue

- `src/com/dragonballultimate/data/TransformationRules.java`: lógica pura Java 17 para verificar nível/desbloqueio, compatibilidade racial, liberação e Ki antes de ativar ou manter uma forma.
- `tools/TransformationRulesSmokeTest.java`: matriz de testes para todas as raças/formas da r4, níveis, unlock, Ki e liberação.
- `patches/PlayerProgression-r4.patch`: proposta de integração **apenas** em `src/main/java/com/dragonballultimate/data/PlayerProgression.java` da fonte r4, NÃO aplicada no repositório.
- `QA-2026-11-21.md`: comandos, resultados, limites e pendências verificáveis.

## Comportamento proposto

- BASE sempre permitida, inclusive para destransformação sem Ki.
- Kaioken mantém nível 3 ou unlock, 5% de liberação, pelo menos 2× custo de Ki ao ativar e 3 Ki/s para manter.
- Super Saiyajin mantém nível 8 ou unlock, restrito a Saiyajin/Híbrido; pelo menos 2× custo de Ki ao ativar e 2 Ki/s para manter.
- Um unlock autorizado não remove a exigência racial.
- Estado de forma inválido carregado de um save (por raça/nível sem unlock) volta à forma base no próximo tick de progressão **sem zerar Ki**. Ki insuficiente mantém queda para forma base, Ki=0 e liberação zerada como na r4.
- IDs existentes e tags de save são mantidos. Sem novas teclas, packet IDs, efeitos visuais, reequilíbrio de números, recursos proprietários ou dependência do pack HD opcional.

## Para aplicar *depois da revisão* de EDITH

Em uma **cópia** do projeto r4 (raiz `DragonBallUltimate/`):

```sh
cp agents/oracle/src/com/dragonballultimate/data/TransformationRules.java \
   src/main/java/com/dragonballultimate/data/
patch --dry-run -p1 < agents/oracle/patches/PlayerProgression-r4.patch
patch -p1 < agents/oracle/patches/PlayerProgression-r4.patch
javac --release 17 -d /tmp/dbu-oracle-test \
  src/main/java/com/dragonballultimate/data/{Race,Transformation,TransformationRules}.java \
  agents/oracle/tools/TransformationRulesSmokeTest.java
java -cp /tmp/dbu-oracle-test TransformationRulesSmokeTest
```

O patch parte exclusivamente da r4 experimental. Revalidar conflitos caso EDITH tenha alterado `PlayerProgression` desde a r4. **Não mesclar na `main` sem revisão de EDITH/Codex.**

## Pendências

1. EDITH confirmar regra de invalidação de formas persistidas e comportamento no esgotamento de Ki.
2. Codex importar fonte principal, aplicar patch, compilar com **Forge 47.3.0/Java 17** e validar rede/recursos.
3. SENTINEL validar mundo antigo, reconexão, troca de raça, multiplayer cliente-servidor, comandos e atalhos DBC; pack HD ausente/desativado/ativado.
4. Escopo posterior de transformações canônicas, progressão e atributos **não iniciado** até validação de requisitos pelo líder.
