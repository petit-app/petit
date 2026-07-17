---
spec: "0302"
title: "Restaurar Backup"
family: backup-recovery
phase: 4
status: On Hold
owner: ""
depends_on: ["0301"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Restaurar Backup

## Contexto e motivação

> Como usuário logado,
> Eu quero restaurar meus dados de um backup no Google Drive,
> Para que eu possa recuperar meus dados em um novo celular ou após reinstalar o app.

Esta é uma hipótese histórica ainda não implementada. Produto, provedor externo, disponibilidade e monetização precisam ser revalidados antes de sua aprovação.

## Requisitos funcionais

### Cenário 1: Restaurar backup com sucesso

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que estou logado com Google
E tenho backups salvos no Google Drive
QUANDO acesso "Backups salvos"
E seleciono um backup para restaurar
E confirmo a restauração
ENTÃO vejo progresso de download
E os dados são restaurados no banco local
E vejo mensagem "Dados restaurados com sucesso"
```

### Cenário 2: Restaurar em dispositivo novo

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que instalei o app em um novo celular
E fiz login com minha conta Google
QUANDO acesso "Restaurar de backup"
ENTÃO vejo lista de backups disponíveis
E posso selecionar qual restaurar
```

### Cenário 3: Restaurar substitui dados locais

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que tenho dados locais
E restauro um backup
QUANDO confirmo "Substituir dados locais"
ENTÃO TODOS os dados locais são apagados
E os dados do backup são importados
E vejo os dados do backup na home
```

### Cenário 4: Restaurar com merge

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que tenho dados locais
E restauro um backup
QUANDO escolho "Mesclar com dados locais"
ENTÃO dados são mesclados (last-write-wins)
E dados únicos de ambas fontes são mantidos
```

### Cenário 5: Restaurar sem backups

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que não tenho backups no Google Drive
QUANDO acesso "Backups salvos"
ENTÃO vejo mensagem "Nenhum backup encontrado"
E vejo sugestão para fazer primeiro backup
```

### Cenário 6: Erro de download

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que seleciono um backup para restaurar
QUANDO a conexão falha durante download
ENTÃO vejo mensagem de erro
E os dados locais não são alterados
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

### Tela: Lista de Backups

```
┌────────────────────────────────┐
│ ← Backups Salvos               │
├────────────────────────────────┤
│                                │
│ Selecione um backup para       │
│ restaurar:                     │
│                                │
├────────────────────────────────┤
│ ┌────────────────────────────┐ │
│ │ 📦 18/03/2026 10:30        │ │
│ │ 2 pets • 15.4 KB          │ │
│ │ v1.0.0                     │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ 📦 15/03/2026 14:20        │ │
│ │ 2 pets • 14.8 KB          │ │
│ │ v1.0.0                     │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ 📦 10/03/2026 09:15        │ │
│ │ 1 pet • 8.2 KB            │ │
│ │ v1.0.0                     │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Dialog: Confirmar Restauração

```
┌────────────────────────────────┐
│      Restaurar Backup          │
├────────────────────────────────┤
│                                │
│ Backup de 18/03/2026 10:30     │
│ 2 pets • 15.4 KB              │
│                                │
│ ⚠️ Você tem dados locais.      │
│ O que deseja fazer?            │
│                                │
│ ○ Substituir dados locais      │
│   (apaga tudo e restaura)      │
│                                │
│ ● Mesclar com dados locais     │
│   (mantém dados mais recentes) │
│                                │
│ ┌──────────┐  ┌──────────────┐ │
│ │ CANCELAR │  │  RESTAURAR   │ │
│ └──────────┘  └──────────────┘ │
└────────────────────────────────┘
```

### Estado: Restaurando

```
┌────────────────────────────────┐
│                                │
│                                │
│         ┌─────────┐            │
│         │  ████░░ │            │
│         └─────────┘            │
│                                │
│      Restaurando backup...     │
│      Baixando dados            │
│                                │
│      Não feche o app           │
│                                │
│                                │
└────────────────────────────────┘
```

### Estado: Sem Backups

```
┌────────────────────────────────┐
│ ← Backups Salvos               │
├────────────────────────────────┤
│                                │
│                                │
│         📭                     │
│                                │
│   Nenhum backup encontrado     │
│                                │
│   Faça seu primeiro backup     │
│   para proteger seus dados.    │
│                                │
│ ┌────────────────────────────┐ │
│ │    FAZER BACKUP AGORA      │ │
│ └────────────────────────────┘ │
│                                │
│                                │
└────────────────────────────────┘
```

---

### Fluxo de Onboarding (Device Novo)

```kotlin
class OnboardingViewModel(...) {

    fun checkForBackups() {
        viewModelScope.launch {
            // Verificar se usuário tem backups
            backupStorageRepository.listBackups()
                .onSuccess { backups ->
                    if (backups.isNotEmpty()) {
                        // Mostrar opção de restaurar
                        _showRestoreOption.value = true
                    }
                }
        }
    }
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
