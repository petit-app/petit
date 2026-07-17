# Tarefas: Grupo familiar local

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [x] **Consultar e gerenciar o grupo local** (test-type: integration)
  - blocked-by: spec 0101
  - summary: listar membros, remover localmente, sair e persistir preferências.
  - desired behavior: a pessoa controla seus vínculos sem afetar os dados dos pets.
  - acceptance criteria: lista, remoção local, saída e DataStore existem no app atual.
  - verification: `./gradlew test`

- [ ] **Renomear o dispositivo por identidade estável** (test-type: both)
  - blocked-by: consulta do grupo local
  - summary: editar o nome sem alterar o UUID do membro.
  - desired behavior: o novo nome persiste após reinício e fica disponível para propagação.
  - acceptance criteria: nome editado aparece localmente; identidade e chave não mudam.
  - verification: `./gradlew test`

- [ ] **Propagar mudanças de membresia** (test-type: both)
  - blocked-by: spec 0102; renomear o dispositivo
  - summary: sincronizar renomeação, remoção e saída como eventos idempotentes.
  - desired behavior: os pares convergem e uma chave revogada não recupera acesso.
  - acceptance criteria: segundo dispositivo observa a mudança e rejeita o membro removido.
  - verification: `./gradlew test`

- [ ] **Exibir última sincronização e estados do grupo** (test-type: unit)
  - blocked-by: consulta do grupo local
  - summary: apresentar horário conhecido, “nunca sincronizou” e grupo vazio.
  - desired behavior: a pessoa entende a atualidade de cada vínculo.
  - acceptance criteria: todos os estados têm conteúdo acessível e localizado.
  - verification: `./gradlew test`

- [ ] **Validar remoção e saída em dois dispositivos** (test-type: integration)
  - blocked-by: propagação de membresia; estados do grupo
  - summary: executar renomeação, remoção, saída e tentativa com chave antiga.
  - desired behavior: ambos convergem sem apagar dados dos pets.
  - acceptance criteria: vínculo é revogado nos dois lados e o histórico local permanece.
  - verification: `./gradlew assembleDebug && ./gradlew installDebug`
