# Tarefas: Compartilhamento Familiar na Nuvem

> Status da spec: **On Hold**. Todas as tarefas permanecem pendentes até aprovação explícita.

## Tarefas

- [ ] **Cenário 1: Criar grupo familiar** (test-type: both)
  - blocked-by: spec 0201, spec 0401
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 1: Criar grupo familiar” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que sou usuário premium QUANDO acesso Configurações > "Família" E toco em "Criar grupo familiar" ENTÃO um grupo é criado E eu me torno o administrador E recebo um código de convite
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 2: Convidar membro** (test-type: both)
  - blocked-by: spec 0201, spec 0401; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 2: Convidar membro” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que sou admin de um grupo familiar QUANDO compartilho o código de convite "PETIT-ABC123" E outra pessoa insere o código no app dela ENTÃO ela entra no grupo familiar E vê todos os pets do grupo
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 3: Todos veem e editam** (test-type: both)
  - blocked-by: spec 0201, spec 0401; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 3: Todos veem e editam” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que Pessoa A e Pessoa B estão no mesmo grupo familiar QUANDO Pessoa B adiciona uma pesagem para um pet ENTÃO Pessoa A vê a pesagem automaticamente E Pessoa A também pode adicionar/editar dados
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 4: Permissões de admin** (test-type: both)
  - blocked-by: spec 0201, spec 0401; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 4: Permissões de admin” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que sou admin do grupo QUANDO acesso a lista de membros ENTÃO posso: - Remover membros - Gerar novo código de convite - Deletar o grupo
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 5: Membro sai do grupo** (test-type: both)
  - blocked-by: spec 0201, spec 0401; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 5: Membro sai do grupo” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que sou membro de um grupo familiar QUANDO escolho "Sair do grupo" ENTÃO perco acesso aos dados compartilhados E os dados permanecem para os outros membros E meus dados pessoais (não compartilhados) permanecem comigo
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 6: Pets privados vs compartilhados** (test-type: both)
  - blocked-by: spec 0201, spec 0401; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 6: Pets privados vs compartilhados” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que sou membro de um grupo familiar QUANDO cadastro um novo pet ENTÃO posso escolher: - "Compartilhar com família" (todos veem) - "Manter privado" (só eu vejo) ---
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`
