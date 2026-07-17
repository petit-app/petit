# Architecture

> Cross-cutting document. Decisions and requirements specific to each
> capability are kept in the numbered folders under [`specs/`](../specs/README.md).

## Principles

### 1. Local-First

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   UI/View   │────▶│  ViewModel  │────▶│ Repository  │
└─────────────┘     └─────────────┘     └─────────────┘
                                               │
                    ┌──────────────────────────┼──────────────────────────┐
                    │                          │                          │
                    ▼                          ▼                          ▼
             ┌─────────────┐           ┌─────────────┐           ┌─────────────┐
             │    Room     │           │  DataStore  │           │ WorkManager │
             │  (truth)    │           │   (prefs)   │           │   (sync)    │
             └─────────────┘           └─────────────┘           └─────────────┘
```

**Rules**:

- The UI always reads from the local database (Room)
- Write operations go directly to Room
- Remote sync runs in the background through WorkManager
- Lack of internet access **NEVER** blocks operations

### 2. Soft Delete

All syncable entities use soft deletion:

- `deletedAt: Long?` - deletion timestamp or null
- Queries filter with `WHERE deletedAt IS NULL` by default
- Sync propagates deletions so they can be resolved on other devices

### 3. Sync Status

```kotlin
enum class SyncStatus {
    LOCAL_ONLY,      // Never synced (free tier)
    PENDING_SYNC,    // Modified and awaiting sync
    SYNCED,          // Synced successfully
    CONFLICT         // Conflict detected (requires resolution)
}
```

---

## Layers

### Presentation Layer

- **Jetpack Compose** for the UI
- **ViewModel** with StateFlow for state management
- **Navigation Compose** for navigation

### Domain Layer

- **Use Cases** encapsulate business rules
- Pure **Domain Models** (without Room annotations)

### Data Layer

- **Repository** abstracts the data source
- **Room DAOs** provide database access
- **DataStore** stores preferences

### Background Layer

- **WorkManager Workers** handle:
  - Triggering task notifications (`TaskNotificationWorker` — one-shot)
  - Automating task creation when health records are saved (`AutoTaskService`)
  - Scheduling and canceling notifications (`TaskScheduler` / `TaskSchedulerImpl`)
  - Local network sync through NSD (Network Service Discovery) — Phase 2
  - Remote sync through Firebase Firestore — Phase N+2 (on hold)

---

## Package Structure

```
com.woliveiras.petit/
├── data/
│   ├── local/
│   │   ├── db/                     — PetitDatabase and migrations
│   │   ├── dao/                    — @Dao interfaces (PetDao, WeightEntryDao, etc.)
│   │   └── entity/                 — Room @Entity classes
│   ├── mapper/                     — Entity ↔ Domain model conversion
│   └── repository/                 — Repository interfaces and implementations
├── domain/
│   ├── model/                      — pure domain models (no Android dependencies)
│   └── usecase/                    — cross-repository actions (ExportImport, DeleteAll)
├── presentation/
│   ├── feature/
│   │   ├── home/                   — HomeScreen, HomeViewModel, HomeUiState
│   │   ├── pets/                   — PetList, PetDetail, PetForm, PetDeleteConfirmation, PetSelection
│   │   ├── weight/                 — WeightEntry, WeightForm
│   │   ├── vaccination/            — VaccinationRecords, VaccinationForm
│   │   ├── deworming/              — DewormingRecords, DewormingForm
│   │   ├── tasks/                  — TaskList, TaskForm, CompletedTasks, TaskSettings, TaskFilter
│   │   ├── timeline/               — ActivityTimeline, ActivityTimelineViewModel
│   │   ├── settings/               — Settings, ExportImport, DeleteAllData
│   │   ├── onboarding/             — OnboardingScreen, OnboardingViewModel
│   │   ├── familygroup/            — FamilyGroup, Pairing, and Transfer
│   │   └── quickadd/               — QuickAddScreen
│   ├── components/                 — shared composables (PetCard, EmptyState, HealthStatusBadge,
│   │                               —   PetitTopAppBar, SpeedDialFab, TimelineEventCard, WeightChart)
│   ├── navigation/                 — Screen routes, PetitNavGraph, PetitBottomNavBar
│   └── util/                       — EnumExtensions, LocalizedEnums
├── ui/
│   └── theme/                      — Color, Typography, Theme (PetitTheme, LightColorScheme, DarkColorScheme)
├── util/                           — LocaleHelper
├── worker/                         — TaskNotificationWorker, TaskScheduler, AutoTaskService
├── di/                             — AppModule, DatabaseModule, FamilyGroupModule, RepositoryModule
└── PetitApplication.kt             — @HiltAndroidApp
```

This tree reflects the packages that existed at the time of the migration. The
continuous synchronization components described below remain part of the planned architecture.

---

## Conflict Resolution

**Strategy**: Last-Write-Wins based on `updatedAt`

```
Local:  { id: "abc", name: "Luna", updatedAt: 1000 }
Remote: { id: "abc", name: "Little Luna", updatedAt: 1500 }

Result: Remote wins (higher updatedAt)
```

Remote sync (Phase N+2, on hold) will use Firebase Firestore as its transport.

### Battery Use and Local Sync Protocols

Local sync (Phase 2) follows strict battery-use rules:

- **Continuous sync** uses **NSD + TCP over infrastructure Wi-Fi** (the home router) — approximately 5–15 mW
- **Wi-Fi Direct is NEVER used for continuous sync** — only for one-shot transfers
- **Nearby Connections** (Google Play Services) automatically manages the one-shot transport (BLE → BT → Wi-Fi Direct)
- **WorkManager** controls background sync with network constraints
- **NSD is lifecycle-aware**: active in the foreground and unregistered when the app closes
