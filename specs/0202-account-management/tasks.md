# Tarefas: Gerenciamento de Conta

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Status da spec: **On Hold**. Todas as tarefas permanecem pendentes até aprovação explícita.

## Tarefas

- [ ] **Cenário 1: Ver informações da conta** (test-type: both)
  - blocked-by: spec 0201
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 1: Ver informações da conta” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que estou logado como "pessoa-a@example.com" QUANDO acesso Configurações > Minha Conta ENTÃO vejo: - Minha foto de perfil do Google - Meu nome "Pessoa A" - Meu email "pessoa-a@example.com" - Meu status de plano (Gratuito/Premium) - Data do último login
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 2: Fazer logout** (test-type: both)
  - blocked-by: spec 0201; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 2: Fazer logout” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que estou logado QUANDO toco em "Sair" E confirmo a ação ENTÃO sou deslogado do Firebase Auth E volto ao estado "Anônimo" E meus dados locais permanecem no dispositivo E posso continuar usando o app sem login
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 3: Logout mantém dados locais** (test-type: both)
  - blocked-by: spec 0201; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 3: Logout mantém dados locais” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que tenho 2 pets cadastrados E estou logado QUANDO faço logout ENTÃO continuo vendo meus 2 pets E posso adicionar novos dados E os dados não são deletados
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 4: Trocar de conta** (test-type: both)
  - blocked-by: spec 0201; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 4: Trocar de conta” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que estou logado como "pessoa-a@example.com" QUANDO faço logout E faço login com "pessoa-b@example.com" ENTÃO estou autenticado como Pessoa B E os dados locais permanecem (da Pessoa A) (associação de dados por conta é feita no sync - fases futuras)
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 5: Deletar conta** (test-type: both)
  - blocked-by: spec 0201; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 5: Deletar conta” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que estou logado QUANDO toco em "Deletar minha conta" ENTÃO vejo aviso explicando consequências: - "Sua conta será removida do Firebase" - "Dados locais permanecerão no dispositivo" - "Dados na nuvem serão removidos em até 30 dias" QUANDO confirmo digitando "DELETAR" ENTÃO minha conta é removida do Firebase E os dados na nuvem são agendados para purge em 30 dias E sou deslogado E volto ao modo anônimo ---
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`
