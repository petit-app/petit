# Tarefas: Acompanhamento de peso

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [x] **Registrar peso em gramas ou quilos** (test-type: both)
  - blocked-by: 0001
  - desired behavior: validar, converter para gramas e persistir uma pesagem.
  - acceptance criteria: apenas valores entre 0 e 50 kg e datas não futuras são aceitos.
  - verification: `./gradlew test`
- [x] **Manter uma pesagem por pet e dia** (test-type: integration)
  - blocked-by: registrar peso em gramas ou quilos
  - desired behavior: substituir a entrada ativa quando pet e data coincidirem.
  - acceptance criteria: a consulta retorna uma única entrada para o dia.
  - verification: `./gradlew test`
- [x] **Exibir histórico e gráfico de evolução** (test-type: integration)
  - blocked-by: registrar peso em gramas ou quilos
  - desired behavior: ordenar entradas e renderizar gráfico de barras em kg.
  - acceptance criteria: histórico decrescente e gráfico atualizam após mudanças.
  - verification: `./gradlew test`
- [x] **Editar e excluir uma pesagem** (test-type: both)
  - blocked-by: exibir histórico e gráfico de evolução
  - desired behavior: atualizar valor/`updatedAt` e realizar soft delete.
  - acceptance criteria: lista e gráfico refletem edição e exclusão.
  - verification: `./gradlew test`
- [ ] **Adicionar regressões automatizadas de peso** (test-type: both)
  - blocked-by: editar e excluir uma pesagem
  - desired behavior: cobrir conversão, limites, upsert e consultas.
  - acceptance criteria: suíte automatizada protege todos os critérios da spec.
  - verification: `./gradlew test`
