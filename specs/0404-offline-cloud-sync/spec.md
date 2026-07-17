---
spec: "0404"
title: "Sincronização Offline-First"
family: cloud-sync
phase: 5
status: On Hold
owner: ""
depends_on: ["0401"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Sincronização Offline-First

## Contexto e motivação

> Como usuário premium,
> Eu quero que o app funcione normalmente mesmo offline,
> Para que eu possa registrar dados sem conexão e eles sincronizem depois.

Esta é uma hipótese histórica ainda não implementada. Produto, provedor externo, disponibilidade e monetização precisam ser revalidados antes de sua aprovação.

## Requisitos funcionais

### Cenário 1: Criar dados offline

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que estou sem internet
QUANDO cadastro um novo pet "Mia"
ENTÃO Mia é salva no Room (syncStatus = PENDING_SYNC)
E Mia aparece na lista normalmente
E vejo indicador "Pendente de sync" no item
```

### Cenário 2: Sync automático ao reconectar

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que tenho dados pendentes de sync
E estou offline
QUANDO a internet volta
ENTÃO o sync é iniciado automaticamente
E os dados pendentes são enviados
E o syncStatus muda para SYNCED
E o indicador de pendente desaparece
```

### Cenário 3: Múltiplas edições offline

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que estou offline
QUANDO faço várias edições:
  - Adiciono pet Mia
  - Adiciono pesagem para Mia
  - Edito nome de Luna para Luninha
ENTÃO todas as edições são salvas localmente
E todas ficam como PENDING_SYNC
E ao reconectar, todas são enviadas
```

### Cenário 4: Conflito após voltar online

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que editei Luna offline (updatedAt = 1000)
E outro dispositivo editou Luna online (updatedAt = 1500)
QUANDO volto online e sincronizo
ENTÃO a resolução de conflito acontece
E a versão mais recente (1500) vence
```

### Cenário 5: Queue de sync persiste após fechar app

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que fiz edições offline
E fecho o app
E reabro o app (ainda offline)
ENTÃO as edições ainda estão PENDING_SYNC
E ao reconectar, serão sincronizadas
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

### UI/UX

### Indicador em Item Pendente

```
┌────────────────────────────────┐
│ ← Meus Pets                    │
├────────────────────────────────┤
│ ┌──────────────────────────────┐
│ │ ┌────┐  Luna            ☁️✓  │  ← Synced
│ │ │ 📷 │  3.5 kg               │
│ │ └────┘                       │
│ └──────────────────────────────┘
│ ┌──────────────────────────────┐
│ │ ┌────┐  Mia             ☁️⏳  │  ← Pending
│ │ │ 📷 │  Novo                 │
│ │ └────┘                       │
│ └──────────────────────────────┘
└────────────────────────────────┘
```

### Banner de Status Offline

```
┌────────────────────────────────┐
│ ⚠️ Sem conexão                 │
│ Alterações serão sincronizadas │
│ quando a internet voltar.      │
└────────────────────────────────┘
┌────────────────────────────────┐
│ 🐱 Petit                    ⚙️    │
├────────────────────────────────┤
│ ...                            │
```

### Status de Sync com Detalhes

```
┌────────────────────────────────┐
│ ← Sincronização                │
├────────────────────────────────┤
│                                │
│ 📊 STATUS DA SYNC              │
│ ┌────────────────────────────┐ │
│ │ ⚠️ 3 itens pendentes       │ │
│ │                            │ │
│ │ • 1 pet novo              │ │
│ │ • 1 pesagem                │ │
│ │ • 1 vacina editada         │ │
│ │                            │ │
│ │ Aguardando conexão...      │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
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
