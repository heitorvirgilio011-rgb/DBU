# Verificação SAGA · 21/11/2026

A cópia local `DragonBallUltimate-1.5.4-r4-fonte.zip` tinha SHA-256 `f7aee5ae96ab9bf151aa8576dd8c17ae0a2243d60fd7635cdc783f904797c753`. Foi usada como base de teste em uma extração separada. Seu SHA difere do ZIP r4 citado por [FORGE no PR #7](https://github.com/heitorvirgilio011-rgb/DBU/pull/7).

Ambiente verificado no Windows com Microsoft OpenJDK 17.0.15, Gradle 8.8, ForgeGradle 6.0.54 e Minecraft 1.20.1 / Forge 47.3.0. `Apply-Overlay.ps1` executou duas vezes na mesma cópia. A segunda aplicação aceitou arquivos idênticos. Dos arquivos preexistentes em `src/main`, apenas os dois JSONs de idioma tiveram diferenças. Cada um manteve os 24 pares chave/valor originais e ganhou 19 chaves de SAGA.

## Reprodução

Depois de aplicar a camada conforme o [README](README.md), rode a partir da raiz da fonte do mod, com `JAVA_HOME` apontado para o JDK 17.

```powershell
.\gradlew.bat -I 'C:/caminho/para/DBU/agents/saga/Verify-Overlay.init.gradle' build verifySaga
```

Na execução local, Gradle usou o cache existente em modo offline. A primeira tentativa restrita parou ao acessar um JAR do cache. A nova execução com acesso ao cache compilou, executou os testes e concluiu a tarefa de remapeamento `reobfJar`. Trecho da saída real:

```text
> Task :reobfJar
> Task :build
> Task :runSagaProgressSmoke
SagaProgressSmoke: 85 checks passed
> Task :runSagaReloadSmoke
SagaReloadSmoke: 49 checks passed
> Task :runSagaSavedDataSmoke
SagaSavedDataSmoke: 43 checks passed
> Task :verifySaga
BUILD SUCCESSFUL in 32s
12 actionable tasks: 12 executed
```

O JAR local `dragonballultimate-1.5.4-r4.jar` continha as classes de SAGA e os três JSONs de missões, com zero ZIPs aninhados. SHA-256 local `aca20b6510f2ab59a2ec2fcfc55b5fdd4a6835c49750a6976d7e11cd21aae576`.

| Verificação | Evidência |
|---|---|
| Sequência, isolamento e repetição | 85 verificações em Java 17. Dois UUIDs simulados, objetivos fora de ordem, cliques repetidos, entrega única, marcos, ausência de definição, revisão incompatível e snapshots de retomada. |
| Catálogo e idiomas | 49 verificações com JSONs do protótipo e bibliotecas Forge. Sete entradas inválidas foram omitidas com avisos, uma entrada independente foi mantida e um recarregamento para catálogo vazio substituiu a visão anterior. Paridade PT-BR/EN-US e argumentos de mensagens conferidos. |
| Persistência NBT | 43 verificações com compressão e leitura NBT do Minecraft. Inclui dois UUIDs, recibos com e sem marco, salvamento durante estágio, reentrega, versão futura, tipos inválidos e UUID duplicado. As quatro amostras inválidas produziram os diagnósticos esperados de desativação. |
| Limites da integração | Conferência de arquivos e do JAR. KeyMappings, rede, Ki, DBUH1, DBUS1 e recursos visuais existentes conservam os mesmos arquivos da fonte testada. |

## Aceite em mundo descartável · pendente

Os testes acima executam fora de uma sessão do Minecraft. A tabela descreve a validação de jogo que Codex/SENTINEL deve registrar antes de integrar. Preserve cópias de saves e registre o hash do JAR usado, a versão do servidor e o trecho de log de cada caso.

| Caso | Procedimento e resultado esperado |
|---|---|
| Dois clientes | A em PT-BR e B em EN-US usam o mesmo Lume. A faz a primeira missão e B aborda o vigia fora de ordem. Só A deve avançar, receber seu diálogo de resultado e acumular um selo. B mantém sequência própria. |
| Retomada e recibo | Complete um objetivo, rode `/save-all flush`, desligue e reabra o mundo. Continue a etapa correta. Grave após a primeira entrega, reinicie e repita a interação. O recibo de Mapa dos Ventos deve continuar único. |
| Conteúdo ausente ou inválido | Em cópia do mundo, sobreponha uma definição com JSON inválido ou mude sua revisão e rode `/reload`. Capture o aviso, confirme progresso preservado e retome com a definição original. Faça o mesmo com uma cadeia de pré-requisito ausente. |
| Arquivo de saga ilegível | Em cópia separada, torne apenas `dbu_saga.dat` ilegível. O log deve indicar falha; o arquivo deve continuar presente sem regravação pela saga. |
| Save anterior, DBC e formatos | Abra um backup sem `dbu_saga.dat`, confirme o estado inicial e compare dados do personagem, remapeamentos, rede DBU e importação de códigos DBUH1/DBUS1 antes e depois. Confirme interação configurada no Minecraft e ausência de novos atalhos. |
| Pack HD opcional | Repita a interação com pack ausente, instalado e desativado, e ativo. O diálogo e o aldeão padrão devem funcionar em todos os estados, com servidor configurado sem exigir o ZIP. |
