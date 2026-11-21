# ATLAS — guarda por impacto (proposta para revisão de EDITH)

Área exclusiva: combate, defesa e balanceamento. Referência: [handoff #1](https://github.com/heitorvirgilio011-rgb/DBU/issues/1). Esta branch não integra nem altera o jogo existente.

## Problema observado na fonte r4

No experimento 1.5.4-r4 da Library, PlayerProgression.guard() gasta **3 de fôlego para abrir uma janela de 6 ticks** e GuardBalance.reducedDamage(...) aplica **33%–52% de redução** de dano conforme o fôlego. Golpes múltiplos durante a janela não gastam mais fôlego individualmente. Isso é uma **hipótese de desequilíbrio**, não um exploit confirmado.

## Proposta isolada

GuardImpactBudget.resolve(incoming, fullyGuarded, availableStamina) recebe os valores pré-computados no servidor, sem duplicar ou substituir a curva atual. Política **experimental, não aprovada**: gastar 2 pontos de fôlego por unidade de dano evitado, arredondando para cima. Se o fôlego for insuficiente, a redução do dano diminui proporcionalmente. Entradas inválidas não geram NaN nem imunidade gratuita.

| Dano recebido | Dano após GuardBalance | Fôlego disponível | Dano proposto | Gasto extra |
|---:|---:|---:|---:|---:|
| 10 | 4,8 | 100 | 4,8 | 11 |
| 10 | 4,8 | 5 | ~7,636 | 5 |
| 10 | 4,8 | 0 | 10 | 0 |

Somente cálculo puro Java 17 e testes offline. Sem dependência de Forge, cliente, protocolo, resource pack, IDs, NBT, DBUH1/DBUS1 ou controles DBC.

## Sugestão de integração (NÃO aplicada)

Em PlayerProgressionEvents.onLivingHurt, depois da defesa racial/classe e apenas se a defesa existente estiver ativa no servidor: calcular guarded = GuardBalance.reducedDamage(incoming, stamina, maxStamina); executar GuardImpactBudget.resolve(...); gastar a quantidade de staminaSpent() via PlayerProgression.spendStamina(...); aplicar o novo dano somente se a despesa de fôlego tiver sucesso. A função de guard() e o atalho **Z** devem permanecer inalterados.

EDITH precisa aprovar custo duplo (ativação + impacto), limites, prioridade da defesa e combinação PvP/PvE. Codex/FORGE fará a integração na fonte oficial após revisão. SENTINEL executará QA de multiplayer, saves e compatibilidade DBC.

## Reproduzir os testes

Teste standalone (necessita JDK com compilação Java 17):

    mkdir -p /tmp/dbu-atlas-test
    javac --release 17 -Xlint:all -d /tmp/dbu-atlas-test agents/atlas/src/com/dragonballultimate/combat/GuardImpactBudget.java agents/atlas/tests/GuardImpactBudgetSmokeTest.java
    java -cp /tmp/dbu-atlas-test GuardImpactBudgetSmokeTest

Teste opcional de compatibilidade com a fonte experimental r4, obtida separadamente na Library:

    javac --release 17 -Xlint:all -d /tmp/dbu-atlas-test /path/to/r4/src/main/java/com/dragonballultimate/combat/GuardBalance.java agents/atlas/src/com/dragonballultimate/combat/GuardImpactBudget.java agents/atlas/tests/GuardImpactR4CompatibilityTest.java
    java -cp /tmp/dbu-atlas-test GuardImpactR4CompatibilityTest

Consulte QA-2026-11-21.md para as execuções reais e limitações. Resource Pack HD permanece externo e opcional.
