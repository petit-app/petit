# Plano: Grupo familiar local

Spec: [spec.md](./spec.md)

## Estado de partida

O app já lista e remove membros localmente, permite sair do grupo e persiste as
preferências necessárias. A próxima execução deve acrescentar identidade
editável e tornar mudanças de membresia parte do protocolo de sincronização.

## Sequência de implementação

1. Cobrir o estado existente de lista, remoção e saída com testes de regressão.
2. Implementar renomeação por ID local estável e persistir o nome no DataStore/Room.
3. Modelar renomeação, remoção e saída como mudanças sincronizáveis e idempotentes.
4. Rejeitar conexões de identidades removidas ou com chave revogada.
5. Exibir a última sincronização conhecida e estados vazios na tela do grupo.
6. Validar propagação e preservação dos dados em dois dispositivos.

## Dependências e integração

- Depende da identidade e da chave estabelecidas pela spec 0101.
- A propagação manual pode usar a spec 0102.
- A propagação automática futura usa a spec 0104.
- Resolução idempotente segue a spec 0105.

## Riscos e mitigação

| Risco | Mitigação |
| --- | --- |
| Dispositivo removido volta a aparecer | Propagar marcador de remoção e validar autorização antes de sincronizar. |
| Nome usado como identidade | Manter UUID imutável separado do nome editável. |
| Saída apaga dados de saúde | Isolar limpeza de membresia das tabelas de pets e testar essa fronteira. |
| Mudanças offline divergem | Aplicar eventos de membresia de forma determinística e idempotente. |

## Verificação final

1. Executar `./gradlew spotlessCheck` e `./gradlew test`.
2. Executar `./gradlew assembleDebug && ./gradlew installDebug`.
3. Em dois dispositivos, validar renomear, remover, sair e reencontrar o par.
4. Confirmar que a chave antiga é recusada e que os dados dos pets permanecem.
