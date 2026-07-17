# Plano: Pareamento de dispositivos

Spec: [spec.md](./spec.md)

## Estado de partida

O projeto já contém a integração Nearby, permissões, apresentação,
repositórios e persistência do grupo. A execução deve preservar essas partes e
fechar o protocolo explícito de código e a validação entre dispositivos.

## Sequência de implementação

1. Mapear as transições atuais de `PairingState` e separar descoberta de autorização.
2. Implementar geração, expiração e validação do código de quatro dígitos.
3. Conectar a entrada do código no receptor ao pedido de conexão Nearby.
4. Garantir troca atômica e persistência da chave somente após autorização.
5. Encerrar advertising, discovery e conexões em cancelamento, erro e sucesso.
6. Validar permissões por API level e mensagens de fallback.
7. Cobrir lógica e integração; concluir com teste manual em dois dispositivos.

## Dependências e integração

- Nearby Connections e Google Play Services para o modo de pareamento.
- DataStore para chave do grupo e identidade do dispositivo.
- A spec 0102 consome a conexão autorizada resultante.
- Referência técnica: [protocolos de compartilhamento local](../../docs/local-sharing-protocols.md).

## Riscos e mitigação

| Risco | Mitigação |
| --- | --- |
| Endpoint errado aceito por descoberta automática | Tornar a validação do código obrigatória antes da troca da chave. |
| Estado residual após interrupção | Centralizar cleanup idempotente para todos os estados terminais. |
| Diferenças de permissões entre versões do Android | Testar matrizes abaixo e acima das APIs 31 e 33. |
| Dispositivo sem Google Play Services | Detectar a indisponibilidade e explicar a limitação sem perder dados. |

## Verificação final

1. Executar `./gradlew spotlessCheck` e `./gradlew test`.
2. Executar `./gradlew assembleDebug && ./gradlew installDebug` no primeiro dispositivo.
3. Instalar o mesmo APK no segundo dispositivo.
4. Validar código correto, incorreto, cancelamento e ausência de internet.
5. Confirmar que ambos persistem a mesma chave e que nenhum dado é apagado ao sair.
