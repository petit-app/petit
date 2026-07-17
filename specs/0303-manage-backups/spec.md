---
spec: "0303"
title: "Gerenciar Backups"
family: backup-recovery
phase: 4
status: On Hold
owner: ""
depends_on: ["0301"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Gerenciar Backups

## Contexto e motivação

> Como usuário logado,
> Eu quero gerenciar meus backups no Google Drive,
> Para que eu possa ver o histórico e limpar backups antigos se necessário.

Esta é uma hipótese histórica ainda não implementada. Produto, provedor externo, disponibilidade e monetização precisam ser revalidados antes de sua aprovação.

## Requisitos funcionais

### Cenário 1: Ver lista de backups

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que estou logado com Google
E tenho múltiplos backups salvos
QUANDO acesso "Backups salvos"
ENTÃO vejo lista com todos os backups
E cada item mostra:
  - Data e hora do backup
  - Quantidade de pets
  - Tamanho do arquivo
  - Versão do app
```

### Cenário 2: Ver detalhes do backup

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que estou na lista de backups
QUANDO toco em um backup
ENTÃO vejo detalhes completos:
  - Data e hora
  - Conteúdo (X pets, Y pesagens, Z vacinas)
  - Tamanho
  - Versão do app que criou
E vejo opções: Restaurar, Deletar
```

### Cenário 3: Deletar backup específico

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que estou nos detalhes de um backup
QUANDO toco em "Deletar"
E confirmo a exclusão
ENTÃO o backup é removido do Google Drive
E não aparece mais na lista
```

### Cenário 4: Deletar múltiplos backups

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que estou na lista de backups
QUANDO ativo modo de seleção (long press)
E seleciono múltiplos backups
E toco em "Deletar selecionados"
E confirmo
ENTÃO todos os backups selecionados são removidos
```

### Cenário 5: Limite de backups manuais

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que tenho 10 backups manuais salvos (limite)
QUANDO faço um novo backup manual
ENTÃO o backup manual mais antigo é removido automaticamente
E o novo backup é adicionado
E vejo notificação "Backup antigo removido para liberar espaço"
```

### Cenário 7: Backups após exclusão de conta

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que tenho backups salvos no Google Drive
QUANDO excluo minha conta do app
ENTÃO os backups são mantidos por 90 dias (período de grace)
E vejo aviso "Sua conta será permanentemente excluída em X dias."
E após 90 dias sem reativação, os backups são purgados automaticamente
```

### Cenário 8: Backups após exclusão de conta

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que tenho backups salvos
QUANDO deleto minha conta
ENTÃO os backups são agendados para purge em 30 dias
E após 30 dias, todos os arquivos no bucket do usuário são removidos permanentemente
```

### Cenário 6: Espaço total usado

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que estou na tela de backup
QUANDO vejo a seção "Backups salvos"
ENTÃO vejo o total de backups
E o espaço total usado (ex: "3 backups • 45.2 KB")
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

### Tela: Detalhes do Backup

```
┌────────────────────────────────┐
│ ← Detalhes do Backup           │
├────────────────────────────────┤
│                                │
│ 📦 BACKUP                      │
│                                │
│ 18/03/2026 às 10:30            │
│ Versão do app: 1.0.0           │
│                                │
├────────────────────────────────┤
│                                │
│ 📊 CONTEÚDO                    │
│ ┌────────────────────────────┐ │
│ │ 🐱 Pets           2       │ │
│ │ ⚖️ Pesagens       15       │ │
│ │ 💉 Vacinas         8       │ │
│ │ 🪱 Vermífugos      6       │ │
│ │ 🔔 Lembretes       3       │ │
│ └────────────────────────────┘ │
│                                │
│ 📁 Tamanho: 15.4 KB            │
│                                │
├────────────────────────────────┤
│                                │
│ ┌────────────────────────────┐ │
│ │       RESTAURAR            │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │        DELETAR             │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Lista com Seleção Múltipla

```
┌────────────────────────────────┐
│ ← Backups Salvos    [🗑️] [✓]   │
├────────────────────────────────┤
│ 2 selecionados                 │
├────────────────────────────────┤
│ ┌────────────────────────────┐ │
│ │ ☑️ 18/03/2026 10:30        │ │
│ │ 2 pets • 15.4 KB          │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ ☐ 15/03/2026 14:20         │ │
│ │ 2 pets • 14.8 KB          │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ ☑️ 10/03/2026 09:15        │ │
│ │ 1 pet • 8.2 KB            │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Dialog: Confirmar Exclusão

```
┌────────────────────────────────┐
│     Deletar Backup?            │
├────────────────────────────────┤
│                                │
│ ⚠️ Esta ação não pode ser      │
│ desfeita.                      │
│                                │
│ O backup será removido         │
│ permanentemente do Firebase    │
│ Storage.                       │
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
