# Plano: Gerenciamento de pets

Spec: [spec.md](./spec.md)

## Sequenciamento

1. Modelar `PetEntity`, domínio e mapeadores com metadados de criação, atualização, exclusão e sincronização.
2. Expor CRUD e consultas ativas por `PetDao` e `PetRepository`.
3. Implementar validação e estado nos ViewModels.
4. Integrar lista, detalhe, formulário, confirmação de exclusão e seleção de pet à navegação.
5. Integrar seleção/captura e armazenamento local da foto.

## Arquitetura

- Room é a fonte local de verdade; consultas ativas filtram `deletedAt IS NULL`.
- ViewModels dependem de `PetRepository`, nunca do DAO.
- Rotas: `pets`, `pets/{petId}`, `pets/form?petId={petId}`, `pets/{petId}/delete` e `select-pet/{action}`.
- A exclusão é lógica e atualiza `deletedAt` e `updatedAt`.

## Dependências e riscos

- Base para todas as demais specs da família `pet-care`.
- URIs de foto exigem permissões persistentes e tratamento de conteúdo indisponível.
