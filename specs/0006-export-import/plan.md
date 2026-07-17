# Plano: Exportação e importação JSON

Spec: [spec.md](./spec.md)

## Sequenciamento

1. Agregar os cinco domínios em `ExportBundle` com `ExportMetadata`.
2. Serializar/deserializar com `org.json` e versionar o schema.
3. Ler e escrever por `ContentResolver`, validando o documento antes de importar.
4. Analisar contagens e conflitos e aplicar `REPLACE`, `KEEP` ou `MERGE` em transação.
5. Integrar exportação/importação às configurações e exportação por pet ao perfil.

## Arquitetura

- `ExportImportUseCase` coordena repositórios, parsing, análise e merge.
- `ExportBundle` usa as chaves `pets`, `weightEntries`, `vaccinationEntries`, `dewormingEntries` e `tasks`.
- A importação só persiste depois da validação e confirmação do usuário.

## Dependências e riscos

- Depende de `0001`–`0005` e precisa preservar IDs e referências.
- Mudanças de schema exigem migração explícita e testes com backups antigos.
- Falhas de I/O ou parse não podem produzir importação parcial.
