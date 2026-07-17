# Tarefas: Transferência entre Dispositivos

> Status da spec: **On Hold**. Todas as tarefas permanecem pendentes até aprovação explícita.

## Tarefas

- [ ] **Cenário 1: Enviar dados para outro device** (test-type: both)
  - blocked-by: spec 0101
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 1: Enviar dados para outro device” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que tenho dados locais (pets, pesos, etc.) E outro device com Petit está próximo QUANDO acesso Configurações > "Compartilhar dados" E toco em "Enviar dados" ENTÃO vejo código de 4 dígitos para compartilhar E aguardo conexão do receptor QUANDO receptor insere o código ENTÃO dados são enviados via Nearby Connections E vejo "Dados enviados com sucesso"
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 2: Receber dados de outro device** (test-type: both)
  - blocked-by: spec 0101; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 2: Receber dados de outro device” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que estou no app QUANDO acesso Configurações > "Receber dados" E toco em "Receber de outro celular" ENTÃO vejo campo para inserir código QUANDO insiro código de 4 dígitos do transmissor ENTÃO conexão é estabelecida E vejo progresso de transferência QUANDO transferência completa ENTÃO vejo opção "Substituir" ou "Mesclar" dados
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 3: Transferência sem internet** (test-type: both)
  - blocked-by: spec 0101; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 3: Transferência sem internet” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que ambos devices estão sem internet MAS estão na mesma rede Wi-Fi OU com Bluetooth ativo QUANDO inicio transferência ENTÃO funciona normalmente (Nearby Connections usa Wi-Fi Direct ou Bluetooth)
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 4: Mesclar dados recebidos** (test-type: both)
  - blocked-by: spec 0101; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 4: Mesclar dados recebidos” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que recebi dados de outro device E tenho dados locais QUANDO escolho "Mesclar" ENTÃO dados são combinados E duplicatas são resolvidas por ID (UUIDs únicos) E vejo resumo: "2 pets adicionados, 10 pesagens mescladas"
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 5: Substituir dados locais** (test-type: both)
  - blocked-by: spec 0101; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 5: Substituir dados locais” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que recebi dados de outro device QUANDO escolho "Substituir" ENTÃO vejo confirmação "Seus dados locais serão apagados. Continuar?" QUANDO confirmo ENTÃO todos dados locais são deletados E dados recebidos são importados E vejo "Dados restaurados com sucesso"
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 6: Cancelar transferência** (test-type: both)
  - blocked-by: spec 0101; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 6: Cancelar transferência” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que transferência está em andamento QUANDO toco em "Cancelar" ENTÃO transferência é interrompida E dados parciais são descartados E ambos devices voltam ao estado inicial
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`

- [ ] **Cenário 7: Erro de conexão** (test-type: both)
  - blocked-by: spec 0101; tarefa anterior desta spec
  - summary: entregar este comportamento como uma fatia vertical, incluindo domínio, persistência/serviço e interface quando aplicável.
  - desired behavior: o fluxo “Cenário 7: Erro de conexão” funciona de ponta a ponta sem comprometer os dados locais.
  - acceptance criteria: DADO que devices estão muito distantes OU Bluetooth/Wi-Fi estão desativados QUANDO tento iniciar transferência ENTÃO vejo mensagem "Não foi possível conectar. Aproxime os devices e ative Wi-Fi ou Bluetooth." ---
  - verification: `./gradlew test` e `./gradlew connectedDebugAndroidTest`
