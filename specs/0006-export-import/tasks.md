# Tarefas: Exportação e importação JSON

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [x] **Exportar todos os domínios em JSON** (test-type: both)
  - blocked-by: 0001, 0002, 0003, 0004, 0005
  - desired behavior: gerar bundle versionado e escrevê-lo na URI escolhida.
  - acceptance criteria: arquivo nomeado com data contém metadados e todos os domínios.
  - verification: `./gradlew test`
- [x] **Analisar e importar backup** (test-type: both)
  - blocked-by: exportar todos os domínios em JSON
  - desired behavior: validar, resumir e importar com estratégia de conflito.
  - acceptance criteria: arquivo inválido não altera dados; merge usa `updatedAt`; operação é atômica.
  - verification: `./gradlew test`
- [x] **Integrar seleção de documentos** (test-type: integration)
  - blocked-by: exportar todos os domínios em JSON, analisar e importar backup
  - desired behavior: usar seletor de documentos para abrir e criar backup.
  - acceptance criteria: usuário escolhe origem/destino sem acesso direto inseguro ao filesystem.
  - verification: `./gradlew test`
- [ ] **Integrar exportação ao share sheet** (test-type: integration)
  - blocked-by: integrar seleção de documentos
  - desired behavior: permitir compartilhar o backup por apps compatíveis.
  - acceptance criteria: share sheet recebe a URI com permissão temporária de leitura.
  - verification: `./gradlew test`
- [ ] **Expor exportação de um pet no perfil** (test-type: integration)
  - blocked-by: exportar todos os domínios em JSON
  - desired behavior: chamar `exportForPet(petId)` a partir do perfil.
  - acceptance criteria: bundle contém somente o pet selecionado e seus registros relacionados.
  - verification: `./gradlew test`
- [ ] **Adicionar regressões de serialização e importação** (test-type: both)
  - blocked-by: analisar e importar backup
  - desired behavior: cobrir round-trip, conflitos, corrupção, atomicidade e versões antigas.
  - acceptance criteria: todos os critérios de aceite têm cobertura automatizada.
  - verification: `./gradlew test`
