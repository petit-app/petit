# Tarefas: Resolução de Conflitos na Nuvem

> Status da spec: **On Hold**. Todas as tarefas permanecem pendentes até aprovação explícita.

## Tarefas

- [ ] **Cenário 1: Last-write-wins básico** (test-type: both)
  - blocked-by: spec 0401
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 1: Last-write-wins básico” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que o pet "Luna" tem updatedAt = 1000 no dispositivo A E o dispositivo B edita Luna (updatedAt = 1500) QUANDO o dispositivo A recebe a mudança do snapshot listener do Firestore ENTÃO a versão do dispositivo B (mais recente) é mantida E o dispositivo A mostra as alterações do B
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 2: Edição offline mais antiga** (test-type: both)
  - blocked-by: spec 0401; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 2: Edição offline mais antiga” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que dispositivo A está offline e edita Luna (updatedAt = 1000) E dispositivo B edita Luna online (updatedAt = 1500) QUANDO dispositivo A volta online e tenta sync ENTÃO a versão do dispositivo B vence (updatedAt maior) E as alterações do dispositivo A são descartadas E o dispositivo A atualiza para a versão do B
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 3: Edição offline mais recente** (test-type: both)
  - blocked-by: spec 0401; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 3: Edição offline mais recente” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que dispositivo A está offline e edita Luna (updatedAt = 2000) E a versão no Firestore tem updatedAt = 1500 QUANDO dispositivo A volta online e faz sync ENTÃO a versão do dispositivo A vence (updatedAt maior) E o Firestore é atualizado com a versão do A E outros dispositivos recebem a versão do A
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 4: Delete vs Edit** (test-type: both)
  - blocked-by: spec 0401; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 4: Delete vs Edit” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que dispositivo A deleta Luna (deletedAt = 1500) E dispositivo B editou Luna (updatedAt = 1600) antes de receber o delete QUANDO o sync acontece ENTÃO se a edição é mais recente que o delete, Luna é restaurada OU se o delete é mais recente, Luna permanece deletada
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 5: Campos diferentes editados** (test-type: both)
  - blocked-by: spec 0401; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 5: Campos diferentes editados” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que dispositivo A edita o nome de Luna para "Luninha" E dispositivo B edita o peso de Luna ao mesmo tempo QUANDO o sync acontece ENTÃO ambas as mudanças são mantidas (se a estratégia for field-level) OU a versão mais recente vence completamente (se document-level) ---
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`
