# PRD: Gestão de saúde dos pets no Petit

**Status:** Ativo

**Origem:** `getmiw/specs-miw@09b4497`, adaptado para o Petit

**Última reconciliação:** 2026-07-17

## Problema

Pessoas responsáveis por pets precisam lembrar cuidados recorrentes e manter o
histórico de saúde de cada animal em um lugar fácil de consultar e compartilhar
com profissionais veterinários.

## Objetivos

1. Evitar que cuidados básicos sejam esquecidos.
2. Centralizar o histórico de saúde de um ou mais pets.
3. Manter os dados acessíveis offline e exportáveis quando necessário.

## Documento de requisitos original

## Especificação Técnica:

* **offline-first de verdade**
* **notificações e lembretes locais**
* **sincronização posterior**
* **sincronização via Firebase/Google Cloud Platform**
* **Android only**, pelo menos por enquanto
* domínio simples, mas com bastante estado local e regras de datas

## Stack Tecnológico:

- Android nativo em Kotlin
- Jetpack Compose
- Banco local: Room
- Configurações pequenas / flags / preferências: DataStore
- Jobs de sincronização e lembretes: WorkManager
- Compartilhamento local: Nearby Connections API (pareamento + transferência P2P)
- Sync na rede local: NSD (Network Service Discovery / mDNS) + TCP Sockets
- Transferência device-to-device: Nearby Connections API (P2P_POINT_TO_POINT)

> **Firebase e serviços cloud estão em holding** até que haja demanda de usuários.
> Quando retomados: Firebase Auth, Google Drive API, Firestore, Analytics, Crashlytics, FCM, Remote Config.

## Fases

Fase 1: sem backend obrigatório, tudo local, export/import manual, sem login, sem sincronização, apenas para ter um app funcional o mais rápido possível ✅ CONCLUÍDA

