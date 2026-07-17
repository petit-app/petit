# Specs Index

Índice de todas as especificações do Petit. Atualize este arquivo ao criar,
aprovar, implementar, colocar em espera ou concluir uma spec.

Cada capacidade vive em uma pasta global `specs/NNNN-<slug>/` com `spec.md`,
`plan.md` e `tasks.md`. Família, fase e status ficam no frontmatter de
`spec.md` e são refletidos nas tabelas abaixo; não existem pastas por fase ou
status.

## Numeração

| Bloco | Família | Reservado para |
| --- | --- | --- |
| 0001–0099 | pet-care | Cadastro, saúde, histórico e rotina dos pets |
| 0100–0199 | local-sharing | Compartilhamento familiar sem servidor remoto |
| 0200–0299 | identity-access | Identidade, conta e autorização |
| 0300–0399 | backup-recovery | Backup, restauração e transferência |
| 0400–0499 | cloud-sync | Sincronização remota e colaboração em nuvem |

## Status

| Status | Significado |
| --- | --- |
| Draft | Comportamento proposto; ainda não aprovado |
| Approved | Spec aprovada para implementação |
| In Progress | Implementação parcial ou em andamento |
| Implemented | Comportamento principal disponível no código |
| Completed | Todos os critérios e tarefas concluídos e verificados |
| On Hold | Hipótese preservada, sem compromisso atual de implementação |

## Specs

### pet-care

PRD: [Gestão de saúde dos pets no Petit](../docs/prds/2026-07-17-petit-pet-health-management.md)

| Spec | Título | Fase | Status | Depende de / Origem |
| --- | --- | --- | --- | --- |
| [0001](0001-pet-management/spec.md) | Gerenciamento de pets | 1 | Implemented | PRD Petit |
| [0002](0002-weight-tracking/spec.md) | Acompanhamento de peso | 1 | Implemented | 0001 |
| [0003](0003-vaccination/spec.md) | Registro de vacinação | 1 | Implemented | 0001 |
| [0004](0004-deworming/spec.md) | Registro de desparasitação | 1 | Implemented | 0001 |
| [0005](0005-reminders/spec.md) | Tarefas e lembretes locais | 1 | Implemented | 0001 |
| [0006](0006-export-import/spec.md) | Exportação e importação JSON | 1 | Implemented | 0001–0005 |
| [0007](0007-home-dashboard/spec.md) | Dashboard inicial | 1 | Implemented | 0001–0005 |

### local-sharing

| Spec | Título | Fase | Status | Depende de / Origem |
| --- | --- | --- | --- | --- |
| [0101](0101-device-pairing/spec.md) | Pareamento de dispositivos | 2 | In Progress | PRD Petit |
| [0102](0102-one-shot-transfer/spec.md) | Transferência pontual de dados | 2 | In Progress | 0101 |
| [0103](0103-family-group/spec.md) | Grupo familiar local | 2 | In Progress | 0101 |
| [0104](0104-local-network-sync/spec.md) | Sincronização na rede local | 2 | Draft | 0101, 0103 |
| [0105](0105-local-conflict-resolution/spec.md) | Resolução local de conflitos | 2 | In Progress | 0102 |

### identity-access

| Spec | Título | Fase | Status | Depende de / Origem |
| --- | --- | --- | --- | --- |
| [0201](0201-google-login/spec.md) | Login com Google | 3 | On Hold | Roadmap histórico |
| [0202](0202-account-management/spec.md) | Gerenciamento de conta | 3 | On Hold | 0201 |
| [0203](0203-data-ownership/spec.md) | Vinculação de Dados | 3 | On Hold | 0201 |
| [0204](0204-premium-gate/spec.md) | Gate Premium | 3 | On Hold | 0201 |

### backup-recovery

| Spec | Título | Fase | Status | Depende de / Origem |
| --- | --- | --- | --- | --- |
| [0301](0301-manual-backup/spec.md) | Backup Manual | 4 | On Hold | 0201 |
| [0302](0302-restore-backup/spec.md) | Restaurar Backup | 4 | On Hold | 0301 |
| [0303](0303-manage-backups/spec.md) | Gerenciar Backups | 4 | On Hold | 0301 |
| [0304](0304-device-transfer/spec.md) | Transferência entre dispositivos | 4 | On Hold | 0101 |
| [0305](0305-automatic-backup/spec.md) | Backup automático | 4 | On Hold | 0301 |
| [0306](0306-backup-settings/spec.md) | Configurações de backup | 4 | On Hold | 0305 |
| [0307](0307-backup-triggers/spec.md) | Gatilhos de backup | 4 | On Hold | 0305, 0306 |

### cloud-sync

| Spec | Título | Fase | Status | Depende de / Origem |
| --- | --- | --- | --- | --- |
| [0401](0401-realtime-cloud-sync/spec.md) | Sincronização em Tempo Real | 5 | On Hold | 0201 |
| [0402](0402-multi-device-sync/spec.md) | Sincronização entre Múltiplos Dispositivos | 5 | On Hold | 0401 |
| [0403](0403-cloud-conflict-resolution/spec.md) | Resolução de conflitos na nuvem | 5 | On Hold | 0401 |
| [0404](0404-offline-cloud-sync/spec.md) | Sincronização Offline-First | 5 | On Hold | 0401 |
| [0405](0405-cloud-family-sharing/spec.md) | Compartilhamento familiar na nuvem | 5 | On Hold | 0201, 0401 |
