# Tarefas: Sincronização na rede local

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [ ] **Definir um protocolo local versionado e autenticado** (test-type: unit)
  - blocked-by: specs 0101 e 0103 aprovadas
  - summary: modelar mensagens, limites, erros, ACK e regra para sessões simultâneas.
  - desired behavior: nenhuma entidade é enviada antes de validar versão, chave e membro.
  - acceptance criteria: mensagens válidas round-trip; chave/versão inválida encerra a sessão.
  - verification: `./gradlew test`

- [ ] **Descobrir pares com NSD durante o foreground** (test-type: integration)
  - blocked-by: protocolo local
  - summary: registrar, descobrir, resolver e filtrar `_petit._tcp` com lifecycle e timeout.
  - desired behavior: pares são encontrados sem anunciar indefinidamente fora do app.
  - acceptance criteria: encontra o outro processo, ignora a si mesmo e libera listeners em `ON_STOP`.
  - verification: `./gradlew test`

- [ ] **Sincronizar changesets por TCP de forma idempotente** (test-type: both)
  - blocked-by: protocolo local; descoberta NSD; spec 0105
  - summary: trocar lotes bidirecionais, aplicar em transação e confirmar com ACK.
  - desired behavior: repetição e queda de conexão convergem sem duplicar ou perder dados.
  - acceptance criteria: dois processos convergem; ACK perdido permite reenvio seguro.
  - verification: `./gradlew test`

- [ ] **Agendar tentativas econômicas em background** (test-type: integration)
  - blocked-by: sync TCP
  - summary: criar trabalho periódico único com rede conectada e backoff.
  - desired behavior: Android agenda tentativas sem serviço contínuo ou Wi-Fi Direct persistente.
  - acceptance criteria: constraints, periodicidade mínima e política de unicidade são verificadas.
  - verification: `./gradlew test`

- [ ] **Expor configuração e estado de sincronização** (test-type: both)
  - blocked-by: sync TCP; trabalho periódico
  - summary: implementar on/off, tentativa manual e indicador global acessível.
  - desired behavior: pessoa entende e controla a sincronização local.
  - acceptance criteria: desativar para NSD/worker; estados são localizados e não dependem só de cor.
  - verification: `./gradlew test`

- [ ] **Validar sincronização LAN em dois dispositivos** (test-type: integration)
  - blocked-by: configuração e estado
  - summary: testar mesma Wi-Fi, mudanças bidirecionais, reconexão, background e chave inválida.
  - desired behavior: dispositivos convergem com consumo e lifecycle previstos.
  - acceptance criteria: todos os critérios da spec passam em hardware real.
  - verification: `./gradlew assembleDebug && ./gradlew installDebug`
