---
spec: "0201"
title: "Login com Google"
family: identity-access
phase: 3
status: On Hold
owner: ""
depends_on: []
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Login com Google

## Contexto e motivação

> Como usuário do app,
> Eu quero fazer login com minha conta Google,
> Para que eu possa fazer backup dos meus dados no Google Drive e acessar recursos premium.

Esta é uma hipótese histórica ainda não implementada. Produto, provedor externo, disponibilidade e monetização precisam ser revalidados antes de sua aprovação.

## Requisitos funcionais

### Cenário 1: Login bem-sucedido

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que estou usando o app sem login
QUANDO toco em "Entrar com Google"
ENTÃO vejo o seletor de contas Google do sistema
QUANDO seleciono minha conta
E autorizo o app
ENTÃO sou autenticado com sucesso
E vejo meu nome/foto na tela de configurações
E o estado muda para "Authenticated"
```

### Cenário 2: Primeiro login associa dados existentes

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que tenho dados locais (pets, pesos, etc.)
E nunca fiz login antes
QUANDO faço login com Google pela primeira vez
ENTÃO meus dados locais são associados ao meu userId
E posso continuar usando normalmente
```

### Cenário 3: Login cancelado

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que inicio o processo de login
QUANDO cancelo o seletor de contas
OU fecho o dialog
ENTÃO volto ao estado anterior (anônimo)
E não vejo mensagem de erro
E posso tentar novamente
```

### Cenário 4: Erro de rede no login

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que estou sem conexão de internet
QUANDO tento fazer login
ENTÃO vejo mensagem "Sem conexão. Tente novamente."
E continuo no modo anônimo
E o app continua funcionando normalmente offline
```

### Cenário 5: Login ativado ao tentar backup

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que estou usando o app sem login
E tenho dados locais (pets, pesos, etc.)
QUANDO tento fazer "Backup para Google Drive"
ENTÃO vejo dialog explicando que é necessário login
E tenho opção "Entrar com Google"
QUANDO faço login com sucesso
ENTÃO o backup é iniciado automaticamente
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

### Tela de Configurações (Não Logado)

```
┌────────────────────────────────┐
│ ← Configurações                │
├────────────────────────────────┤
│                                │
│ 👤 CONTA                       │
│ ┌────────────────────────────┐ │
│ │        🔒                  │ │
│ │  Você não está logado      │ │
│ │                            │ │
│ │  Faça login para proteger  │ │
│ │  seus dados e acessar      │ │
│ │  recursos premium.         │ │
│ │                            │ │
│ │ ┌────────────────────────┐ │ │
│ │ │ 🔵 Entrar com Google   │ │ │
│ │ └────────────────────────┘ │ │
│ └────────────────────────────┘ │
│                                │
│ 📦 DADOS                       │
│ ...                            │
└────────────────────────────────┘
```

### Tela de Configurações (Logado)

```
┌────────────────────────────────┐
│ ← Configurações                │
├────────────────────────────────┤
│                                │
│ 👤 CONTA                       │
│ ┌────────────────────────────┐ │
│ │ ┌────┐                     │ │
│ │ │ 📷 │ Pessoa A            │ │
│ │ └────┘ pessoa-a@example.com │ │
│ │        Plano: Gratuito     │ │
│ │                            │ │
│ │ [Gerenciar conta]  [Sair]  │ │
│ └────────────────────────────┘ │
│                                │
│ ⭐ PREMIUM                     │
│ ┌────────────────────────────┐ │
│ │ Desbloqueie sync na nuvem, │ │
│ │ backup automático e mais!  │ │
│ │ [Ver planos]               │ │
│ └────────────────────────────┘ │
└────────────────────────────────┘
```

### Fluxo de Login

```
┌──────────────────────────────────────────────────┐
│                                                  │
│  ┌────────────────────────────────────────────┐  │
│  │                                            │  │
│  │  Escolha uma conta                         │  │
│  │                                            │  │
│  │  ┌────┐  pessoa-a@example.com              │  │
│  │  │ 📷 │  Pessoa A                          │  │
│  │  └────┘                                    │  │
│  │                                            │  │
│  │  ┌────┐  pessoa-b@example.com              │  │
│  │  │ 📷 │  Pessoa B                          │  │
│  │  └────┘                                    │  │
│  │                                            │  │
│  │  ┌────────────────────────────────────┐   │  │
│  │  │ + Usar outra conta                 │   │  │
│  │  └────────────────────────────────────┘   │  │
│  │                                            │  │
│  └────────────────────────────────────────────┘  │
│                                                  │
└──────────────────────────────────────────────────┘
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
