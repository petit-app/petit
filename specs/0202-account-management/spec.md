---
spec: "0202"
title: "Gerenciamento de Conta"
family: identity-access
phase: 3
status: On Hold
owner: ""
depends_on: ["0201"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Gerenciamento de Conta

## Contexto e motivação

> Como usuário logado,
> Eu quero gerenciar minha conta (ver dados, fazer logout, deletar conta),
> Para que eu tenha controle sobre minha identidade no app.

Esta é uma hipótese histórica ainda não implementada. Produto, provedor externo, disponibilidade e monetização precisam ser revalidados antes de sua aprovação.

## Requisitos funcionais

### Cenário 1: Ver informações da conta

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que estou logado como "pessoa-a@example.com"
QUANDO acesso Configurações > Minha Conta
ENTÃO vejo:
  - Minha foto de perfil do Google
  - Meu nome "Pessoa A"
  - Meu email "pessoa-a@example.com"
  - Meu status de plano (Gratuito/Premium)
  - Data do último login
```

### Cenário 2: Fazer logout

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que estou logado
QUANDO toco em "Sair"
E confirmo a ação
ENTÃO sou deslogado do Firebase Auth
E volto ao estado "Anônimo"
E meus dados locais permanecem no dispositivo
E posso continuar usando o app sem login
```

### Cenário 3: Logout mantém dados locais

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que tenho 2 pets cadastrados
E estou logado
QUANDO faço logout
ENTÃO continuo vendo meus 2 pets
E posso adicionar novos dados
E os dados não são deletados
```

### Cenário 4: Trocar de conta

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que estou logado como "pessoa-a@example.com"
QUANDO faço logout
E faço login com "pessoa-b@example.com"
ENTÃO estou autenticado como Pessoa B
E os dados locais permanecem (da Pessoa A)
(associação de dados por conta é feita no sync - fases futuras)
```

### Cenário 5: Deletar conta

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que estou logado
QUANDO toco em "Deletar minha conta"
ENTÃO vejo aviso explicando consequências:
  - "Sua conta será removida do Firebase"
  - "Dados locais permanecerão no dispositivo"
  - "Dados na nuvem serão removidos em até 30 dias"
QUANDO confirmo digitando "DELETAR"
ENTÃO minha conta é removida do Firebase
E os dados na nuvem são agendados para purge em 30 dias
E sou deslogado
E volto ao modo anônimo
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

### Tela: Minha Conta

```
┌────────────────────────────────┐
│ ← Minha Conta                  │
├────────────────────────────────┤
│                                │
│         ┌──────────┐           │
│         │          │           │
│         │   📷     │           │
│         │          │           │
│         └──────────┘           │
│         Pessoa A               │
│   pessoa-a@example.com         │
│                                │
├────────────────────────────────┤
│                                │
│ 📊 STATUS                      │
│ ┌────────────────────────────┐ │
│ │ Plano: Gratuito            │ │
│ │ [Fazer upgrade ⭐]         │ │
│ └────────────────────────────┘ │
│                                │
│ 📅 ATIVIDADE                   │
│ ┌────────────────────────────┐ │
│ │ Último login: 18/03/2026   │ │
│ │ Membro desde: 01/01/2026   │ │
│ └────────────────────────────┘ │
│                                │
├────────────────────────────────┤
│                                │
│ ┌────────────────────────────┐ │
│ │         SAIR               │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │    DELETAR MINHA CONTA     │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Dialog: Confirmar Logout

```
┌────────────────────────────────┐
│           Sair                 │
├────────────────────────────────┤
│                                │
│ Você será desconectado da sua  │
│ conta Google.                  │
│                                │
│ Seus dados locais serão        │
│ mantidos no dispositivo.       │
│                                │
│ ┌──────────┐  ┌──────────────┐ │
│ │ CANCELAR │  │     SAIR     │ │
│ └──────────┘  └──────────────┘ │
└────────────────────────────────┘
```

### Dialog: Deletar Conta

```
┌────────────────────────────────┐
│     ⚠️ Deletar Conta           │
├────────────────────────────────┤
│                                │
│ Esta ação é irreversível!      │
│                                │
│ • Sua conta será removida      │
│ • Dados na nuvem serão removidos│
│   em até 30 dias               │
│ • Dados locais serão mantidos  │
│                                │
│ Digite DELETAR para confirmar: │
│ ┌────────────────────────────┐ │
│ │                            │ │
│ └────────────────────────────┘ │
│                                │
│ ┌──────────┐  ┌──────────────┐ │
│ │ CANCELAR │  │   DELETAR    │ │
│ └──────────┘  └──────────────┘ │
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
