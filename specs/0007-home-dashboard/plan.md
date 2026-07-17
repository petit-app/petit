# Plano: Dashboard inicial

Spec: [spec.md](./spec.md)

## Sequenciamento

1. Agregar pets, tarefas, timeline e último peso no `HomeViewModel`.
2. Calcular `PetWithSummary` e o estado vazio sem bloquear a UI.
3. Implementar cards, próximas tarefas, atividade recente e pull-to-refresh.
4. Integrar navegação para perfil, listas completas, configurações e Quick Add.
5. Avaliar separação visual de alertas e estado global saudável.

## Arquitetura

- `HomeViewModel` combina `PetRepository`, `TaskRepository`, `TimelineRepository`, `WeightEntryRepository` e resumo de saúde.
- `HomeUiState` representa carregamento, refresh, pets, tarefas e eventos.
- `QuickAddScreen` oferece cinco ações; `PetSelectionScreen` intermedeia ações que exigem um pet.
- `ActivityTimelineScreen` oferece filtros por período e pet.

## Dependências e riscos

- Depende de `0001`–`0005`; falhas parciais de uma fonte não devem ocultar todo o dashboard.
- Agregações por pet precisam evitar consultas excessivas e estados inconsistentes.
