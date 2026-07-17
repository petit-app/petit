# Tarefas: Gerenciamento de pets

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [x] **Cadastrar e editar um pet válido** (test-type: both)
  - blocked-by: none
  - desired behavior: validar o formulário, persistir o pet e atualizar `updatedAt` na edição.
  - acceptance criteria: nome e tipo obrigatórios; limites e nascimento não futuro; dados persistem após reinício.
  - verification: `./gradlew test`
- [x] **Listar e abrir pets ativos** (test-type: integration)
  - blocked-by: cadastrar e editar um pet válido
  - desired behavior: mostrar apenas pets sem `deletedAt` e navegar ao perfil.
  - acceptance criteria: lista ordenada e perfil com dados e ações de gestão.
  - verification: `./gradlew test`
- [x] **Excluir um pet logicamente** (test-type: both)
  - blocked-by: listar e abrir pets ativos
  - desired behavior: confirmar exclusão, preencher `deletedAt` e ocultar o pet.
  - acceptance criteria: registro permanece no banco e deixa consultas ativas.
  - verification: `./gradlew test`
- [x] **Selecionar e exibir foto local do pet** (test-type: integration)
  - blocked-by: cadastrar e editar um pet válido
  - desired behavior: selecionar uma imagem da galeria e exibi-la no formulário, lista e perfil.
  - acceptance criteria: a URI selecionada permanece associada após salvar.
  - verification: `./gradlew test`
- [ ] **Completar captura e validação de foto** (test-type: integration)
  - blocked-by: selecionar e exibir foto local do pet
  - desired behavior: oferecer câmera e rejeitar arquivo maior que 5 MB ou formato não suportado.
  - acceptance criteria: câmera e seletor aceitam apenas JPG/PNG dentro do limite.
  - verification: `./gradlew test`
- [ ] **Cobrir o fluxo com testes automatizados** (test-type: both)
  - blocked-by: excluir um pet logicamente, completar captura e validação de foto
  - desired behavior: proteger validação, DAO, soft delete e navegação contra regressões.
  - acceptance criteria: testes unitários do DAO e testes básicos de UI executam com sucesso.
  - verification: `./gradlew test`
