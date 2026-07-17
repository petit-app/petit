# Tarefas: Login com Google

> Status da spec: **On Hold**. Todas as tarefas permanecem pendentes até aprovação explícita.

## Tarefas

- [ ] **Cenário 1: Login bem-sucedido** (test-type: both)
  - blocked-by: nenhum
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 1: Login bem-sucedido” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que estou usando o app sem login QUANDO toco em "Entrar com Google" ENTÃO vejo o seletor de contas Google do sistema QUANDO seleciono minha conta E autorizo o app ENTÃO sou autenticado com sucesso E vejo meu nome/foto na tela de configurações E o estado muda para "Authenticated"
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 2: Primeiro login associa dados existentes** (test-type: both)
  - blocked-by: nenhum; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 2: Primeiro login associa dados existentes” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que tenho dados locais (pets, pesos, etc.) E nunca fiz login antes QUANDO faço login com Google pela primeira vez ENTÃO meus dados locais são associados ao meu userId E posso continuar usando normalmente
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 3: Login cancelado** (test-type: both)
  - blocked-by: nenhum; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 3: Login cancelado” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que inicio o processo de login QUANDO cancelo o seletor de contas OU fecho o dialog ENTÃO volto ao estado anterior (anônimo) E não vejo mensagem de erro E posso tentar novamente
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 4: Erro de rede no login** (test-type: both)
  - blocked-by: nenhum; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 4: Erro de rede no login” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que estou sem conexão de internet QUANDO tento fazer login ENTÃO vejo mensagem "Sem conexão. Tente novamente." E continuo no modo anônimo E o app continua funcionando normalmente offline
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 5: Login ativado ao tentar backup** (test-type: both)
  - blocked-by: nenhum; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 5: Login ativado ao tentar backup” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que estou usando o app sem login E tenho dados locais (pets, pesos, etc.) QUANDO tento fazer "Backup para Google Drive" ENTÃO vejo dialog explicando que é necessário login E tenho opção "Entrar com Google" QUANDO faço login com sucesso ENTÃO o backup é iniciado automaticamente ---
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`
