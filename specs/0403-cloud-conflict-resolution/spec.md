---
spec: "0403"
title: "Resolução de Conflitos na Nuvem"
family: cloud-sync
phase: 5
status: On Hold
owner: ""
depends_on: ["0401"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Resolução de Conflitos na Nuvem

## Contexto e motivação

> Como usuário com múltiplos dispositivos,
> Eu quero que conflitos de edição sejam resolvidos automaticamente,
> Para que eu não perca dados e não precise resolver conflitos manualmente.

Esta é uma hipótese histórica ainda não implementada. Produto, provedor externo, disponibilidade e monetização precisam ser revalidados antes de sua aprovação.

## Requisitos funcionais

### Cenário 1: Last-write-wins básico

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que o pet "Luna" tem updatedAt = 1000 no dispositivo A
E o dispositivo B edita Luna (updatedAt = 1500)
QUANDO o dispositivo A recebe a mudança do snapshot listener do Firestore
ENTÃO a versão do dispositivo B (mais recente) é mantida
E o dispositivo A mostra as alterações do B
```

### Cenário 2: Edição offline mais antiga

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que dispositivo A está offline e edita Luna (updatedAt = 1000)
E dispositivo B edita Luna online (updatedAt = 1500)
QUANDO dispositivo A volta online e tenta sync
ENTÃO a versão do dispositivo B vence (updatedAt maior)
E as alterações do dispositivo A são descartadas
E o dispositivo A atualiza para a versão do B
```

### Cenário 3: Edição offline mais recente

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que dispositivo A está offline e edita Luna (updatedAt = 2000)
E a versão no Firestore tem updatedAt = 1500
QUANDO dispositivo A volta online e faz sync
ENTÃO a versão do dispositivo A vence (updatedAt maior)
E o Firestore é atualizado com a versão do A
E outros dispositivos recebem a versão do A
```

### Cenário 4: Delete vs Edit

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que dispositivo A deleta Luna (deletedAt = 1500)
E dispositivo B editou Luna (updatedAt = 1600) antes de receber o delete
QUANDO o sync acontece
ENTÃO se a edição é mais recente que o delete, Luna é restaurada
OU se o delete é mais recente, Luna permanece deletada
```

### Cenário 5: Campos diferentes editados

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que dispositivo A edita o nome de Luna para "Luninha"
E dispositivo B edita o peso de Luna ao mesmo tempo
QUANDO o sync acontece
ENTÃO ambas as mudanças são mantidas (se a estratégia for field-level)
OU a versão mais recente vence completamente (se document-level)
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

### Estratégia de Resolução

### Document-Level (Implementação Atual)

```
Regra: Last-Write-Wins baseado em updatedAt

Local:  { id: "1", name: "Luna",    updatedAt: 1000 }
Remote: { id: "1", name: "Luninha", updatedAt: 1500 }

Resultado: Remote vence (1500 > 1000)
           Local é substituído por Remote
```

### Field-Level (Futura Melhoria)

```
Local:  { id: "1", name: "Luna",    weight: 3.5, updatedAt: 1000, weightUpdatedAt: 1000 }
Remote: { id: "1", name: "Luninha", weight: 3.4, updatedAt: 1500, weightUpdatedAt: 900 }

Resultado: Merge
           name: "Luninha" (remote mais recente)
           weight: 3.5 (local mais recente)
```

---

### Casos Edge

### 1. Timestamps Iguais
```kotlin
// Se timestamps são exatamente iguais (raro), preferir remoto
// Isso evita loops de sync
if (remote.updatedAt == local.updatedAt && local.syncStatus == "SYNCED") {
    return Resolution.KeepLocal  // Já está sincronizado
}
```

### 2. Clock Drift Grande
```kotlin
// Se a diferença de timestamp for absurda (> 1 ano), algo está errado
val MAX_REASONABLE_DIFF = 365L * 24 * 60 * 60 * 1000  // 1 ano em ms

if (abs(remote.updatedAt - local.updatedAt) > MAX_REASONABLE_DIFF) {
    Log.w("Sync", "Suspicious timestamp difference, preferring local")
    return Resolution.KeepLocal
}
```

### 3. Dados Corrompidos
```kotlin
// Validar dados antes de aceitar
if (!remote.isValid()) {
    Log.e("Sync", "Invalid remote data, keeping local")
    return Resolution.KeepLocal
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
