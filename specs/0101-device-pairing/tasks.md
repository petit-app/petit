# Tarefas: Pareamento de dispositivos

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [x] **Disponibilizar descoberta e persistência do grupo** (test-type: integration)
  - blocked-by: nenhum
  - summary: integrar Nearby, permissões, UI, repositórios e DataStore.
  - desired behavior: o app inicia ou encerra advertising/discovery e mantém a identidade local do grupo.
  - acceptance criteria: os componentes existem no fluxo atual sem dependência de cloud.
  - verification: `./gradlew test`

- [ ] **Autorizar o pareamento com código de quatro dígitos** (test-type: both)
  - blocked-by: descoberta e persistência do grupo
  - summary: gerar, exibir, receber, validar e expirar o código.
  - desired behavior: somente o receptor com o código correto conclui o pareamento.
  - acceptance criteria: código correto conecta; incorreto é rejeitado; nova tentativa é possível.
  - verification: `./gradlew test`

- [ ] **Tornar cancelamento e falhas atômicos** (test-type: both)
  - blocked-by: autorização por código
  - summary: limpar advertising, discovery, conexão e estado incompleto.
  - desired behavior: interrupções nunca deixam chave ou membro parcialmente persistidos.
  - acceptance criteria: cancelar ou perder o endpoint retorna ao estado inicial sem dados residuais.
  - verification: `./gradlew test`

- [ ] **Validar pareamento ponta a ponta em dois dispositivos** (test-type: integration)
  - blocked-by: autorização por código; cancelamento e falhas atômicos
  - summary: executar a matriz de aceite em hardware real.
  - desired behavior: pareamento funciona com e sem internet e respeita permissões.
  - acceptance criteria: dois dispositivos persistem a mesma chave; código inválido e cancelamento não pareiam.
  - verification: `./gradlew assembleDebug && ./gradlew installDebug`
