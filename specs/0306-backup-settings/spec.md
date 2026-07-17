---
spec: "0306"
title: "Configurações de Backup"
family: backup-recovery
phase: 4
status: On Hold
owner: ""
depends_on: ["0305"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Configurações de Backup

## Contexto e motivação

> Como usuário logado,
> Eu quero configurar como o backup automático funciona,
> Para que eu possa otimizar consumo de dados e bateria.

Esta é uma hipótese histórica ainda não implementada. Produto, provedor externo, disponibilidade e monetização precisam ser revalidados antes de sua aprovação.

## Requisitos funcionais

### Cenário 1: Ativar/desativar backup automático

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que estou logado com Google
QUANDO acesso Configurações > Backup Automático
E ativo o toggle "Backup automático"
ENTÃO o backup diário às 2h é agendado
E vejo "Próximo backup: hoje/amanhã às 2h"

QUANDO desativo o toggle
ENTÃO o agendamento é cancelado
E vejo "Backup automático desativado"
```

### Cenário 2: Configurar Wi-Fi only

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que backup automático está ativado
E "Apenas em Wi-Fi" está desativado
QUANDO ativo "Apenas em Wi-Fi"
ENTÃO backups futuros só executam em Wi-Fi
E o agendamento atual é ajustado

DADO que estou em rede móvel às 2h
E "Apenas em Wi-Fi" está ativado
QUANDO o backup deveria executar
ENTÃO é adiado até conectar em Wi-Fi
```

### Cenário 3: Ver histórico de backups

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que tenho backups automáticos realizados
QUANDO acesso "Ver histórico"
ENTÃO vejo lista dos últimos backups
E cada item mostra:
  - Data/hora
  - Se foi automático ou manual
  - Status (sucesso/falha)
```

### Cenário 5: Notificação de backup

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que "Notificar após backup" está ativado
QUANDO um backup automático é realizado com sucesso
ENTÃO recebo uma notificação silenciosa
"Backup realizado: 2 pets, 15 KB"

DADO que "Notificar após backup" está desativado
QUANDO um backup é realizado
ENTÃO NÃO recebo notificação
```

### Cenário 6: Forçar backup agora

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que estou na tela de configurações de backup
QUANDO toco em "Fazer backup agora"
ENTÃO um backup é executado imediatamente
E o timer do próximo backup automático é resetado
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

### Tela: Configurações de Backup Automático

```
┌────────────────────────────────┐
│ ← Backup Automático            │
├────────────────────────────────┤
│                                │
│ ☁️ BACKUP AUTOMÁTICO           │
│ ┌────────────────────────────┐ │
│ │ Ativar                [ON] │ │
│ └────────────────────────────┘ │
│                                │
│ ℹ️ Seus dados são salvos       │
│ automaticamente no Firebase   │
│ Storage, mesmo com o app      │
│ fechado.                      │
│                                │
├────────────────────────────────┤
│                                │
│ 📊 STATUS                      │
│ ┌────────────────────────────┐ │
│ │ ✅ Último: Hoje 10:30      │ │
│ │ ⏰ Próximo: Amanhã 10:30   │ │
│ └────────────────────────────┘ │
│                                │
├────────────────────────────────┤
│                                │
│ ⚙️ CONFIGURAÇÕES               │
│                                │
│ ┌────────────────────────────┐ │
│ │ Frequência                 │ │
│ │ A cada 24 horas          ▶ │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │ Apenas em Wi-Fi      [ON]  │ │
│ │ Economiza dados móveis     │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │ Notificar sucesso   [OFF]  │ │
│ │ Mostra notificação após    │ │
│ │ cada backup                │ │
│ └────────────────────────────┘ │
│                                │
├────────────────────────────────┤
│                                │
│ ┌────────────────────────────┐ │
│ │    FAZER BACKUP AGORA      │ │
│ └────────────────────────────┘ │
│                                │
│ Ver histórico de backups    ▶  │
│                                │
└────────────────────────────────┘
```

### Bottom Sheet: Frequência

```
┌────────────────────────────────┐
│                    ─────       │
│                                │
│ Frequência do backup           │
│                                │
│ ○ A cada 6 horas               │
│   Mais proteção, mais dados    │
│                                │
│ ● A cada 24 horas              │
│   Recomendado                  │
│                                │
│ ○ Uma vez por semana           │
│   Menor consumo                │
│                                │
└────────────────────────────────┘
```

### Tela: Histórico de Backups

```
┌────────────────────────────────┐
│ ← Histórico de Backups         │
├────────────────────────────────┤
│                                │
│ Março 2026                     │
│ ┌────────────────────────────┐ │
│ │ ✅ 18/03 10:30  Automático │ │
│ │    2 pets • 15.4 KB       │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ ✅ 17/03 10:30  Automático │ │
│ │    2 pets • 15.2 KB       │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ ✅ 16/03 14:00  Manual     │ │
│ │    2 pets • 15.1 KB       │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ ❌ 15/03 10:30  Automático │ │
│ │    Falhou: Sem conexão     │ │
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
