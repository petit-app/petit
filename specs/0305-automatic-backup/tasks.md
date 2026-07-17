# Tarefas: Backup Automático

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Status da spec: **On Hold**. Todas as tarefas permanecem pendentes até aprovação explícita.

## Tarefas

- [ ] **Cenário 1: Backup automático ativado por padrão (usuário logado)** (test-type: both)
  - blocked-by: spec 0301
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 1: Backup automático ativado por padrão (usuário logado)” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que estou logado com Google QUANDO habilito backup automático nas configurações ENTÃO o WorkManager agenda backup diário às 2h da madrugada E vejo "Backup automático ativado — próximo às 2h"
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 2: Backup diário executa em background** (test-type: both)
  - blocked-by: spec 0301; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 2: Backup diário executa em background” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que backup automático está ativado QUANDO chega 2h da madrugada ENTÃO o backup é executado automaticamente MESMO que o app esteja fechado E não preciso abrir o app E o backup é salvo no Google Drive
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 3: Backup apenas em Wi-Fi** (test-type: both)
  - blocked-by: spec 0301; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 3: Backup apenas em Wi-Fi” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que "Backup apenas em Wi-Fi" está ativado E estou conectado em rede móvel (4G/5G) QUANDO o backup automático deveria executar ENTÃO o backup é adiado E executa quando conectar em Wi-Fi
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 4: Backup apenas se logado** (test-type: both)
  - blocked-by: spec 0301; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 4: Backup apenas se logado” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que backup automático está agendado E não estou mais logado (logout) QUANDO chega 2h da madrugada ENTÃO o backup NÃO é executado E vejo notificação "Faça login para continuar backups automáticos"
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 5: Configuração Wi-Fi only respeitada** (test-type: both)
  - blocked-by: spec 0301; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 5: Configuração Wi-Fi only respeitada” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que "Backup apenas em Wi-Fi" está ativado E estou conectado em rede móvel (4G/5G) às 2h QUANDO o backup automático deveria executar ENTÃO o backup é adiado E executa quando conectar em Wi-Fi E vejo notificação "Aguardando Wi-Fi para backup"
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 6: Sem internet** (test-type: both)
  - blocked-by: spec 0301; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 6: Sem internet” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que não tenho conexão de internet QUANDO o backup automático deveria executar ENTÃO o backup falha silenciosamente E será tentado novamente na próxima vez E posso ver "Último backup: há 2 dias (falhou)" nas configurações ---
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`
