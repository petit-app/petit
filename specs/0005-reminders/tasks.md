# Tarefas: Tarefas e lembretes locais

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [x] **Criar tarefas automáticas de cuidado** (test-type: both)
  - blocked-by: 0001
  - desired behavior: criar/cancelar tarefas vinculadas a vacina, desparasitação e peso conforme preferências.
  - acceptance criteria: tipo e referência corretos; exclusão do registro cancela e remove a tarefa ativa.
  - verification: `./gradlew test`
- [x] **Gerenciar tarefa personalizada** (test-type: both)
  - blocked-by: 0001
  - desired behavior: criar, editar, concluir e listar tarefas customizadas.
  - acceptance criteria: tarefa futura inicia `PENDING`; conclusão move para o histórico.
  - verification: `./gradlew test`
- [x] **Agendar notificação local one-shot** (test-type: integration)
  - blocked-by: criar tarefas automáticas de cuidado, gerenciar tarefa personalizada
  - desired behavior: agendar via WorkManager e notificar mesmo offline.
  - acceptance criteria: execução usa a tarefa atual e `PendingIntent` imutável.
  - verification: `./gradlew test`
- [x] **Configurar e filtrar tarefas** (test-type: integration)
  - blocked-by: gerenciar tarefa personalizada
  - desired behavior: persistir preferências e filtrar por hoje, semana e mês.
  - acceptance criteria: configurações sobrevivem ao reinício e filtros exibem tarefas esperadas.
  - verification: `./gradlew test`
- [ ] **Adicionar repetição automática** (test-type: both)
  - blocked-by: decisão futura de produto
  - desired behavior: ainda fora do escopo desta fase.
  - acceptance criteria: requer nova aprovação de spec antes de implementação.
  - verification: `./gradlew test`
- [ ] **Adicionar snooze** (test-type: both)
  - blocked-by: decisão futura de produto
  - desired behavior: ainda fora do escopo desta fase.
  - acceptance criteria: requer nova aprovação de spec antes de implementação.
  - verification: `./gradlew test`
