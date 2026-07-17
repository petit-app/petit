# Tarefas: Transferência pontual de dados

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

## Tasks

- [x] **Enviar e importar um bundle pareado** (test-type: integration)
  - blocked-by: spec 0101
  - summary: serializar, transportar e apresentar opções de merge/replace.
  - desired behavior: o receptor recebe o conjunto compartilhável e escolhe como aplicá-lo.
  - acceptance criteria: o fluxo e os componentes correspondentes existem no app atual.
  - verification: `./gradlew test`

- [x] **Mesclar entidades por identidade e atualização** (test-type: unit)
  - blocked-by: bundle recebido
  - summary: inserir UUIDs novos e manter a versão com `updatedAt` mais recente.
  - desired behavior: reaplicar o mesmo bundle não duplica registros.
  - acceptance criteria: a estratégia de merge atual executa UUID + `updatedAt`.
  - verification: `./gradlew test`

- [ ] **Substituir a base compartilhável de forma transacional** (test-type: both)
  - blocked-by: validação do bundle
  - summary: remover registros locais ausentes e importar somente o conteúdo recebido.
  - desired behavior: replace reflete exatamente o bundle ou não altera nada em caso de falha.
  - acceptance criteria: registros locais ausentes deixam de existir; falha reverte toda a operação.
  - verification: `./gradlew test`

- [ ] **Exibir progresso, resultado e falhas confiáveis** (test-type: both)
  - blocked-by: envio e importação do bundle
  - summary: ligar bytes transferidos e resultado persistido aos estados da interface.
  - desired behavior: progresso é monotônico, contadores são exatos e payload parcial é descartado.
  - acceptance criteria: interrupção não persiste dados; conclusão mostra quantidades reais.
  - verification: `./gradlew test`

- [ ] **Validar transferência ponta a ponta em dois dispositivos** (test-type: integration)
  - blocked-by: replace transacional; progresso, resultado e falhas
  - summary: executar merge, replace e interrupção com e sem internet.
  - desired behavior: as bases finais e a interface correspondem à opção escolhida.
  - acceptance criteria: todos os cenários da spec passam em hardware real.
  - verification: `./gradlew assembleDebug && ./gradlew installDebug`