Fase 2: compartilhamento local entre dispositivos da casa (grupo familiar) —
pareamento via Nearby Connections, transferência one-shot, grupo familiar
local e sync contínuo na rede local via NSD. Está **parcialmente implementada**;
consulte as [specs da família local-sharing](../../specs/README.md#local-sharing).

> **Fases abaixo estão em holding até que haja demanda de usuários:**
>
> Fase N: Firebase Auth (login Google opcional)
> Fase N+1: Backup Google Drive (manual + automático)
> Fase N+2: Sync cloud (Firestore, premium)

---

## Arquitetura alvo

**Android nativo + Room + WorkManager + Firebase/Google Cloud Platform**

### Posição de cada peça

* **Room** = fonte de verdade local
* **WorkManager** = trabalhos em background e notificações de tarefas
* **Nearby Connections API** = pareamento de dispositivos + transferência P2P local
* **NSD (mDNS) + TCP** = discovery e sync contínuo na rede local (Wi-Fi de casa)
* **Export/Import JSON** = fallback universal gratuito

> **Componentes em holding (Firebase/Cloud):**
> Firebase Auth, Google Drive API, Firestore, Analytics, Crashlytics, FCM, Remote Config

---

## Modelagem de Produto:

Modelo freemium

### Gratuito

- cadastro dos pets
- pesagem
- gráfico de peso
- vacinação
- desparasitação
- lembretes locais
- exportação JSON
- importação JSON
- compartilhamento local entre dispositivos da casa (grupo familiar)
- sync contínuo na rede local (Wi-Fi de casa)
- transferência de dados entre dispositivos (nearby, one-shot)

### Premium (futuro — quando houver demanda)

- sincronização em tempo real na nuvem (Firebase Firestore)
- sincronização automática multi-device remoto
- backup Google Drive
- compartilhamento de dados com veterinário (opcional mais tarde)
- exportação PDF (opcional mais tarde)

---

# Arquitetura funcional

## Princípio central

O app deve ser **local-first**, não “cloud-first com cache”.

Ou seja:

1. usuário salva tudo no banco local
2. UI sempre lê do banco local
3. sync remoto acontece depois
4. ausência de internet não bloqueia nada
5. conflitos são resolvidos em background

Esse padrão segue a arquitetura offline-first recomendada pelo Android.

---

## Domínios principais

* **Pet**
* **WeightEntry**
* **VaccinationEntry**
* **DewormingEntry**
* **Task** (inclui lembretes)
* **ExportBundle**
* **SyncStatus**

---

## Estrutura de dados sugerida

### Pet

* `id`
* `name`
* `petType` (`CAT`, `DOG`, `RABBIT`, `BIRD`, `HAMSTER`, `OTHER`)
* `birthDate`
* `sex` opcional
* `breed` opcional
* `microchip` opcional
* `passport` opcional
* `createdAt`
* `updatedAt`
* `deletedAt` opcional
* `syncStatus`

### WeightEntry

* `id`
* `petId`
* `date`
* `weightGrams`
* `note` opcional
* `createdAt`
* `updatedAt`
* `deletedAt`
* `syncStatus`

### VaccinationEntry

* `id`
* `petId`
* `vaccineType`
* `customVaccineTypeName` opcional
* `applicationDate`
* `nextDueDate`
* `status` calculado (`OK`, `SCHEDULED`, `OVERDUE`)
* `note`
* `createdAt`
* `updatedAt`
* `deletedAt`
* `syncStatus`

### DewormingEntry

* `id`
* `petId`
* `type` (`INTERNAL`, `EXTERNAL`, `BOTH`)
* `medication` opcional
* `applicationDate`
* `nextDueDate`
* `note`
* `createdAt`
* `updatedAt`
* `deletedAt`
* `syncStatus`

### Task

* `id`
* `kind` (`WEIGHT`, `VACCINATION`, `DEWORMING`, `MEDICATION`, `CUSTOM`)
* `petId` opcional
* `referenceEntityId`
* `title`
* `description` opcional
* `scheduledFor`
* `status` (`PENDING`, `COMPLETED`)

---

# Estratégia de sincronização

## Para o MVP

* toda entidade tem:

  * `id` UUID
  * `updatedAt`
  * `deletedAt`
  * `syncStatus`

### Fluxo

1. salva local
2. marca como `PENDING_SYNC`
3. WorkManager tenta sync quando houver rede
4. se der certo, marca como `SYNCED`
5. se falhar, mantém pendente

## Resolução de conflitos

* **last-write-wins** por `updatedAt`

Para seu domínio pessoal/familiar isso é suficiente no MVP.

### Onde conflitos podem acontecer

* mesmo usuário usando dois celulares
* import manual em cima de dados mais novos
* restore de backup antigo

### Como reduzir dor

* mostrar “última sincronização”
* permitir “forçar upload local” ou “forçar download remoto”
* logs simples de sync

---

# Estratégia de export/import

## Formato

Um único arquivo JSON é o formato oficial atual. O objeto raiz contém
`metadata`, `pets`, `weightEntries`, `vaccinationEntries`,
`dewormingEntries` e `tasks`.

## Por que não CSV como formato principal

* CSV quebra fácil em relacionamentos
* JSON preserva estrutura
* futuro backup/sync fica mais alinhado

## Regras

* export gratuito sempre disponível
* import com validação de schema + versão
* manter compatibilidade de versões por migração

---

# Lembretes e notificações

## Pesagem

Não vamos ter um componente ou tela de calendário visível no app, então os lembretes precisam ser mais proativos.

### Abordagem

Na home page o usuário vê todos os dados de saúde dos pets, e ali tem algo como:

* lista “**Para pesar em breve**”
* lista “**Atrasados**”
* lembrete local por periodicidade configurável:

  * a cada 15 dias
  * mensal
  * bimestral

Porém, vamos agregar nessa tela todos os dados de saúde, não só peso, para ser o dashboard operacional do app.

Os gráficos de peso ficam na tela de pesagem normal, mas os lembretes ficam nesse dashboard geral.

### Regra sugerida

Cada pet tem (exemplo para pesagem, mas mesma lógica para vacina e desparasitação):

* `weightReminderEnabled`
* `weightReminderFrequencyDays`

O app calcula a partir da última pesagem.

---

## Vacinação

### Tela

* “Próximas vacinas”
* “Atrasadas”
* “Concluídas recentemente”

### Regra

Status derivado:

* `OK` → falta bastante tempo
* `SCHEDULED` → janela próxima
* `OVERDUE` → passou da data

Mesma lógica para desparasitação.

---

# Estrutura de telas

## 1. Home Page

Home como dashboard operacional, onde o usuário vê tudo o que precisa fazer e o status geral dos pets.

Se o usuário ainda não tiver cadastrado nenhum pet, a home é um convite para cadastrar o primeiro.

Se o usuário tiver pets cadastrados, a home é um dashboard geral, com lembretes e status de saúde.

Se o usuário tiver cadastrado um pet, mas não tiver registrado peso ou vacina, a home é um convite para registrar o primeiro peso/vacina.

### Conteúdo

* resumo dos pets
* próximos lembretes
* próximas vacinas
* próximas desparasitações
* último peso de cada pet
* CTA para cadastrar algo rapidamente

### Se usuário não pareado

* banner discreto na Home:

  * "Compartilhe dados com sua família"
  * "Conecte outro dispositivo"
  * Toca → abre tab Perfil > seção Grupo Familiar

---

## 2. Profile Page (ex-Settings)

A tab \"Perfil\" no bottom nav (ícone Person) é o hub pessoal do usuário.

### Seções do Profile (em ordem)

1. **Grupo Familiar** — status do grupo ou card de onboarding se não pareado
2. **Configurações** — tema (sistema/claro/escuro), idioma (sistema/pt-BR/en/es)
3. **Dados** — exportar, importar, apagar todos os dados
4. **Sobre** — versão do app

### Se usuário não pareado (seção Grupo Familiar)

* card de onboarding:
  * \"Compartilhe os dados dos seus pets com sua família\"
  * \"Funciona sem internet!\"
* botão \"Parear dispositivo\"
* botão \"Entrar em grupo familiar\"

### Se usuário pareado (seção Grupo Familiar)

* nome do dispositivo parceiro e status de sync
* link \"Gerenciar Grupo\" → tela de gerenciamento

### Regra

* explicar claramente:
  * compartilhamento funciona apenas na rede local
  * dados permanecem nos dispositivos, sem nuvem

Isso é importante para a confiança do usuário.

---

## 3. Cadastro de Pets

* nome
* nascimento
* microchip opcional
* passaporte opcional
* foto opcional depois
* frequência desejada de pesagem
* observações

---

## 4. Pesagem

### Lista

* histórico por pet
* peso atual
* delta em relação à pesagem anterior
* data da última pesagem

### Cadastro

* selecionar pet
* data
* peso
* observação opcional

### Gráfico

* linha temporal por pet
* eixo Y em gramas ou kg
* opção de ver 3 meses / 6 meses / 1 ano

---

## 5. Desparasitação

### Cadastro

* pet
* tipo interna/externa
* medicação
* data da aplicação
* próxima aplicação

### Lista

* próximas
* atrasadas
* histórico

---

## 6. Vacinação

### Cadastro

* pet
* vacina
* data da aplicação
* próxima aplicação

### Lista

* próximas
* atrasadas
* histórico

---

## 7. Exportação / Importação

* exportar arquivo local
* importar arquivo local
* restaurar do backup cloud
* backup manual agora
* última data de export/sync

---

# Fases de entrega

## Fase 1

Perfeita como MVP.

### Entrega técnica

* Pet
* WeightEntry
* Export/Import
* Home simples
* lembrete local de pesagem
* gráfico de peso
* Room + WorkManager base
* arquitetura pronta para sync futuro

### Meta de produto

Validar:

* fluxo offline
* modelo de dados
* UX de cadastro e histórico
* qualidade dos lembretes

---

## Fase 2

### Desparasitação

Aqui já reaproveita bastante da estrutura de dados e telas da vacinação, então é um bom próximo passo.:

* padrão de cadastro
* lista de próximas datas
* reminder local
* export/import

---

## Fase 3

### Vacinação

Mesmo padrão da fase 2, com um pouco mais de semântica por tipo de vacina.

---

# Decisões técnicas importantes

## 1. Banco local como source of truth

Essa é a decisão mais importante do projeto.
Nada de UI lendo da nuvem diretamente.

## 2. Datas sempre em UTC internamente

* guardar ISO UTC / epoch
* formatar localmente para o usuário

## 3. Peso em gramas, não float em kg

Para evitar problemas de precisão:

* guardar `3600`
* exibir `3,60 kg`

## 4. Soft delete

Para sincronização ficar consistente:

* `deletedAt`
* não apagar imediatamente do banco

## 5. Versionamento de schema de export

O arquivo exportado deve ter:

* `appVersion`
* `schemaVersion`
* `exportedAt`

---

# Segurança e privacidade

Como é um app de dados de pets, o risco é baixo, mas eu ainda faria:

* banco local criptografado depois, se quiser evoluir
* token de auth guardado em storage seguro (Firebase Auth token management automático)
* nada sensível em prefs simples
* Firebase Storage com Firestore Security Rules para isolar dados por usuário
* Firebase Auth com Firestore Security Rules

---

# O que **não** será feito no começo

* multiusuário
* colaboração em tempo real
* backend como fonte primária
* Drive como banco principal
* pricing complicado
* iOS simultaneamente
* calendário visual
* integração com veterinário
* monitoramento de saúde baseado em AI

---

# Documentação técnica inicial

Abaixo está um esqueleto de documentação que eu usaria como base do projeto.

## 1. Visão do produto

**Nome provisório:** Petit - The Pet Health Tracker
**Objetivo:** permitir que usuários acompanhem peso, vacinação e desparasitação de pets offline, com backup/sync opcional

## 2. Requisitos funcionais

* cadastrar pets
* registrar pesagens
* visualizar evolução de peso
* registrar vacinações
* listar próximas vacinações
* registrar desparasitações
* listar próximas desparasitações
* exportar dados
* importar dados
* login Google opcional
* sincronização cloud opcional (Firebase Firestore)
* backup no Firebase Storage opcional
* notificações locais para pesagem e eventos próximos

## 3. Requisitos não funcionais

* funcionar offline
* sincronizar quando houver internet
* startup rápido
* UI simples
* sem dependência de calendário visual
* persistência local confiável
* migração de schema suportada
* observabilidade básica de erros

## 4. Arquitetura

Camadas:

* UI
* ViewModel
* Use Cases
* Repository
* Local Data Source
* Remote Data Source
* Sync Engine

## 5. Modelo de dados

* Pet
* WeightEntry
* VaccinationEntry
* DewormingEntry
* Task
* SyncMetadata

## 6. Estratégia offline-first

* Room como fonte de verdade
* WorkManager para sync
* filas de mudança local
* retry exponencial
* last-write-wins no MVP

## 7. Estratégia de monetização

* free: tudo local
* premium: login + nuvem (Firebase) + backups automáticos

## 8. Roadmap

> **Registro legado:** a lista abaixo é uma decomposição anterior à organização
> por fases apresentada no início deste documento. Ela foi preservada como
> contexto histórico e não representa o roadmap vigente.

* Fase 1: cadastro do pet, peso, gráfico, offline-first
* Fase 2: desparasitação
* Fase 3: vacinação
* Fase 4: export/import
* Fase 5: login/sync cloud (Firebase)

---

# Execução

1. **definir o modelo de dados e regras de status**
2. **desenhar a arquitetura offline-first**
3. **fazer a documentação técnica do MVP**
4. **desenhar as telas da Fase 1**
5. **quebrar em backlog técnico**
