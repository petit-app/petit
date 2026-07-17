# Plano: Tarefas e lembretes locais

Spec: [spec.md](./spec.md)

## Sequenciamento

1. Modelar `TaskEntity` com tipos `VACCINATION`, `DEWORMING`, `WEIGHT` e `CUSTOM` e estados `PENDING`/`COMPLETED`.
2. Implementar repositório e `AutoTaskService` para criar/cancelar tarefas vinculadas.
3. Integrar `TaskScheduler` e `TaskNotificationWorker` ao WorkManager.
4. Persistir `ReminderPreferences` no DataStore.
5. Integrar lista, formulário, configurações e concluídas.

## Arquitetura

- Tarefas e notificações são locais e one-shot.
- `referenceEntityId` liga uma tarefa ao registro de saúde que a originou.
- `PendingIntent` deve ser imutável; exclusão e conclusão cancelam o trabalho agendado.
- Rotas: `tasks`, `tasks/form?taskId={taskId}`, `tasks/settings` e `tasks/completed`.

## Dependências e riscos

- Depende de `0001` e recebe eventos de `0002`, `0003` e `0004`.
- Restrições do Android sobre notificações e execução em segundo plano exigem testes de integração.
