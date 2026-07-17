---
spec: "0204"
title: "Gate Premium"
family: identity-access
phase: 3
status: On Hold
owner: ""
depends_on: ["0201"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Gate Premium

## Contexto e motivação

> Como usuário do app,
> Eu quero entender quais recursos são premium,
> Para que eu possa decidir se vale a pena assinar.

Esta é uma hipótese histórica ainda não implementada. Produto, provedor externo, disponibilidade e monetização precisam ser revalidados antes de sua aprovação.

## Requisitos funcionais

### Cenário 1: Ver indicador premium em feature bloqueada

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que sou usuário gratuito
QUANDO vejo a opção "Sincronização em tempo real" nas configurações
ENTÃO vejo um ícone de ⭐ ou 🔒 indicando que é premium
E ao tocar, vejo informação sobre o plano premium
```

### Cenário 2: Tentar usar feature premium

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que sou usuário gratuito
QUANDO tento ativar "Sincronização em tempo real"
ENTÃO vejo um bottom sheet ou dialog explicando:
  - O que a feature faz
  - Que é exclusiva para premium
  - Botão para ver planos
```

### Cenário 3: Listar benefícios premium

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que estou no app
QUANDO acesso "Ver planos premium"
ENTÃO vejo lista de benefícios:
  - ☁️ Sincronização em tempo real na nuvem
  - 📱 Múltiplos dispositivos sincronizados automaticamente
  - 👨‍👩‍👧 Compartilhar com família
  - 📄 Exportar PDF (futuro)
```

### Cenário 4: Verificar status premium

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que sou usuário premium
QUANDO acesso configurações
ENTÃO vejo "Plano: Premium"
E não vejo indicadores de bloqueio em features premium
E as features premium estão liberadas
```

### Cenário 5: Funcionalidades gratuitas disponíveis sem login

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que não estou logado
QUANDO uso o app
ENTÃO posso cadastrar pets, pesar, vacinar, criar lembretes
E posso exportar/importar JSON
MAS não posso fazer backup no Google Drive (requer login)
E não posso usar sync em tempo real (premium)
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

### Features por Tier

| Feature | Free (sem login) | Free (com login) | Premium |
|---------|------------------|------------------|---------|
| Cadastro de pets | ✅ | ✅ | ✅ |
| Pesagem + gráfico | ✅ | ✅ | ✅ |
| Vacinação/Vermífugo | ✅ | ✅ | ✅ |
| Lembretes locais | ✅ | ✅ | ✅ |
| Export/Import JSON | ✅ | ✅ | ✅ |
| Login Google | ❌ | ✅ | ✅ |
| Backup manual Google Drive | ❌ | ✅ | ✅ |
| Backup automático Google Drive (2h da madrugada) | ❌ | ✅ | ✅ |
| Restaurar backup do Google Drive | ❌ | ✅ | ✅ |
| Transferência device-to-device (Nearby) | ✅ | ✅ | ✅ |
| Sync em tempo real (Firebase Firestore) | 🔒 | 🔒 | ✅ |
| Múltiplos devices sincronizados | 🔒 | 🔒 | ✅ |
| Compartilhar com família | 🔒 | 🔒 | ✅ |

---

### UI/UX

### Configurações com Gates

```
┌────────────────────────────────┐
│ ← Configurações                │
├────────────────────────────────┤
│                                │
│ 📦 DADOS                       │
│ ┌────────────────────────────┐ │
│ │ 📤 Exportar dados          │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ 📥 Importar dados          │ │
│ └────────────────────────────┘ │
│                                │
│ ☁️ BACKUP (GOOGLE DRIVE)       │
│ ┌────────────────────────────┐ │
│ │ 💾 Backup manual            │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ ⏰ Backup automático (2h)    │ │
│ └────────────────────────────┘ │
│                                │
│ 📶 TRANSFERÊNCIA               │
│ ┌────────────────────────────┐ │
│ │ 🔄 Compartilhar dados       │ │
│ └────────────────────────────┘ │
│                                │
│ 🔒 PREMIUM                     │
│ ┌────────────────────────────┐ │
│ │ 🔄 Sync em tempo real      ⭐ │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ Desbloqueie sync            │ │
│ │ automático multi-device!    │ │
│ │ [Ver planos]               │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Bottom Sheet: Feature Bloqueada

```
┌────────────────────────────────┐
│                    ─────       │
│                                │
│         ⭐                     │
│   Recurso Premium              │
│                                │
│   Sincronização em tempo real  │
│   na nuvem                       │
│                                │
│   Seus dados sincronizam         │
│   automaticamente entre todos    │
│   os seus dispositivos.          │
│                                │
│ ┌────────────────────────────┐ │
│ │      VER PLANOS            │ │
│ └────────────────────────────┘ │
│                                │
│        Agora não               │
│                                │
└────────────────────────────────┘
```

### Tela: Planos Premium

```
┌────────────────────────────────┐
│ ← Petit Premium                  │
├────────────────────────────────┤
│                                │
│         ⭐ PREMIUM             │
│                                │
│ Cuide melhor dos seus pets     │
│                                │
├────────────────────────────────┤
│                                │
│ ✅ Sincronização em tempo real │
│ ✅ Múltiplos dispositivos       │
│ ✅ Compartilhar com família    │
│ ✅ Suporte prioritário         │
│                                │
├────────────────────────────────┤
│                                │
│ ┌────────────────────────────┐ │
│ │       MENSAL               │ │
│ │       R$ 9,90/mês          │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │       ANUAL                │ │
│ │       R$ 79,90/ano         │ │
│ │       (economize 33%)      │ │
│ └────────────────────────────┘ │
│                                │
│ Cancele quando quiser.         │
│ Seus dados locais são seus.    │
│                                │
└────────────────────────────────┘
```

---

### Nota sobre Billing

A implementação com Google Play Billing (compra de assinatura) era uma hipótese posterior do roadmap antigo. Caso essa hipótese seja validada, o escopo proposto seria:

1. ✅ Exibir gates visuais
2. ✅ Verificar status premium via Firebase Firestore
3. ✅ Bloquear features premium via código
4. ⏳ Integração com Google Play Billing (implementação futura)

O status premium pode ser definido manualmente no Firebase Console para testes.

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
