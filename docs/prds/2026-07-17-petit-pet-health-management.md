# PRD: Pet Health Management in Petit

**Status:** Active

**Last update:** 2026-07-17

## Problem

Pet caregivers need to remember recurring care tasks and keep each animal's
health history in one place that is easy to review and share with veterinary
professionals.

## Objectives

1. Prevent basic care tasks from being forgotten.
2. Centralize the health history of one or more pets.
3. Keep data available offline and exportable when needed.

## Product and technical foundation

## Technical Specification:

* **true offline-first behavior**
* **local notifications and reminders**
* **synchronization at a later stage**
* **synchronization through Firebase/Google Cloud Platform**
* **Android only**, at least for now
* a simple domain with substantial local state and date-based rules

## Technology Stack:

- Native Android in Kotlin
- Jetpack Compose
- Local database: Room
- Small settings / flags / preferences: DataStore
- Synchronization and reminder jobs: WorkManager
- Local sharing: Nearby Connections API (pairing + P2P transfer)
- Local network synchronization: NSD (Network Service Discovery / mDNS) + TCP Sockets
- Device-to-device transfer: Nearby Connections API (P2P_POINT_TO_POINT)

> **Firebase and cloud services are on hold** until there is user demand.
> When resumed: Firebase Auth, Google Drive API, Firestore, Analytics, Crashlytics, FCM, Remote Config.

## Delivery status

- **pet-care:** implemented locally, including manual export/import and no
  required sign-in or backend.
