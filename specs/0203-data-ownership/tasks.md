# Tarefas: Vinculação de Dados

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Status da spec: **On Hold**. Todas as tarefas permanecem pendentes até aprovação explícita.

## Tarefas

- [ ] **Cenário 1: Primeiro login vincula dados existentes** (test-type: both)
  - blocked-by: spec 0201
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 1: Primeiro login vincula dados existentes” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que tenho 2 pets cadastrados localmente E nunca fiz login antes QUANDO faço login com "pessoa-a@example.com" ENTÃO meus 2 pets são marcados com ownerId = meu userId E posso continuar usando normalmente
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 2: Dados criados após login já vêm vinculados** (test-type: both)
  - blocked-by: spec 0201; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 2: Dados criados após login já vêm vinculados” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que estou logado como "pessoa-a@example.com" QUANDO cadastro um novo pet "Luna" ENTÃO Luna é criada com ownerId = meu userId
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 3: Dados em modo anônimo não têm owner** (test-type: both)
  - blocked-by: spec 0201; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 3: Dados em modo anônimo não têm owner” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que estou usando sem login QUANDO cadastro um pet "Simba" ENTÃO Simba é criado com ownerId = null
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 4: Logout não remove vinculação** (test-type: both)
  - blocked-by: spec 0201; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 4: Logout não remove vinculação” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que tenho pets vinculados ao meu userId QUANDO faço logout ENTÃO os pets continuam com o ownerId preenchido E continuam visíveis no app
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 5: Login com conta diferente** (test-type: both)
  - blocked-by: spec 0201; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 5: Login com conta diferente” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que tenho pets do "user-a" no dispositivo E faço login como "pessoa-b@example.com" (user-b) ENTÃO vejo os pets da Pessoa A (dados locais) MAS eles continuam com ownerId = user-a E novos dados criados terão ownerId = user-b (gestão de múltiplos owners é tratada em sync futuro) ---
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`
