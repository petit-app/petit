# Plano: Registro de desparasitação

Spec: [spec.md](./spec.md)

## Sequenciamento

1. Modelar `DewormingEntryEntity` referenciando o pet.
2. Implementar repositório, consultas ativas e cálculo por registro.
3. Integrar formulário, histórico, edição e soft delete.
4. Evoluir consultas para último registro por categoria e contabilização de `BOTH`.
5. Exibir seções interna e externa com status agregado.

## Arquitetura

- Room armazena `type`, medicamento, datas e metadados de sincronização.
- `DewormingViewModel` acessa o repositório e expõe estado às telas de formulário e registros.
- A visão por categoria requer selecionar os registros mais recentes aplicáveis a cada categoria.
- Salvar ou excluir pode acionar tarefas automáticas da spec `0005`.

## Dependências e riscos

- Depende de `0001`; integra opcionalmente com `0005`.
- `BOTH` pode competir com registros específicos; a regra deve escolher o evento aplicável mais recente.