- **local-sharing:** partially implemented, including Nearby Connections
  pairing, one-shot transfer, a local family group, and local network
  synchronization via NSD. See the
  [local-sharing family specs](../../specs/README.md#local-sharing).
- **identity-access, backup-recovery, and cloud-sync:** on hold until there is
  demonstrated user demand. Their specifications remain available for review.

---

## Target Architecture

**Native Android + Room + WorkManager + Firebase/Google Cloud Platform**

### Role of each component

* **Room** = local source of truth
* **WorkManager** = background work and task notifications
* **Nearby Connections API** = device pairing + local P2P transfer
* **NSD (mDNS) + TCP** = discovery and continuous synchronization over the local network (home Wi-Fi)
* **JSON Export/Import** = free universal fallback

> **Components on hold (Firebase/Cloud):**
> Firebase Auth, Google Drive API, Firestore, Analytics, Crashlytics, FCM, Remote Config

---

## Product Model:

Freemium model

### Free

- pet registration
- weight logging
- weight chart
- vaccination
- deworming
- local reminders
- JSON export
- JSON import
- local sharing between household devices (family group)
- continuous synchronization over the local network (home Wi-Fi)
- data transfer between devices (nearby, one-shot)

### Premium (future — when there is demand)

- real-time cloud synchronization (Firebase Firestore)
- automatic remote multi-device synchronization
- Google Drive backup
- data sharing with a veterinarian (optional at a later stage)
- PDF export (optional at a later stage)

---

# Functional Architecture

## Core Principle

The app must be **local-first**, not “cloud-first with a cache.”

In other words:

1. the user saves everything to the local database
2. the UI always reads from the local database
3. remote synchronization happens afterward
4. lack of internet access does not block anything
5. conflicts are resolved in the background

This pattern follows Android's recommended offline-first architecture.

---

## Core Domains

* **Pet**
* **WeightEntry**
* **VaccinationEntry**
* **DewormingEntry**
* **Task** (includes reminders)
* **ExportBundle**
* **SyncStatus**

---

## Suggested Data Structure

### Pet

* `id`
* `name`
* `petType` (`CAT`, `DOG`, `RABBIT`, `BIRD`, `HAMSTER`, `OTHER`)
* `birthDate`
* optional `sex`
* optional `breed`
* optional `microchip`
* optional `passport`
* `createdAt`
* `updatedAt`
* optional `deletedAt`
* `syncStatus`

### WeightEntry

* `id`
* `petId`
* `date`
* `weightGrams`
* optional `note`
* `createdAt`
* `updatedAt`
* `deletedAt`
* `syncStatus`

### VaccinationEntry

* `id`
* `petId`
* `vaccineType`
* optional `customVaccineTypeName`
* `applicationDate`
* `nextDueDate`
* calculated `status` (`OK`, `SCHEDULED`, `OVERDUE`)
* `note`
* `createdAt`
* `updatedAt`
* `deletedAt`
* `syncStatus`

### DewormingEntry

* `id`
* `petId`
* `type` (`INTERNAL`, `EXTERNAL`, `BOTH`)
* optional `medication`
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
* optional `petId`
* `referenceEntityId`
* `title`
* optional `description`
* `scheduledFor`
* `status` (`PENDING`, `COMPLETED`)

---

# Synchronization Strategy

## For the MVP

* every entity has:

  * UUID `id`
  * `updatedAt`
  * `deletedAt`
  * `syncStatus`

### Flow

1. save locally
2. mark as `PENDING_SYNC`
3. WorkManager attempts synchronization when a network is available
4. if successful, mark as `SYNCED`
5. if it fails, keep it pending

## Conflict Resolution

* **last-write-wins** based on `updatedAt`

For this personal/family domain, that is sufficient for the MVP.

### Where conflicts may occur

* the same user using two phones
* a manual import overwriting newer data
* restoration of an old backup

### How to reduce friction

* display “last synchronized”
* allow “force local upload” or “force remote download”
* keep simple synchronization logs

---

# Export/Import Strategy

## Format

A single JSON file is the current official format. The root object contains
`metadata`, `pets`, `weightEntries`, `vaccinationEntries`,
`dewormingEntries`, and `tasks`.

## Why CSV is not the primary format

* CSV breaks down easily when representing relationships
* JSON preserves structure
* it aligns better with future backup/synchronization

## Rules

* free export is always available
* imports include schema + version validation
* preserve version compatibility through migration

---

# Reminders and Notifications

## Weighing

The app will not have a visible calendar component or screen, so reminders
need to be more proactive.

### Approach

On the home page, the user sees all pet health data, including sections such as:

* “**Due for weighing soon**” list
* “**Overdue**” list
* local reminders at a configurable frequency:

  * every 15 days
  * monthly
  * every two months

However, this screen will aggregate all health data, not only weight, so that
it serves as the app's operational dashboard.

Weight charts remain on the standard weighing screen, while reminders appear
on this general dashboard.

### Suggested rule

Each pet has the following fields (using weighing as an example, with the same
logic applying to vaccination and deworming):

* `weightReminderEnabled`
* `weightReminderFrequencyDays`

The app calculates the next date from the latest weight entry.

---

## Vaccination

### Screen

* “Upcoming vaccinations”
* “Overdue”
* “Recently completed”

### Rule

Derived status:

* `OK` → plenty of time remains
* `SCHEDULED` → approaching the due window
* `OVERDUE` → past the due date

The same logic applies to deworming.

---

# Screen Structure

## 1. Home Page

The Home screen is an operational dashboard where the user can see everything
that needs attention and the overall health status of their pets.

If the user has not registered any pets, the Home screen prompts them to
register their first pet.

If the user has registered pets, the Home screen is a general dashboard with
reminders and health status information.

If the user has registered a pet but has not logged a weight or vaccination,
the Home screen prompts them to add the first weight/vaccination record.

### Content

* pet summary
* upcoming reminders
* upcoming vaccinations
* upcoming deworming treatments
* latest weight for each pet
* CTA to add a record quickly

### If the user has not paired a device

* subtle banner on the Home screen:

  * "Share data with your family"
  * "Connect another device"
  * Tap → opens the Profile tab > Family Group section

---

## 2. Profile Page (formerly Settings)

The "Profile" tab in the bottom navigation (Person icon) is the user's personal hub.

### Profile sections (in order)

1. **Family Group** — group status or onboarding card if no device is paired
2. **Settings** — theme (system/light/dark), language (system/pt-BR/en/es)
3. **Data** — export, import, delete all data
4. **About** — app version

### If the user has not paired a device (Family Group section)

* onboarding card:
  * "Share your pets' data with your family"
  * "Works without internet access!"
* "Pair device" button
* "Join family group" button

### If the user has paired a device (Family Group section)

* partner device name and synchronization status
* "Manage Group" link → management screen

### Rule

* clearly explain that:
  * sharing works only on the local network
  * data remains on the devices, with no cloud storage

This is important for user trust.

---

## 3. Pet Registration

* name
* date of birth
* optional microchip
* optional passport
* optional photo at a later stage
* preferred weighing frequency
* notes

---

## 4. Weighing

### List

* history by pet
* current weight
* change from the previous weighing
* date of the latest weighing

### Entry

* select pet
* date
* weight
* optional note

### Chart

* timeline for each pet
* Y-axis in grams or kilograms
* option to view 3 months / 6 months / 1 year

---

## 5. Deworming

### Entry

* pet
* internal/external type
* medication
* application date
* next application

### List

* upcoming
* overdue
* history

---

## 6. Vaccination

### Entry

* pet
* vaccine
* application date
* next application

### List

* upcoming
* overdue
* history

---

## 7. Export / Import

* export local file
* import local file
* restore from cloud backup
* create a manual backup now
* date of latest export/synchronization

---

# Initial Delivery Scope

## Pet care foundation

Core capabilities for a useful offline-first release.

### Technical deliverables

* Pet
* WeightEntry
* Export/Import
* simple Home screen
* local weighing reminder
* weight chart
* foundational Room + WorkManager setup
* architecture ready for future synchronization

### Product goal

Validate:

* offline flow
* data model
* registration and history UX
* reminder quality

---

## Deworming

This capability can reuse much of the vaccination data structure and screens:

* entry pattern
* list of upcoming dates
* local reminder
* export/import

---

## Vaccination

This follows the same care-history pattern as deworming, with additional
semantics for each vaccine type.

---

# Important Technical Decisions

## 1. Local database as the source of truth

This is the project's most important decision.
The UI must never read directly from the cloud.

## 2. Store dates internally in UTC

* store ISO UTC / epoch values
* format them locally for the user

## 3. Store weight in grams, not as a float in kilograms

To prevent precision issues:

* store `3600`
* display `3.60 kg`

## 4. Soft delete

To keep synchronization consistent:

* use `deletedAt`
* do not immediately delete records from the database

## 5. Export schema versioning

The exported file must include:

* `appVersion`
* `schemaVersion`
* `exportedAt`

---

# Security and Privacy

The risk is low because this is a pet data app, but I would still include:

* an encrypted local database later, if the product evolves
* authentication tokens stored securely (automatic Firebase Auth token management)
* no sensitive data in basic preferences
* Firebase Storage with Firestore Security Rules to isolate data by user
* Firebase Auth with Firestore Security Rules

---

# What Will **Not** Be Built Initially

* multiple users
* real-time collaboration
* a backend as the primary source
* Drive as the primary database
* complicated pricing
* simultaneous iOS development
* a visual calendar
* veterinarian integration
* AI-based health monitoring

---

# Initial Technical Documentation

Below is a documentation outline that I would use as the project foundation.

## 1. Product Vision

**Working name:** Petit - The Pet Health Tracker
**Objective:** enable users to track pet weight, vaccination, and deworming offline, with optional backup/synchronization

## 2. Functional Requirements

* register pets
* log weights
* view weight trends
* record vaccinations
* list upcoming vaccinations
* record deworming treatments
* list upcoming deworming treatments
* export data
* import data
* optional Google sign-in
* optional cloud synchronization (Firebase Firestore)
* optional backup to Firebase Storage
* local notifications for weighing and upcoming events

## 3. Non-functional Requirements

* work offline
* synchronize when internet access is available
* fast startup
* simple UI
* no dependency on a visual calendar
* reliable local persistence
* supported schema migration
* basic error observability

## 4. Architecture

Layers:

* UI
* ViewModel
* Use Cases
* Repository
* Local Data Source
* Remote Data Source
* Sync Engine

## 5. Data Model

* Pet
* WeightEntry
* VaccinationEntry
* DewormingEntry
* Task
* SyncMetadata

## 6. Offline-first Strategy

* Room as the source of truth
* WorkManager for synchronization
* local change queues
* exponential retry
* last-write-wins in the MVP

## 7. Monetization Strategy

* free: all local functionality
* premium: sign-in + cloud (Firebase) + automatic backups

## 8. Roadmap

The canonical roadmap is the family-based [specs index](../../specs/README.md),
where each capability has its own status. Current priorities are pet care and
local sharing. Identity, remote backup, and cloud synchronization remain on
hold until there is demonstrated user demand.

---

# Execution

1. **define the data model and status rules**
2. **design the offline-first architecture**
3. **write the MVP technical documentation**
4. **design the core pet-care screens**
5. **break the work down into a technical backlog**
