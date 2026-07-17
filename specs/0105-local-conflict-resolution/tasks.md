# Tarefas: Resolução local de conflitos

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [x] **Mesclar por UUID e atualização mais recente** (test-type: unit)
  - blocked-by: spec 0102
  - summary: inserir UUID novo e usar `updatedAt` para versões distintas.
  - desired behavior: o merge atual mantém a versão temporalmente mais recente.
  - acceptance criteria: comportamento existe no fluxo atual de importação.
  - verification: `./gradlew test`

- [x] **Registrar metadados da sincronização** (test-type: integration)
  - blocked-by: merge existente
  - summary: gravar `SyncLog` ao importar dados.
  - desired behavior: a operação deixa um registro local de auditoria.
  - acceptance criteria: o fluxo atual persiste `SyncLog`.
  - verification: `./gradlew test`

- [ ] **Decidir desempate para timestamps iguais** (test-type: unit)
  - blocked-by: testes de caracterização
  - summary: selecionar uma chave estável e documentar a ordem total.
  - desired behavior: ambos os dispositivos escolhem a mesma versão com payloads diferentes.
  - acceptance criteria: decisão cobre versão ativa/deletada e passa nas duas ordens de entrada.
  - verification: `./gradlew test`

- [ ] **Extrair e cobrir um resolver único** (test-type: unit)
  - blocked-by: desempate decidido
  - summary: centralizar inserção, edição, soft delete e empate em função pura.
  - desired behavior: resolver é determinístico, idempotente e simétrico.
  - acceptance criteria: matriz completa, repetição e ordem inversa passam.
  - verification: `./gradlew test`

- [ ] **Aplicar lote e log na mesma transação** (test-type: integration)
  - blocked-by: resolver único
  - summary: integrar o resolver à spec 0102 com rollback consistente.
  - desired behavior: falha não deixa entidades aplicadas sem log nem log sem entidades.
  - acceptance criteria: teste com falha injetada reverte todo o lote.
  - verification: `./gradlew test`

- [ ] **Exibir histórico local de sincronizações** (test-type: both)
  - blocked-by: transação com log
  - summary: listar peer, horário, tipo e contadores sem conteúdo clínico.
  - desired behavior: pessoa consegue confirmar e diagnosticar sincronizações recentes.
  - acceptance criteria: tela mostra dados corretos, vazios e erros de forma acessível.
  - verification: `./gradlew test`

- [ ] **Validar convergência em dois dispositivos** (test-type: integration)
  - blocked-by: resolver único; transação com log
  - summary: exercitar edição/edição, edição/delete, delete/delete e empate.
  - desired behavior: os dois bancos terminam iguais após transferências nas duas direções.
  - acceptance criteria: todos os cenários convergem sem perda silenciosa.
  - verification: `./gradlew assembleDebug && ./gradlew installDebug`
