# Plano: Registro de vacinação

Spec: [spec.md](./spec.md)

## Sequenciamento

1. Modelar `VaccinationEntryEntity` com chave do pet e metadados de auditoria.
2. Implementar repositório, consultas ativas e cálculo de `HealthStatus`.
3. Implementar formulário com catálogo filtrado por `PetType` e tipo customizado.
4. Integrar agrupamento, indicadores visuais, edição e exclusão.

## Arquitetura

- Room mantém as doses; o domínio calcula status a partir de `nextDueDate` e da data atual.
- `VaccinationViewModel` acessa o repositório e expõe estado para `VaccinationFormScreen` e `VaccinationRecordsScreen`.
- Salvar ou excluir uma dose pode acionar tarefas automáticas da spec `0005`.

## Dependências e riscos

- Depende de `0001`; integra opcionalmente com `0005`.
- O cálculo de dias deve ser determinístico e testável com relógio controlado.
