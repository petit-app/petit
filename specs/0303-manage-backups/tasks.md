# Tarefas: Gerenciar Backups

> Status da spec: **On Hold**. Todas as tarefas permanecem pendentes até aprovação explícita.

## Tarefas

- [ ] **Cenário 1: Ver lista de backups** (test-type: both)
  - blocked-by: spec 0301
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 1: Ver lista de backups” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que estou logado com Google E tenho múltiplos backups salvos QUANDO acesso "Backups salvos" ENTÃO vejo lista com todos os backups E cada item mostra: - Data e hora do backup - Quantidade de pets - Tamanho do arquivo - Versão do app
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 2: Ver detalhes do backup** (test-type: both)
  - blocked-by: spec 0301; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 2: Ver detalhes do backup” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que estou na lista de backups QUANDO toco em um backup ENTÃO vejo detalhes completos: - Data e hora - Conteúdo (X pets, Y pesagens, Z vacinas) - Tamanho - Versão do app que criou E vejo opções: Restaurar, Deletar
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 3: Deletar backup específico** (test-type: both)
  - blocked-by: spec 0301; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 3: Deletar backup específico” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que estou nos detalhes de um backup QUANDO toco em "Deletar" E confirmo a exclusão ENTÃO o backup é removido do Google Drive E não aparece mais na lista
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 4: Deletar múltiplos backups** (test-type: both)
  - blocked-by: spec 0301; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 4: Deletar múltiplos backups” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que estou na lista de backups QUANDO ativo modo de seleção (long press) E seleciono múltiplos backups E toco em "Deletar selecionados" E confirmo ENTÃO todos os backups selecionados são removidos
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 5: Limite de backups manuais** (test-type: both)
  - blocked-by: spec 0301; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 5: Limite de backups manuais” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que tenho 10 backups manuais salvos (limite) QUANDO faço um novo backup manual ENTÃO o backup manual mais antigo é removido automaticamente E o novo backup é adicionado E vejo notificação "Backup antigo removido para liberar espaço"
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 7: Backups após exclusão de conta** (test-type: both)
  - blocked-by: spec 0301; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 7: Backups após exclusão de conta” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que tenho backups salvos no Google Drive QUANDO excluo minha conta do app ENTÃO os backups são mantidos por 90 dias (período de grace) E vejo aviso "Sua conta será permanentemente excluída em X dias." E após 90 dias sem reativação, os backups são purgados automaticamente
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 8: Backups após exclusão de conta** (test-type: both)
  - blocked-by: spec 0301; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 8: Backups após exclusão de conta” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que tenho backups salvos QUANDO deleto minha conta ENTÃO os backups são agendados para purge em 30 dias E após 30 dias, todos os arquivos no bucket do usuário são removidos permanentemente
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 6: Espaço total usado** (test-type: both)
  - blocked-by: spec 0301; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 6: Espaço total usado” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que estou na tela de backup QUANDO vejo a seção "Backups salvos" ENTÃO vejo o total de backups E o espaço total usado (ex: "3 backups • 45.2 KB") ---
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`
