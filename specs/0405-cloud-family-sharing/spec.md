---
spec: "0405"
title: "Compartilhamento Familiar na Nuvem"
family: cloud-sync
phase: 5
status: On Hold
owner: ""
depends_on: ["0201", "0401"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Compartilhamento Familiar na Nuvem

## Contexto e motivação

> Como usuário premium,
> Eu quero compartilhar os dados dos meus pets com minha família,
> Para que todos possamos acompanhar e registrar informações dos pets juntos.

Esta é uma hipótese histórica ainda não implementada. Produto, provedor externo, disponibilidade e monetização precisam ser revalidados antes de sua aprovação.

## Requisitos funcionais

### Cenário 1: Criar grupo familiar

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que sou usuário premium
QUANDO acesso Configurações > "Família"
E toco em "Criar grupo familiar"
ENTÃO um grupo é criado
E eu me torno o administrador
E recebo um código de convite
```

### Cenário 2: Convidar membro

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que sou admin de um grupo familiar
QUANDO compartilho o código de convite "PETIT-ABC123"
E outra pessoa insere o código no app dela
ENTÃO ela entra no grupo familiar
E vê todos os pets do grupo
```

### Cenário 3: Todos veem e editam

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que Pessoa A e Pessoa B estão no mesmo grupo familiar
QUANDO Pessoa B adiciona uma pesagem para um pet
ENTÃO Pessoa A vê a pesagem automaticamente
E Pessoa A também pode adicionar/editar dados
```

### Cenário 4: Permissões de admin

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que sou admin do grupo
QUANDO acesso a lista de membros
ENTÃO posso:
  - Remover membros
  - Gerar novo código de convite
  - Deletar o grupo
```

### Cenário 5: Membro sai do grupo

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que sou membro de um grupo familiar
QUANDO escolho "Sair do grupo"
ENTÃO perco acesso aos dados compartilhados
E os dados permanecem para os outros membros
E meus dados pessoais (não compartilhados) permanecem comigo
```

### Cenário 6: Pets privados vs compartilhados

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que sou membro de um grupo familiar
QUANDO cadastro um novo pet
ENTÃO posso escolher:
  - "Compartilhar com família" (todos veem)
  - "Manter privado" (só eu vejo)
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

### Tela: Família

```
┌────────────────────────────────┐
│ ← Família                      │
├────────────────────────────────┤
│                                │
│ 👨‍👩‍👧 GRUPO FAMILIAR             │
│ ┌────────────────────────────┐ │
│ │ Família Exemplo            │ │
│ │ 3 membros                  │ │
│ └────────────────────────────┘ │
│                                │
│ 👥 MEMBROS                     │
│ ┌────────────────────────────┐ │
│ │ 👤 Pessoa A (você)          │ │
│ │    Admin                   │ │
│ │                            │ │
│ │ 👤 Pessoa B                 │ │
│ │    Membro                  │ │
│ │                            │ │
│ │ 👤 Pessoa C                 │ │
│ │    Membro                  │ │
│ └────────────────────────────┘ │
│                                │
│ 🔗 CONVITE                     │
│ ┌────────────────────────────┐ │
│ │ Código: PETIT-ABC123         │ │
│ │ [Copiar]  [Compartilhar]   │ │
│ │                            │ │
│ │ Expira em 7 dias           │ │
│ │ [Gerar novo código]        │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │     SAIR DO GRUPO          │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Tela: Entrar em Grupo

```
┌────────────────────────────────┐
│ ← Entrar em Grupo Familiar     │
├────────────────────────────────┤
│                                │
│ Digite o código de convite:    │
│                                │
│ ┌────────────────────────────┐ │
│ │ PETIT-                       │ │
│ └────────────────────────────┘ │
│                                │
│ Peça o código para quem        │
│ criou o grupo familiar.        │
│                                │
│ ┌────────────────────────────┐ │
│ │         ENTRAR             │ │
│ └────────────────────────────┘ │
│                                │
│           ou                   │
│                                │
│ ┌────────────────────────────┐ │
│ │   CRIAR NOVO GRUPO         │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Seletor ao Criar Pet

```
┌────────────────────────────────┐
│                                │
│ Compartilhamento               │
│                                │
│ ○ 👤 Privado                   │
│   Apenas você verá este pet   │
│                                │
│ ● 👨‍👩‍👧 Família Exemplo           │
│   Todos os membros verão       │
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
