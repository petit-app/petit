# Plano: Resolução local de conflitos

Spec: [spec.md](./spec.md)

## Estado de partida

O merge por `updatedAt` e a gravação de `SyncLog` já existem. A execução deve
primeiro caracterizar esse comportamento, decidir o empate ainda aberto e só
então extrair uma regra única reutilizável.

## Sequência de implementação

1. Criar testes de caracterização para inserção, versão mais recente e log atuais.
2. Escolher e documentar um desempate estável para timestamps iguais.
3. Implementar um resolver puro que compare criação, edição e `deletedAt`.
4. Cobrir determinismo, idempotência, simetria e lotes duplicados com testes tabelados.
5. Integrar o resolver à transferência 0102 dentro de uma transação com `SyncLog`.
6. Implementar consulta e tela de histórico com contadores corretos.
7. Tornar o resolver a única regra consumida pela futura spec 0104.

## Regra base a preservar

1. UUID ausente localmente: inserir a versão remota.
2. Comparar eventos ativos e exclusões pelo instante efetivo mais recente.
3. Evento remoto mais recente: atualizar ou aplicar soft delete.
4. Evento local mais recente: manter local.
5. Instantes iguais: aplicar o desempate estável ainda a decidir, nunca “manter local” em ambos os lados.

## Dependências e integração

- Depende do bundle e do merge exercitados pela spec 0102.
- A spec 0104 dependerá deste resolver antes de aplicar changesets LAN.
- Room fornece a fronteira transacional e `SyncLog` registra o resultado.

## Riscos e mitigação

| Risco | Mitigação |
| --- | --- |
| Empate produz resultados diferentes | Usar chave de desempate derivada de dados estáveis e testar as duas ordens. |
| Delete posterior é perdido | Comparar `deletedAt` como evento, não apenas como flag. |
| Resolver difere entre transportes | Expor uma única API pura usada por todos os importadores. |
| Log diverge da transação | Persistir entidades e log na mesma fronteira transacional. |
| Relógios incorretos | Documentar a limitação e preparar versão/ID lógico como evolução futura. |

## Verificação final

1. Executar testes unitários tabelados nas duas ordens de entrada.
2. Executar testes de integração de Room, rollback e histórico.
3. Executar `./gradlew spotlessCheck` e `./gradlew test`.
4. Executar o mesmo bundle duas vezes e confirmar estado/contadores idempotentes.
5. Validar em dois dispositivos que edições e exclusões convergem.
