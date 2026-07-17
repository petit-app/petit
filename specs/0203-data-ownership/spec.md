---
spec: "0203"
title: "Vinculação de Dados"
family: identity-access
phase: 3
status: On Hold
owner: ""
depends_on: ["0201"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Vinculação de Dados

## Contexto e motivação

> Como usuário que acabou de fazer login,
> Eu quero que meus dados locais sejam vinculados à minha conta,
> Para que futuramente eu possa sincronizá-los na nuvem.

Esta é uma hipótese histórica ainda não implementada. Produto, provedor externo, disponibilidade e monetização precisam ser revalidados antes de sua aprovação.

## Requisitos funcionais

### Cenário 1: Primeiro login vincula dados existentes

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que tenho 2 pets cadastrados localmente
E nunca fiz login antes
QUANDO faço login com "pessoa-a@example.com"
ENTÃO meus 2 pets são marcados com ownerId = meu userId
E posso continuar usando normalmente
```

### Cenário 2: Dados criados após login já vêm vinculados

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que estou logado como "pessoa-a@example.com"
QUANDO cadastro um novo pet "Luna"
ENTÃO Luna é criada com ownerId = meu userId
```

### Cenário 3: Dados em modo anônimo não têm owner

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que estou usando sem login
QUANDO cadastro um pet "Simba"
ENTÃO Simba é criado com ownerId = null
```

### Cenário 4: Logout não remove vinculação

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que tenho pets vinculados ao meu userId
QUANDO faço logout
ENTÃO os pets continuam com o ownerId preenchido
E continuam visíveis no app
```

### Cenário 5: Login com conta diferente

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que tenho pets do "user-a" no dispositivo
E faço login como "pessoa-b@example.com" (user-b)
ENTÃO vejo os pets da Pessoa A (dados locais)
MAS eles continuam com ownerId = user-a
E novos dados criados terão ownerId = user-b
(gestão de múltiplos owners é tratada em sync futuro)
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

### Visualização de Dados

### Fase 2: Mostrar todos os dados locais

```kotlin
// Por enquanto, mostrar todos os dados locais independente do owner
fun getAllPets(): Flow<List<PetEntity>> {
    return petDao.getAllPets()  // Sem filtro por owner
}
```

### Fase futura (5): Filtrar por owner para sync

```kotlin
// Quando implementar sync, filtrar por owner
fun getPetsForSync(userId: String): Flow<List<PetEntity>> {
    return petDao.getPetsForUser(userId)
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
