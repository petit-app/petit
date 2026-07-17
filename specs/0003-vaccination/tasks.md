# Tarefas: Registro de vacinação

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [x] **Registrar e editar uma vacinação** (test-type: both)
  - blocked-by: 0001
  - desired behavior: validar datas, tipo por espécie, `OTHER` e rastreabilidade.
  - acceptance criteria: registro válido é persistido e edição atualiza `updatedAt`.
  - verification: `./gradlew test`
- [x] **Calcular e mostrar o status da dose** (test-type: unit)
  - blocked-by: registrar e editar uma vacinação
  - desired behavior: classificar próxima dose como `OK`, `SCHEDULED` ou `OVERDUE`.
  - acceptance criteria: limites de 30 dias e ausência de próxima dose seguem a spec.
  - verification: `./gradlew test`
- [x] **Exibir e excluir o histórico de vacinas** (test-type: both)
  - blocked-by: calcular e mostrar o status da dose
  - desired behavior: exibir doses por mês, destacar tipos/status e permitir soft delete.
  - acceptance criteria: histórico e estado visual atualizam após exclusão.
  - verification: `./gradlew test`
- [ ] **Oferecer histórico agrupado por tipo** (test-type: integration)
  - blocked-by: exibir e excluir o histórico de vacinas
  - desired behavior: reunir as doses de cada tipo sem perder a cronologia completa.
  - acceptance criteria: cada tipo mostra status atual e acesso a todas as suas doses.
  - verification: `./gradlew test`
- [ ] **Adicionar regressões automatizadas de vacinação** (test-type: both)
  - blocked-by: oferecer histórico agrupado por tipo
  - desired behavior: cobrir cálculo, validações, catálogo, persistência e UI.
  - acceptance criteria: todos os critérios de aceite têm cobertura automatizada.
  - verification: `./gradlew test`
