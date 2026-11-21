# SENTINEL — Matriz de QA de regressão (proposta, 21/11/2026)

**Escopo:** Dragon Ball Ultimate, Minecraft Java 1.20.1, Forge/Java 17. Coordenação: Codex; integração somente após revisão. Fonte do contrato: issue #1. O código principal do mod ainda não está na branch main; **todos os casos de execução Forge abaixo estão PENDENTES, e não APROVADOS**.

## Gate estático executável — sem Minecraft

- Executar: python -m unittest discover -s agents/sentinel -p 'test_pack_gate.py' -v
- Verificar ZIP local de preview externo: python agents/sentinel/pack_gate.py --pack /caminho/pack.zip
- Quando a fonte Forge estiver importada: python agents/sentinel/pack_gate.py --mod-root /caminho/DBU
- Em ambiente Forge completo: combinar as duas flags para testar ambos os limites estáticos.
- **Limitação:** valida pack.mcmeta, entradas ZIP, CRC de ZIP, header/dimensões PNG e ausência de .zip/.jar em src/main/resources. Não verifica CRC interno dos PNGs, renderização, funcionalidade do pack, handshake, saves, multiplayer nem controles DBC. O veto aos namespaces minecraft e forge é política para o **pack de preview**, sujeita a decisão de ULTRON/Codex sobre IDs definitivos.

## Matriz de execução in-game — somente após receber um build

| Caso | Preparação | Procedimento/resultado esperado | Situação |
|---|---|---|---|
| MP-01 | Servidor dedicado Forge + clientes A/B; sem pack instalado | Conectar ambos; criação/join de mundo, HUD básico, transformações funcionam; sem download obrigatório | PENDENTE |
| MP-02 | Servidor dedicado sem pack; A com pack local ativo, B sem pack | Ambos conectam; estado servidor é igual; somente representação visual local varia | PENDENTE |
| MP-03 | Mesmo mundo, cliente A com pack desativado após estar ativo | Recarregar assets, retornar ao jogo sem desconexão, perda de input ou crash | PENDENTE |
| MP-04 | Dois clientes com ações simultâneas | Exercitar Ki, combate/defesa, transformações e replicação; registrar logs, posição, valores do servidor e dos clientes | PENDENTE |
| SAVE-01 | Backup de mundo novo com IDs estáveis | Salvar, fechar e reabrir; comparar raça, atributos, Ki, progressão, itens e inventário | PENDENTE |
| SAVE-02 | Cópia isolada de save anterior 1.5.4-r4, jamais o original | Abrir somente em cópia; testar migração/retorno e comparar NBT nos campos conhecidos; documentar incompatibilidades | PENDENTE |
| SAVE-03 | Mundo salvo com pack HD ativo | Remover o pack local, carregar save e continuar sem perda/alteração de dados de gameplay | PENDENTE |
| INPUT-01 | Mapeamento original DBC documentado + combinações de teclas | Testar teclado e conflitos de keybindings, Ki, transformações e controles DBC; comparar antes/depois | PENDENTE |
| NET-01 | Registro de handshake e mensagens DBUH1/DBUS1 | Comparar versão de protocolo, direção, validação de payload, desconexão e reconexão sem duplicação de estado | PENDENTE |
| NET-02 | A/B em latência induzida (ambiente de testes controlado) | Registrar ações, latência, reconciliação, estado autoritativo e eventuais dessyncs; não inventar limites | PENDENTE |
| PERF-01 | Cenário estático igual com 0/1/2 jogadores | Aquecer 120 s, amostrar 300 s, repetir 3 vezes; reportar TPS, MSPT p95, heap, GC e logs | PENDENTE |
| PERF-02 | Mesmo hardware/render distance; pack ausente/desativado/ativo | 120 s aquecimento + 300 s coleta, três rodadas; reportar FPS médio e 1% low, VRAM, travamentos, tempo de carregamento | PENDENTE |
| REG-01 | Builds conhecidos de EDITH/JARVIS/ULTRON/Codex | Confirmar que alterações em uma área não mudam hashes/contratos de rede, saves, controles DBC ou IDs sem aprovação | PENDENTE |

### Evidência obrigatória por caso

Registrar commit SHA/build hash, versão exata Forge/Java, sistema operacional, mods/dependências, configuração do mundo, seed (quando aplicável), modo pack (ausente/desativado/ativo), log do cliente e servidor, captura ou relatório do profiler, resultados observados, passos de reprodução e decisão de aprovação de Codex. Não usar saves pessoais originais: trabalhar com cópias verificáveis e backups.

### Gates de promoção sugeridos ao coordenador

1. O gate estático deve passar no artefato real e em uma árvore Forge real.
2. Nenhum caso bloqueante de save, protocolo, multiplayer ou DBC pode ser classificado como aprovado sem evidência.
3. Resource pack HD continua como ZIP externo e opcional, sem conteúdo obrigatório no JAR ou download imposto pelo servidor.
4. Limiares de FPS/MSPT são definidos pelo responsável após baseline reproduzível; não há promessa atual de desempenho.
