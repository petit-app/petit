# Tarefas: Sincronização entre Múltiplos Dispositivos

> Status da spec: **On Hold**. Todas as tarefas permanecem pendentes até aprovação explícita.

## Tarefas

- [ ] **Cenário 1: Segundo dispositivo recebe dados** (test-type: both)
  - blocked-by: spec 0401
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 1: Segundo dispositivo recebe dados” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que tenho dados no dispositivo A E instalo o app no dispositivo B QUANDO faço login no dispositivo B E ativo a sincronização ENTÃO todos os meus dados são baixados do Firestore E vejo os mesmos pets que no dispositivo A
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 2: Edição aparece em tempo real** (test-type: both)
  - blocked-by: spec 0401; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 2: Edição aparece em tempo real” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que tenho o app aberto no dispositivo A e B QUANDO edito o nome do pet para "Luninha" no dispositivo A ENTÃO em poucos segundos, o dispositivo B mostra "Luninha" Sem precisar atualizar manualmente
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 3: Criar em um, ver em outro** (test-type: both)
  - blocked-by: spec 0401; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 3: Criar em um, ver em outro” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que adiciono um novo pet "Simba" no dispositivo A QUANDO o sync completa ENTÃO o dispositivo B recebe "Simba" automaticamente E Simba aparece na lista de pets
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 4: Deletar em um, reflete em outro** (test-type: both)
  - blocked-by: spec 0401; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 4: Deletar em um, reflete em outro” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que deleto o pet "Simba" no dispositivo A QUANDO o sync completa ENTÃO o dispositivo B também não mostra mais "Simba"
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 5: Dispositivo offline vs online** (test-type: both)
  - blocked-by: spec 0401; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 5: Dispositivo offline vs online” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que dispositivo A está offline E dispositivo B adiciona pet "Mia" QUANDO dispositivo A volta online ENTÃO dispositivo A recebe "Mia" automaticamente ---
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`
