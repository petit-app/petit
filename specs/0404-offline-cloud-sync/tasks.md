# Tarefas: Sincronização Offline-First

> Status da spec: **On Hold**. Todas as tarefas permanecem pendentes até aprovação explícita.

## Tarefas

- [ ] **Cenário 1: Criar dados offline** (test-type: both)
  - blocked-by: spec 0401
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 1: Criar dados offline” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que estou sem internet QUANDO cadastro um novo pet "Mia" ENTÃO Mia é salva no Room (syncStatus = PENDING_SYNC) E Mia aparece na lista normalmente E vejo indicador "Pendente de sync" no item
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 2: Sync automático ao reconectar** (test-type: both)
  - blocked-by: spec 0401; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 2: Sync automático ao reconectar” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que tenho dados pendentes de sync E estou offline QUANDO a internet volta ENTÃO o sync é iniciado automaticamente E os dados pendentes são enviados E o syncStatus muda para SYNCED E o indicador de pendente desaparece
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 3: Múltiplas edições offline** (test-type: both)
  - blocked-by: spec 0401; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 3: Múltiplas edições offline” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que estou offline QUANDO faço várias edições: - Adiciono pet Mia - Adiciono pesagem para Mia - Edito nome de Luna para Luninha ENTÃO todas as edições são salvas localmente E todas ficam como PENDING_SYNC E ao reconectar, todas são enviadas
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 4: Conflito após voltar online** (test-type: both)
  - blocked-by: spec 0401; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 4: Conflito após voltar online” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que editei Luna offline (updatedAt = 1000) E outro dispositivo editou Luna online (updatedAt = 1500) QUANDO volto online e sincronizo ENTÃO a resolução de conflito acontece E a versão mais recente (1500) vence
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 5: Queue de sync persiste após fechar app** (test-type: both)
  - blocked-by: spec 0401; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 5: Queue de sync persiste após fechar app” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que fiz edições offline E fecho o app E reabro o app (ainda offline) ENTÃO as edições ainda estão PENDING_SYNC E ao reconectar, serão sincronizadas ---
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`
