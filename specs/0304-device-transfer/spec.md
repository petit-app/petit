---
spec: "0304"
title: "Transferência entre Dispositivos"
family: backup-recovery
phase: 4
status: On Hold
owner: ""
depends_on: ["0101"]
origin: "getmiw/specs-miw@09b4497"
---

# Spec: Transferência entre Dispositivos

## Contexto e motivação

> Como usuário do app,
> Eu quero transferir meus dados para outro celular próximo,
> Para que eu possa compartilhar dados com outro device sem usar a nuvem.

Esta é uma hipótese histórica ainda não implementada. Produto, provedor externo, disponibilidade e monetização precisam ser revalidados antes de sua aprovação.

## Requisitos funcionais

### Cenário 1: Enviar dados para outro device

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que tenho dados locais (pets, pesos, etc.)
E outro device com Petit está próximo
QUANDO acesso Configurações > "Compartilhar dados"
E toco em "Enviar dados"
ENTÃO vejo código de 4 dígitos para compartilhar
E aguardo conexão do receptor
QUANDO receptor insere o código
ENTÃO dados são enviados via Nearby Connections
E vejo "Dados enviados com sucesso"
```

### Cenário 2: Receber dados de outro device

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que estou no app
QUANDO acesso Configurações > "Receber dados"
E toco em "Receber de outro celular"
ENTÃO vejo campo para inserir código
QUANDO insiro código de 4 dígitos do transmissor
ENTÃO conexão é estabelecida
E vejo progresso de transferência
QUANDO transferência completa
ENTÃO vejo opção "Substituir" ou "Mesclar" dados
```

### Cenário 3: Transferência sem internet

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que ambos devices estão sem internet
MAS estão na mesma rede Wi-Fi OU com Bluetooth ativo
QUANDO inicio transferência
ENTÃO funciona normalmente
(Nearby Connections usa Wi-Fi Direct ou Bluetooth)
```

### Cenário 4: Mesclar dados recebidos

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que recebi dados de outro device
E tenho dados locais
QUANDO escolho "Mesclar"
ENTÃO dados são combinados
E duplicatas são resolvidas por ID (UUIDs únicos)
E vejo resumo: "2 pets adicionados, 10 pesagens mescladas"
```

### Cenário 5: Substituir dados locais

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que recebi dados de outro device
QUANDO escolho "Substituir"
ENTÃO vejo confirmação "Seus dados locais serão apagados. Continuar?"
QUANDO confirmo
ENTÃO todos dados locais são deletados
E dados recebidos são importados
E vejo "Dados restaurados com sucesso"
```

### Cenário 6: Cancelar transferência

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que transferência está em andamento
QUANDO toco em "Cancelar"
ENTÃO transferência é interrompida
E dados parciais são descartados
E ambos devices voltam ao estado inicial
```

### Cenário 7: Erro de conexão

- [ ] Este cenário é atendido e verificado no limite indicado pela estratégia de testes.

```gherkin
DADO que devices estão muito distantes
OU Bluetooth/Wi-Fi estão desativados
QUANDO tento iniciar transferência
ENTÃO vejo mensagem "Não foi possível conectar. Aproxime os devices e ative Wi-Fi ou Bluetooth."
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

### Tela: Compartilhar Dados (Transmissor)

```
┌────────────────────────────────┐
│ ← Compartilhar Dados           │
├────────────────────────────────┤
│                                │
│        📱 ➡️ 📱                │
│                                │
│  Compartilhe seus dados com    │
│  outro celular próximo         │
│                                │
│ ┌────────────────────────────┐ │
│ │    ENVIAR DADOS            │ │
│ └────────────────────────────┘ │
│                                │
│ ℹ️ Funciona sem internet,      │
│ usando Wi-Fi Direct ou         │
│ Bluetooth.                     │
│                                │
└────────────────────────────────┘
```

### Tela: Aguardando Conexão (Transmissor)

```
┌────────────────────────────────┐
│ ← Aguardando conexão...        │
├────────────────────────────────┤
│                                │
│         🔒                     │
│                                │
│    Código de segurança:        │
│                                │
│        ┌──────────┐            │
│        │   4729   │            │
│        └──────────┘            │
│                                │
│  Peça para o outro celular     │
│  inserir este código.          │
│                                │
│ ┌────────────────────────────┐ │
│ │       CANCELAR             │ │
│ └────────────────────────────┘ │
│                                │
└────────────────────────────────┘
```

### Tela: Receber Dados (Receptor)

```
┌────────────────────────────────┐
│ ← Receber Dados                │
├────────────────────────────────┤
│                                │
│        📱 ⬅️ 📱                │
│                                │
│  Insira o código mostrado no   │
│  outro celular:                │
│                                │
│  ┌────┬────┬────┬────┐         │
│  │  4 │  7 │  2 │  9 │         │
│  └────┴────┴────┴────┘         │
│                                │
│ ┌────────────────────────────┐ │
│ │      CONECTAR              │ │
│ └────────────────────────────┘ │
│                                │
│ ℹ️ Certifique-se de que Wi-Fi  │
│ ou Bluetooth estão ativos.     │
│                                │
└────────────────────────────────┘
```

### Tela: Transferindo

```
┌────────────────────────────────┐
│ Transferindo...                │
├────────────────────────────────┤
│                                │
│         ████████░░             │
│            80%                 │
│                                │
│  Enviando dados...             │
│  2 pets • 25 registros        │
│                                │
│  Não desligue o app            │
│                                │
└────────────────────────────────┘
```

### Dialog: Escolher Ação (Receptor)

```
┌────────────────────────────────┐
│                                │
│         ✅                     │
│                                │
│   Dados recebidos!             │
│                                │
│   2 pets                      │
│   15 pesagens                  │
│   8 vacinas                    │
│                                │
│ ┌────────────────────────────┐ │
│ │    MESCLAR COM LOCAIS      │ │
│ └────────────────────────────┘ │
│                                │
│ ┌────────────────────────────┐ │
│ │    SUBSTITUIR LOCAIS       │ │
│ └────────────────────────────┘ │
│                                │
│       Cancelar                 │
│                                │
└────────────────────────────────┘
```

---

### Segurança

- **Código de 4 dígitos**: Previne conexões não autorizadas
- **Proximity-based**: Funciona apenas com devices próximos (< 10 metros)
- **One-shot transfer**: Conexão é encerrada após transferência
- **No cloud storage**: Dados trafegam diretamente entre devices
- **Encryption**: Nearby Connections usa criptografia automática

---


### Referências

- [Google Nearby Connections API](https://developers.google.com/nearby/connections/overview)
- [Android Strategy.P2P_POINT_TO_POINT](https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/Strategy#P2P_POINT_TO_POINT)

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
