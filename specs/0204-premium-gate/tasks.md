# Tarefas: Gate Premium

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Status da spec: **On Hold**. Todas as tarefas permanecem pendentes até aprovação explícita.

## Tarefas

- [ ] **Cenário 1: Ver indicador premium em feature bloqueada** (test-type: both)
  - blocked-by: spec 0201
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 1: Ver indicador premium em feature bloqueada” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que sou usuário gratuito QUANDO vejo a opção "Sincronização em tempo real" nas configurações ENTÃO vejo um ícone de ⭐ ou 🔒 indicando que é premium E ao tocar, vejo informação sobre o plano premium
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 2: Tentar usar feature premium** (test-type: both)
  - blocked-by: spec 0201; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 2: Tentar usar feature premium” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que sou usuário gratuito QUANDO tento ativar "Sincronização em tempo real" ENTÃO vejo um bottom sheet ou dialog explicando: - O que a feature faz - Que é exclusiva para premium - Botão para ver planos
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 3: Listar benefícios premium** (test-type: both)
  - blocked-by: spec 0201; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 3: Listar benefícios premium” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que estou no app QUANDO acesso "Ver planos premium" ENTÃO vejo lista de benefícios: - ☁️ Sincronização em tempo real na nuvem - 📱 Múltiplos dispositivos sincronizados automaticamente - 👨‍👩‍👧 Compartilhar com família - 📄 Exportar PDF (futuro)
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 4: Verificar status premium** (test-type: both)
  - blocked-by: spec 0201; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 4: Verificar status premium” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que sou usuário premium QUANDO acesso configurações ENTÃO vejo "Plano: Premium" E não vejo indicadores de bloqueio em features premium E as features premium estão liberadas
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 5: Funcionalidades gratuitas disponíveis sem login** (test-type: both)
  - blocked-by: spec 0201; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 5: Funcionalidades gratuitas disponíveis sem login” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que não estou logado QUANDO uso o app ENTÃO posso cadastrar pets, pesar, vacinar, criar lembretes E posso exportar/importar JSON MAS não posso fazer backup no Google Drive (requer login) E não posso usar sync em tempo real (premium) ---
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`
