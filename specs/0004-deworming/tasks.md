# Tarefas: Registro de desparasitação

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [x] **Registrar, editar e excluir desparasitação** (test-type: both)
  - blocked-by: 0001
  - desired behavior: validar e persistir `INTERNAL`, `EXTERNAL` ou `BOTH`, com soft delete.
  - acceptance criteria: medicamento obrigatório, datas válidas e histórico decrescente.
  - verification: `./gradlew test`
- [x] **Calcular e mostrar status por registro** (test-type: unit)
  - blocked-by: registrar, editar e excluir desparasitação
  - desired behavior: classificar cada próxima dose e exibir seu indicador.
  - acceptance criteria: estados `OK`, `SCHEDULED` e `OVERDUE` seguem as datas.
  - verification: `./gradlew test`
- [ ] **Separar a saúde por categoria** (test-type: both)
  - blocked-by: calcular e mostrar status por registro
  - desired behavior: mostrar seções interna e externa usando o registro aplicável mais recente.
  - acceptance criteria: `BOTH` conta nas duas categorias e cada seção tem status próprio.
  - verification: `./gradlew test`
- [ ] **Adicionar regressões automatizadas de desparasitação** (test-type: both)
  - blocked-by: separar a saúde por categoria
  - desired behavior: cobrir validações, status, categorias, Room e UI.
  - acceptance criteria: todos os critérios de aceite têm cobertura automatizada.
  - verification: `./gradlew test`
