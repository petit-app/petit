---
spec: "0307"
title: "Gatilhos de Backup"
family: backup-recovery
phase: 4
status: On Hold
owner: ""
depends_on: ["0305", "0306"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Gatilhos de Backup

## Contexto e motivação

> Como usuário premium,
> Eu quero que o backup seja feito automaticamente após eu fazer alterações importantes,
> Para que meus dados mais recentes estejam sempre protegidos.

Esta é uma hipótese histórica ainda não implementada. Produto, provedor externo, disponibilidade e monetização precisam ser revalidados antes de sua aprovação.

## Requisitos funcionais

### Cenário 1: Backup após criar pet

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que backup automático está ativado
E tenho conexão de internet
QUANDO cadastro um novo pet
ENTÃO após 5 minutos de inatividade
O backup é executado automaticamente
E inclui o novo pet
```

### Cenário 2: Debounce de múltiplas alterações

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que faço várias alterações em sequência:
  - Adiciono pet Luna
  - Adiciono pesagem 3.5kg
  - Adiciono vacina V3
  - Tudo em menos de 5 minutos
ENTÃO apenas UM backup é executado
(após 5 minutos da última alteração)
E inclui todas as mudanças
```

### Cenário 3: Backup após delete

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que backup automático está ativado
QUANDO deleto um pet
ENTÃO após 5 minutos sem alterações
O backup é executado
E reflete a exclusão
```

### Cenário 4: Cancelar backup pendente

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que alterei dados e backup está pendente (em 3 min)
QUANDO faço outra alteração
ENTÃO o timer é resetado para 5 minutos novamente
E apenas um backup será feito
```

### Cenário 5: Não duplicar com backup periódico

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que um backup por alteração está pendente
E o backup periódico deveria executar agora
ENTÃO apenas um backup é feito
E o timer de backup por alteração é cancelado
```

### Cenário 6: App fechado após alteração

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que fiz alterações
E fecho o app imediatamente
ENTÃO o backup pendente ainda será executado
(WorkManager persiste a tarefa)
```

---

## Requisitos não funcionais

- [ ] Preservar a operação local do Petit quando autenticação, rede ou serviço externo estiver indisponível.
- [ ] Proteger dados pessoais e de saúde do pet durante armazenamento, transporte e exclusão.
- [ ] Oferecer estados de carregamento, sucesso, vazio e erro acessíveis e compreensíveis.
- [ ] Evitar perda ou duplicação silenciosa de dados em operações interrompidas.

## Estratégia de testes

| Escopo | Cobertura esperada |
| --- | --- |
| Unitário | Regras de elegibilidade, validação, estado, conflito e transformação de dados. |
| Integração | Fluxos que cruzam interface, repositórios, banco local e provedores externos. |
| Ambos | Cada tarefa vertical usa teste unitário para regras e integração para limites com I/O. |

## Critérios de aceite

Os cenários em **Requisitos funcionais** são os critérios testáveis desta spec e devem possuir cobertura rastreável antes de o status avançar para `Implemented`.

## Notas de produto preservadas

### Triggers de Backup

| Evento | Trigger? | Debounce |
|--------|----------|----------|
| Criar pet | ✅ | 5 min |
| Editar pet | ✅ | 5 min |
| Deletar pet | ✅ | 5 min |
| Adicionar pesagem | ✅ | 5 min |
| Adicionar vacina | ✅ | 5 min |
| Adicionar vermífugo | ✅ | 5 min |
| Criar lembrete | ❌ | - |
| Editar configurações | ❌ | - |

---

### UI Feedback (Opcional)

### Indicador Sutil

Não mostrar nada visualmente. O backup por alteração é "invisível" para o usuário, apenas garante que dados estão protegidos.

### Para Debug/Desenvolvimento

```kotlin
// Apenas em debug builds
if (BuildConfig.DEBUG && hasPendingBackup()) {
    Snackbar.make(
        view,
        "Backup pendente em ${getRemainingTime()} min",
        Snackbar.LENGTH_SHORT
    ).show()
}
```

---

## Casos extremos

- O dispositivo perde conectividade ou o processo é interrompido no meio da operação.
- A sessão expira, muda de conta ou não possui autorização suficiente.
- Dados locais e remotos divergem, estão incompletos ou foram criados por versões diferentes do app.
- O provedor externo está indisponível, limita quota ou altera sua API.

## Decisões

| Decisão | Escolha atual | Motivo |
| --- | --- | --- |
| Estado da proposta | On Hold | A demanda e o modelo do produto ainda precisam ser validados. |
| Tecnologia externa | Não decidida | Firebase, Google Drive e APIs citadas são opções históricas, não compromissos atuais. |
| Fonte de verdade local | Preservar Room como base offline | Mantém o Petit útil sem conta ou conectividade. |

## Fora de escopo

- Implementar esta proposta antes de revisão, aprovação explícita e atualização do índice.
- Tratar exemplos históricos de preço, tier, provedor ou cronograma como decisão vigente.
- Funcionalidades cobertas pelas specs declaradas em `depends_on`.
