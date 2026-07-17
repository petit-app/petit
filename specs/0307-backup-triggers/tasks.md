# Tarefas: Gatilhos de Backup

> Status da spec: **On Hold**. Todas as tarefas permanecem pendentes até aprovação explícita.

## Tarefas

- [ ] **Cenário 1: Backup após criar pet** (test-type: both)
  - blocked-by: spec 0305, spec 0306
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 1: Backup após criar pet” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que backup automático está ativado E tenho conexão de internet QUANDO cadastro um novo pet ENTÃO após 5 minutos de inatividade O backup é executado automaticamente E inclui o novo pet
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 2: Debounce de múltiplas alterações** (test-type: both)
  - blocked-by: spec 0305, spec 0306; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 2: Debounce de múltiplas alterações” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que faço várias alterações em sequência: - Adiciono pet Luna - Adiciono pesagem 3.5kg - Adiciono vacina V3 - Tudo em menos de 5 minutos ENTÃO apenas UM backup é executado (após 5 minutos da última alteração) E inclui todas as mudanças
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 3: Backup após delete** (test-type: both)
  - blocked-by: spec 0305, spec 0306; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 3: Backup após delete” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que backup automático está ativado QUANDO deleto um pet ENTÃO após 5 minutos sem alterações O backup é executado E reflete a exclusão
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 4: Cancelar backup pendente** (test-type: both)
  - blocked-by: spec 0305, spec 0306; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 4: Cancelar backup pendente” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que alterei dados e backup está pendente (em 3 min) QUANDO faço outra alteração ENTÃO o timer é resetado para 5 minutos novamente E apenas um backup será feito
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 5: Não duplicar com backup periódico** (test-type: both)
  - blocked-by: spec 0305, spec 0306; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 5: Não duplicar com backup periódico” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que um backup por alteração está pendente E o backup periódico deveria executar agora ENTÃO apenas um backup é feito E o timer de backup por alteração é cancelado
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 6: App fechado após alteração** (test-type: both)
  - blocked-by: spec 0305, spec 0306; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 6: App fechado após alteração” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que fiz alterações E fecho o app imediatamente ENTÃO o backup pendente ainda será executado (WorkManager persiste a tarefa) ---
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`
