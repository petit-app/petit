---
spec: "0402"
title: "Sincronização entre Múltiplos Dispositivos"
family: cloud-sync
phase: 5
status: On Hold
owner: ""
depends_on: ["0401"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Sincronização entre Múltiplos Dispositivos

## Contexto e motivação

> Como usuário premium com múltiplos dispositivos,
> Eu quero que meus dados estejam disponíveis em todos eles,
> Para que eu possa acessar e editar de qualquer lugar.

Esta é uma hipótese histórica ainda não implementada. Produto, provedor externo, disponibilidade e monetização precisam ser revalidados antes de sua aprovação.

## Requisitos funcionais

### Cenário 1: Segundo dispositivo recebe dados

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que tenho dados no dispositivo A
E instalo o app no dispositivo B
QUANDO faço login no dispositivo B
E ativo a sincronização
ENTÃO todos os meus dados são baixados do Firestore
E vejo os mesmos pets que no dispositivo A
```

### Cenário 2: Edição aparece em tempo real

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que tenho o app aberto no dispositivo A e B
QUANDO edito o nome do pet para "Luninha" no dispositivo A
ENTÃO em poucos segundos, o dispositivo B mostra "Luninha"
Sem precisar atualizar manualmente
```

### Cenário 3: Criar em um, ver em outro

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que adiciono um novo pet "Simba" no dispositivo A
QUANDO o sync completa
ENTÃO o dispositivo B recebe "Simba" automaticamente
E Simba aparece na lista de pets
```

### Cenário 4: Deletar em um, reflete em outro

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que deleto o pet "Simba" no dispositivo A
QUANDO o sync completa
ENTÃO o dispositivo B também não mostra mais "Simba"
```

### Cenário 5: Dispositivo offline vs online

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que dispositivo A está offline
E dispositivo B adiciona pet "Mia"
QUANDO dispositivo A volta online
ENTÃO dispositivo A recebe "Mia" automaticamente
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

### Indicador de Dispositivos

```
┌────────────────────────────────┐
│ ← Sincronização                │
├────────────────────────────────┤
│                                │
│ 📱 DISPOSITIVOS CONECTADOS     │
│ ┌────────────────────────────┐ │
│ │ 📱 Este dispositivo        │ │
│ │    Galaxy S24 • Online     │ │
│ │                            │ │
│ │ 📱 Outro dispositivo       │ │
│ │    Pixel 8 • Há 5 min      │ │
│ └────────────────────────────┘ │
│                                │
│ ℹ️ Seus dados são sincronizados│
│ automaticamente entre todos   │
│ os dispositivos logados.      │
│                                │
└────────────────────────────────┘
```

### Primeiro Sync em Novo Dispositivo

```
┌────────────────────────────────┐
│                                │
│         ☁️ ↓                   │
│                                │
│   Sincronizando seus dados...  │
│                                │
│   ████████░░░░░░  60%          │
│                                │
│   Baixando: 2 pets            │
│             15 pesagens        │
│             8 vacinas          │
│                                │
│   Mantenha a conexão ativa     │
│                                │
└────────────────────────────────┘
```

---

### Resolução de Conflitos Multi-Device

Quando dois dispositivos editam o mesmo dado simultaneamente:

```kotlin
suspend fun handleIncomingChange(remote: PetFirestoreModel) {
    val local = petDao.getPetById(remote.id)

    when {
        // Novo remotamente
        local == null -> {
            petDao.insertPet(remote.toEntity())
        }
        // Remoto mais recente: aceitar
        remote.updatedAt > local.updatedAt -> {
            petDao.updatePet(remote.toEntity())
        }
        // Local mais recente: manter local e re-upload
        local.updatedAt > remote.updatedAt && local.syncStatus == "SYNCED" -> {
            // Local é mais recente mas já foi marcado como synced
            // Isso significa que a mudança local ainda não foi enviada
            // Manter local e enviar para nuvem
            uploadToFirestore(local)
        }
        // Mesmo timestamp: considerar empate, manter local
        else -> {
            // Não fazer nada, local já está correto
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
