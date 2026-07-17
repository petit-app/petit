# Tarefas: Configurações de Backup

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Status da spec: **On Hold**. Todas as tarefas permanecem pendentes até aprovação explícita.

## Tarefas

- [ ] **Cenário 1: Ativar/desativar backup automático** (test-type: both)
  - blocked-by: spec 0305
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 1: Ativar/desativar backup automático” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que estou logado com Google QUANDO acesso Configurações > Backup Automático E ativo o toggle "Backup automático" ENTÃO o backup diário às 2h é agendado E vejo "Próximo backup: hoje/amanhã às 2h" QUANDO desativo o toggle ENTÃO o agendamento é cancelado E vejo "Backup automático desativado"
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 2: Configurar Wi-Fi only** (test-type: both)
  - blocked-by: spec 0305; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 2: Configurar Wi-Fi only” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que backup automático está ativado E "Apenas em Wi-Fi" está desativado QUANDO ativo "Apenas em Wi-Fi" ENTÃO backups futuros só executam em Wi-Fi E o agendamento atual é ajustado DADO que estou em rede móvel às 2h E "Apenas em Wi-Fi" está ativado QUANDO o backup deveria executar ENTÃO é adiado até conectar em Wi-Fi
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 3: Ver histórico de backups** (test-type: both)
  - blocked-by: spec 0305; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 3: Ver histórico de backups” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que tenho backups automáticos realizados QUANDO acesso "Ver histórico" ENTÃO vejo lista dos últimos backups E cada item mostra: - Data/hora - Se foi automático ou manual - Status (sucesso/falha)
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 5: Notificação de backup** (test-type: both)
  - blocked-by: spec 0305; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 5: Notificação de backup” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que "Notificar após backup" está ativado QUANDO um backup automático é realizado com sucesso ENTÃO recebo uma notificação silenciosa "Backup realizado: 2 pets, 15 KB" DADO que "Notificar após backup" está desativado QUANDO um backup é realizado ENTÃO NÃO recebo notificação
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 6: Forçar backup agora** (test-type: both)
  - blocked-by: spec 0305; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 6: Forçar backup agora” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que estou na tela de configurações de backup QUANDO toco em "Fazer backup agora" ENTÃO um backup é executado imediatamente E o timer do próximo backup automático é resetado ---
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`
