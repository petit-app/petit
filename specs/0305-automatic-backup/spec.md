---
spec: "0305"
title: "Backup Automático"
family: backup-recovery
phase: 4
status: On Hold
owner: ""
depends_on: ["0301"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Backup Automático

## Contexto e motivação

> Como usuário logado,
> Eu quero que meus dados sejam salvos automaticamente no Google Drive todos os dias às 2h da madrugada,
> Para que eu não precise me preocupar em fazer backup manualmente.

Esta é uma hipótese histórica ainda não implementada. Produto, provedor externo, disponibilidade e monetização precisam ser revalidados antes de sua aprovação.

## Requisitos funcionais

### Cenário 1: Backup automático ativado por padrão (usuário logado)

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que estou logado com Google
QUANDO habilito backup automático nas configurações
ENTÃO o WorkManager agenda backup diário às 2h da madrugada
E vejo "Backup automático ativado — próximo às 2h"
```

### Cenário 2: Backup diário executa em background

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que backup automático está ativado
QUANDO chega 2h da madrugada
ENTÃO o backup é executado automaticamente
MESMO que o app esteja fechado
E não preciso abrir o app
E o backup é salvo no Google Drive
```

### Cenário 3: Backup apenas em Wi-Fi

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que "Backup apenas em Wi-Fi" está ativado
E estou conectado em rede móvel (4G/5G)
QUANDO o backup automático deveria executar
ENTÃO o backup é adiado
E executa quando conectar em Wi-Fi
```

### Cenário 4: Backup apenas se logado

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que backup automático está agendado
E não estou mais logado (logout)
QUANDO chega 2h da madrugada
ENTÃO o backup NÃO é executado
E vejo notificação "Faça login para continuar backups automáticos"
```

### Cenário 5: Configuração Wi-Fi only respeitada

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que "Backup apenas em Wi-Fi" está ativado
E estou conectado em rede móvel (4G/5G) às 2h
QUANDO o backup automático deveria executar
ENTÃO o backup é adiado
E executa quando conectar em Wi-Fi
E vejo notificação "Aguardando Wi-Fi para backup"
```

### Cenário 6: Sem internet

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que não tenho conexão de internet
QUANDO o backup automático deveria executar
ENTÃO o backup falha silenciosamente
E será tentado novamente na próxima vez
E posso ver "Último backup: há 2 dias (falhou)" nas configurações
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

### Configurações de Backup

```
┌────────────────────────────────┐
│ ← Backup Automático            │
├────────────────────────────────┤
│                                │
│ ☁️ BACKUP AUTOMÁTICO           │
│ ┌────────────────────────────┐ │
│ │ Ativado               [ON] │ │
│ └────────────────────────────┘ │
│                                │
│ 📊 STATUS                      │
│ ┌────────────────────────────┐ │
│ │ Último backup:             │ │
│ │ Hoje às 10:30 ✅           │ │
│ │                            │ │
│ │ Próximo backup:            │ │
│ │ Amanhã às 10:30            │ │
│ └────────────────────────────┘ │
│                                │
│ ⚙️ CONFIGURAÇÕES               │
│ ┌────────────────────────────┐ │
│ │ Frequência          24h  ▶ │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ Apenas em Wi-Fi      [ON]  │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ Notificar sucesso    [OFF] │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │    FAZER BACKUP AGORA      │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Notificação de Backup

```
┌────────────────────────────────┐
│ 🐱 Petit                         │
│ Backup realizado com sucesso   │
│ 2 pets salvos • 15.4 KB       │
│                                │
│                      [Ignorar] │
└────────────────────────────────┘
```

---

### Frequências Disponíveis

| Opção | Horas | Descrição |
|-------|-------|-----------|
| Frequente | 6 | A cada 6 horas |
| Diário | 24 | Uma vez por dia |
| Semanal | 168 | Uma vez por semana |

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
