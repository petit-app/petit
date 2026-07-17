---
spec: "0301"
title: "Backup Manual"
family: backup-recovery
phase: 4
status: On Hold
owner: ""
depends_on: ["0201"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Backup Manual

## Contexto e motivação

> Como usuário do app,
> Eu quero fazer backup dos meus dados no Google Drive,
> Para que eu possa recuperá-los em caso de perda do celular.

Esta é uma hipótese histórica ainda não implementada. Produto, provedor externo, disponibilidade e monetização precisam ser revalidados antes de sua aprovação.

## Requisitos funcionais

### Cenário 1: Fazer backup com sucesso (usuário já logado)

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que estou logado com Google
E tenho conexão com internet
QUANDO acesso Configurações > "Backup Google Drive"
E toco em "Fazer backup agora"
ENTÃO vejo indicador de progresso
E o backup é enviado para o Google Drive (appDataFolder)
E vejo mensagem "Backup realizado com sucesso"
E vejo data/hora do último backup
```

### Cenário 2: Backup sem internet

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que estou sem conexão de internet
QUANDO tento fazer backup
ENTÃO vejo mensagem "Sem conexão. Conecte-se à internet para fazer backup."
E o backup não é iniciado
```

### Cenário 3: Backup sem estar logado (ativa login)

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que não estou logado
QUANDO tento fazer backup
ENTÃO vejo dialog explicando que é necessário login Google
E tenho opção "Entrar com Google"
QUANDO faço login com sucesso
ENTÃO o backup é iniciado automaticamente
```

### Cenário 4: Primeiro backup

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que nunca fiz backup antes
QUANDO faço meu primeiro backup
ENTÃO o arquivo é criado no appDataFolder do Google Drive
E o metadata é inicializado
E vejo "Backup realizado com sucesso"
```

### Cenário 5: Backup subsequente

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que já tenho backups anteriores
QUANDO faço novo backup
ENTÃO um novo arquivo é criado (não substitui o anterior)
E o metadata é atualizado
E backups antigos são mantidos (até o limite)
```

### Cenário 6: Erro durante backup

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que inicio um backup
QUANDO ocorre erro (rede cai, quota excedida, etc.)
ENTÃO vejo mensagem de erro específica
E o backup parcial é descartado
E posso tentar novamente
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

### Tela: Backup na Nuvem

```
┌────────────────────────────────┐
│ ← Backup na Nuvem              │
├────────────────────────────────┤
│                                │
│ ☁️ GOOGLE DRIVE                 │
│                                │
│ Conectado como:                │
│ pessoa-a@example.com           │
│                                │
├────────────────────────────────┤
│                                │
│ 📊 ÚLTIMO BACKUP               │
│ ┌────────────────────────────┐ │
│ │ 18/03/2026 às 10:30        │ │
│ │ 2 pets • 15.4 KB          │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │    FAZER BACKUP AGORA      │ │
│ └────────────────────────────┘ │
│                                │
├────────────────────────────────┤
│                                │
│ 📂 BACKUPS SALVOS          ▶   │
│ 3 backups (45.2 KB total)      │
│                                │
├────────────────────────────────┤
│                                │
│ ℹ️ Os backups são armazenados  │
│ no appDataFolder do Google    │
│ Drive (oculto).               │
│                                │
└────────────────────────────────┘
```

### Tela: Backup na Nuvem (Fazendo Backup)

```
┌────────────────────────────────┐
│ ← Backup na Nuvem              │
├────────────────────────────────┤
│                                │
│                                │
│         ┌─────────┐            │
│         │  ████░░ │            │
│         └─────────┘            │
│                                │
│      Fazendo backup...         │
│      Enviando dados            │
│                                │
│      Não feche o app           │
│                                │
│                                │
└────────────────────────────────┘
```

### Estado: Sucesso

```
┌────────────────────────────────┐
│                                │
│            ✅                  │
│                                │
│   Backup realizado com         │
│   sucesso!                     │
│                                │
│   18/03/2026 às 10:30          │
│   15.4 KB                      │
│                                │
│   ┌────────────────────────┐   │
│   │          OK            │   │
│   └────────────────────────┘   │
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
