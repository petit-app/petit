---
spec: "0401"
title: "Sincronização em Tempo Real"
family: cloud-sync
phase: 5
status: On Hold
owner: ""
depends_on: ["0201"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Sincronização em Tempo Real

## Contexto e motivação

> Como usuário premium,
> Eu quero que meus dados sincronizem automaticamente com a nuvem,
> Para que eles estejam sempre atualizados e disponíveis em qualquer dispositivo.

Esta é uma hipótese histórica ainda não implementada. Produto, provedor externo, disponibilidade e monetização precisam ser revalidados antes de sua aprovação.

## Requisitos funcionais

### Cenário 1: Sync após criar dados

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que sou usuário premium com sync ativado
E tenho conexão de internet
QUANDO cadastro um novo pet "Luna"
ENTÃO Luna é salva no Room imediatamente (syncStatus = PENDING)
E após alguns segundos, Luna é enviada para o Firestore
E o syncStatus muda para SYNCED
E vejo indicador de sync ✓
```

### Cenário 2: Sync em tempo real recebendo dados

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que tenho o app aberto
E alguém (ou outro dispositivo) adiciona dados no Firestore
QUANDO a mudança é detectada pelo snapshot listener do Firestore
ENTÃO os novos dados são baixados automaticamente
E salvos no Room local
E aparecem na UI sem precisar atualizar manualmente
```

### Cenário 3: Sync sem internet (queue)

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que estou sem internet
QUANDO cadastro um novo pet
ENTÃO o pet é salvo no Room (syncStatus = PENDING)
E o pet aparece na UI normalmente
E quando a internet voltar, o sync acontece automaticamente
```

### Cenário 4: Ativar sync pela primeira vez

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que tenho dados locais
E nunca sincronizei antes
QUANDO ativo "Sincronização na nuvem" nas configurações
ENTÃO todos os dados locais são enviados para o Firestore
E vejo progresso "Sincronizando X de Y itens..."
E ao final, todos estão com syncStatus = SYNCED
```

### Cenário 5: Premium expira

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que meu premium expira
QUANDO isso acontece
ENTÃO o snapshot listener do Firestore é desconectado
E novos dados são salvos apenas localmente (syncStatus = LOCAL_ONLY)
E os dados já sincronizados permanecem no dispositivo
E vejo aviso "Sincronização pausada - Renove seu premium"
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

### Indicador de Sync na Toolbar

```
┌────────────────────────────────┐
│ 🐱 Petit                 ☁️✓  ⚙️  │  ← Sync OK
├────────────────────────────────┤
│ ...                            │
└────────────────────────────────┘

┌────────────────────────────────┐
│ 🐱 Petit                 ☁️⟳  ⚙️  │  ← Sincronizando
├────────────────────────────────┤
│ ...                            │
└────────────────────────────────┘

┌────────────────────────────────┐
│ 🐱 Petit                 ☁️!  ⚙️  │  ← Pendente (sem internet)
├────────────────────────────────┤
│ ...                            │
└────────────────────────────────┘
```

### Configuração de Sync

```
┌────────────────────────────────┐
│ ← Sincronização                │
├────────────────────────────────┤
│                                │
│ ☁️ SINCRONIZAÇÃO NA NUVEM      │
│ ┌────────────────────────────┐ │
│ │ Ativar                [ON] │ │
│ └────────────────────────────┘ │
│                                │
│ ℹ️ Seus dados são sincronizados│
│ automaticamente entre todos   │
│ os seus dispositivos.         │
│                                │
├────────────────────────────────┤
│                                │
│ 📊 STATUS                      │
│ ┌────────────────────────────┐ │
│ │ ✅ Sincronizado            │ │
│ │ Última sync: há 2 min      │ │
│ │                            │ │
│ │ 2 pets • 15 pesagens      │ │
│ │ 8 vacinas • 6 vermífugos   │ │
│ └────────────────────────────┘ │
│                                │
├────────────────────────────────┤
│                                │
│ ⚙️ OPÇÕES                      │
│ ┌────────────────────────────┐ │
│ │ Sync apenas em Wi-Fi [OFF] │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │    FORÇAR SYNC COMPLETO    │ │
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
