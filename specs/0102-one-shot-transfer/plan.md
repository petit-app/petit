# Plano: Transferência pontual de dados

Spec: [spec.md](./spec.md)

## Estado de partida

O envio, a recepção, as opções de merge/replace e a apresentação já existem.
O plano concentra a execução nas diferenças semânticas do replace, na
atomicidade, na telemetria local de progresso e na validação em hardware.

## Sequência de implementação

1. Fixar a lista de entidades incluídas no `ExportBundle` e validar o payload antes da escrita.
2. Cobrir o merge atual por UUID, `updatedAt` e soft delete com testes.
3. Implementar replace transacional: limpar o conjunto compartilhável e importar o bundle.
4. Descartar transferências incompletas e mapear erros recuperáveis na UI.
5. Derivar progresso dos bytes efetivamente enviados/recebidos e gerar resumo do resultado.
6. Validar merge, replace, repetição e interrupção em dois dispositivos.

## Dependências e integração

- Depende do canal autorizado pela spec 0101.
- Usa Room como fonte local e o fluxo de exportação/importação existente.
- Reutiliza as regras formalizadas na spec 0105.
- Referência técnica: [protocolos de compartilhamento local](../../docs/local-sharing-protocols.md).

## Riscos e mitigação

| Risco | Mitigação |
| --- | --- |
| Replace apaga dados antes de validar o bundle | Validar primeiro e executar limpeza/importação na mesma transação. |
| Payload excede BYTES | Selecionar FILE para bundles grandes e verificar integridade ao final. |
| Repetição duplica registros | Tornar merge idempotente por UUID. |
| Conexão cai durante escrita | Só persistir depois de receber e validar o payload completo. |

## Verificação final

1. Executar `./gradlew spotlessCheck` e `./gradlew test`.
2. Executar `./gradlew assembleDebug && ./gradlew installDebug`.
3. Em dois dispositivos, validar merge, replace, ausência de internet e interrupção.
4. Comparar a base final e os contadores exibidos com o bundle enviado.
