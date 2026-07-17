# Tarefas: Dashboard inicial

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [x] **Exibir estado inicial sem pets** (test-type: both)
  - blocked-by: 0001
  - desired behavior: mostrar boas-vindas e ação de cadastro.
  - acceptance criteria: ação abre o formulário do primeiro pet.
  - verification: `./gradlew test`
- [x] **Resumir saúde e próximos cuidados por pet** (test-type: both)
  - blocked-by: 0001, 0002, 0003, 0004
  - desired behavior: combinar foto, nome, último peso, status e próximo evento.
  - acceptance criteria: cards atualizam com os dados ativos e abrem o perfil.
  - verification: `./gradlew test`
- [x] **Exibir tarefas e timeline** (test-type: integration)
  - blocked-by: 0005
  - desired behavior: mostrar até cinco próximas tarefas e atividade recente com “ver tudo”.
  - acceptance criteria: ações abrem as listas completas e os eventos corretos.
  - verification: `./gradlew test`
- [x] **Oferecer ações rápidas e atualização** (test-type: integration)
  - blocked-by: resumir saúde e próximos cuidados por pet
  - desired behavior: navegar por Quick Add, configurações e pull-to-refresh.
  - acceptance criteria: cinco ações funcionam e seleção de pet aparece quando necessária.
  - verification: `./gradlew test`
- [ ] **Exibir estado global saudável e alertas separados** (test-type: both)
  - blocked-by: decisão futura de produto
  - desired behavior: adicionar banner “Tudo em ordem” e seção de atenção necessária.
  - acceptance criteria: estados saudáveis e críticos são claros e acessíveis.
  - verification: `./gradlew test`
