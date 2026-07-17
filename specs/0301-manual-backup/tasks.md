# Tarefas: Backup Manual

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Status da spec: **On Hold**. Todas as tarefas permanecem pendentes até aprovação explícita.

## Tarefas

- [ ] **Cenário 1: Fazer backup com sucesso (usuário já logado)** (test-type: both)
  - blocked-by: spec 0201
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 1: Fazer backup com sucesso (usuário já logado)” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que estou logado com Google E tenho conexão com internet QUANDO acesso Configurações > "Backup Google Drive" E toco em "Fazer backup agora" ENTÃO vejo indicador de progresso E o backup é enviado para o Google Drive (appDataFolder) E vejo mensagem "Backup realizado com sucesso" E vejo data/hora do último backup
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 2: Backup sem internet** (test-type: both)
  - blocked-by: spec 0201; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 2: Backup sem internet” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que estou sem conexão de internet QUANDO tento fazer backup ENTÃO vejo mensagem "Sem conexão. Conecte-se à internet para fazer backup." E o backup não é iniciado
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 3: Backup sem estar logado (ativa login)** (test-type: both)
  - blocked-by: spec 0201; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 3: Backup sem estar logado (ativa login)” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que não estou logado QUANDO tento fazer backup ENTÃO vejo dialog explicando que é necessário login Google E tenho opção "Entrar com Google" QUANDO faço login com sucesso ENTÃO o backup é iniciado automaticamente
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 4: Primeiro backup** (test-type: both)
  - blocked-by: spec 0201; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 4: Primeiro backup” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que nunca fiz backup antes QUANDO faço meu primeiro backup ENTÃO o arquivo é criado no appDataFolder do Google Drive E o metadata é inicializado E vejo "Backup realizado com sucesso"
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 5: Backup subsequente** (test-type: both)
  - blocked-by: spec 0201; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 5: Backup subsequente” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que já tenho backups anteriores QUANDO faço novo backup ENTÃO um novo arquivo é criado (não substitui o anterior) E o metadata é atualizado E backups antigos são mantidos (até o limite)
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 6: Erro durante backup** (test-type: both)
  - blocked-by: spec 0201; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 6: Erro durante backup” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que inicio um backup QUANDO ocorre erro (rede cai, quota excedida, etc.) ENTÃO vejo mensagem de erro específica E o backup parcial é descartado E posso tentar novamente ---
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`
