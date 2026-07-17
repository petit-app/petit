# Tarefas: Restaurar Backup

> Status da spec: **On Hold**. Todas as tarefas permanecem pendentes até aprovação explícita.

## Tarefas

- [ ] **Cenário 1: Restaurar backup com sucesso** (test-type: both)
  - blocked-by: spec 0301
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 1: Restaurar backup com sucesso” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que estou logado com Google E tenho backups salvos no Google Drive QUANDO acesso "Backups salvos" E seleciono um backup para restaurar E confirmo a restauração ENTÃO vejo progresso de download E os dados são restaurados no banco local E vejo mensagem "Dados restaurados com sucesso"
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 2: Restaurar em dispositivo novo** (test-type: both)
  - blocked-by: spec 0301; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 2: Restaurar em dispositivo novo” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que instalei o app em um novo celular E fiz login com minha conta Google QUANDO acesso "Restaurar de backup" ENTÃO vejo lista de backups disponíveis E posso selecionar qual restaurar
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 3: Restaurar substitui dados locais** (test-type: both)
  - blocked-by: spec 0301; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 3: Restaurar substitui dados locais” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que tenho dados locais E restauro um backup QUANDO confirmo "Substituir dados locais" ENTÃO TODOS os dados locais são apagados E os dados do backup são importados E vejo os dados do backup na home
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 4: Restaurar com merge** (test-type: both)
  - blocked-by: spec 0301; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 4: Restaurar com merge” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que tenho dados locais E restauro um backup QUANDO escolho "Mesclar com dados locais" ENTÃO dados são mesclados (last-write-wins) E dados únicos de ambas fontes são mantidos
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 5: Restaurar sem backups** (test-type: both)
  - blocked-by: spec 0301; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 5: Restaurar sem backups” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que não tenho backups no Google Drive QUANDO acesso "Backups salvos" ENTÃO vejo mensagem "Nenhum backup encontrado" E vejo sugestão para fazer primeiro backup
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 6: Erro de download** (test-type: both)
  - blocked-by: spec 0301; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 6: Erro de download” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que seleciono um backup para restaurar QUANDO a conexão falha durante download ENTÃO vejo mensagem de erro E os dados locais não são alterados E posso tentar novamente ---
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`
