# Tarefas: Sincronização em Tempo Real

> Status da spec: **On Hold**. Todas as tarefas permanecem pendentes até aprovação explícita.

## Tarefas

- [ ] **Cenário 1: Sync após criar dados** (test-type: both)
  - blocked-by: spec 0201
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 1: Sync após criar dados” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que sou usuário premium com sync ativado E tenho conexão de internet QUANDO cadastro um novo pet "Luna" ENTÃO Luna é salva no Room imediatamente (syncStatus = PENDING) E após alguns segundos, Luna é enviada para o Firestore E o syncStatus muda para SYNCED E vejo indicador de sync ✓
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 2: Sync em tempo real recebendo dados** (test-type: both)
  - blocked-by: spec 0201; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 2: Sync em tempo real recebendo dados” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que tenho o app aberto E alguém (ou outro dispositivo) adiciona dados no Firestore QUANDO a mudança é detectada pelo snapshot listener do Firestore ENTÃO os novos dados são baixados automaticamente E salvos no Room local E aparecem na UI sem precisar atualizar manualmente
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 3: Sync sem internet (queue)** (test-type: both)
  - blocked-by: spec 0201; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 3: Sync sem internet (queue)” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que estou sem internet QUANDO cadastro um novo pet ENTÃO o pet é salvo no Room (syncStatus = PENDING) E o pet aparece na UI normalmente E quando a internet voltar, o sync acontece automaticamente
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 4: Ativar sync pela primeira vez** (test-type: both)
  - blocked-by: spec 0201; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 4: Ativar sync pela primeira vez” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que tenho dados locais E nunca sincronizei antes QUANDO ativo "Sincronização na nuvem" nas configurações ENTÃO todos os dados locais são enviados para o Firestore E vejo progresso "Sincronizando X de Y itens..." E ao final, todos estão com syncStatus = SYNCED
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 5: Premium expira** (test-type: both)
  - blocked-by: spec 0201; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 5: Premium expira” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que meu premium expira QUANDO isso acontece ENTÃO o snapshot listener do Firestore é desconectado E novos dados são salvos apenas localmente (syncStatus = LOCAL_ONLY) E os dados já sincronizados permanecem no dispositivo E vejo aviso "Sincronização pausada - Renove seu premium" ---
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`
