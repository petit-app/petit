# PRD: Pet Health Management in Petit

**Status:** Active

**Last update:** 2026-07-26

## Problem

Pet caregivers need to remember recurring care tasks and keep each animal's
health history in one place that is easy to review and share with veterinary
professionals.

## Objectives

1. Prevent basic care tasks from being forgotten.
2. Centralize the health history of one or more pets.
3. Keep data available offline and exportable when needed.
4. Make care-record presets deliberate for every supported species without
   presenting them as veterinary prescriptions.
5. Keep every officially supported language complete across the entire app.
6. Give cat and dog caregivers an internationally sourced, searchable breed
   catalog that remains available offline and preserves custom or historical
   values.

## Product and technical foundation

## Technical Specification:

* **true offline-first behavior**
* **local notifications and reminders**
* **synchronization at a later stage**
* **synchronization through Firebase/Google Cloud Platform**
* **Android only**, at least for now
* a simple domain with substantial local state and date-based rules
* English and Brazilian Portuguese as the officially supported app languages

## Technology Stack:

- Native Android in Kotlin
- Jetpack Compose
- Local database: Room
- Small settings / flags / preferences: DataStore
- Synchronization and reminder jobs: WorkManager
- Local sharing: Nearby Connections API (pairing + P2P transfer)
- Planned local network synchronization: NSD (Network Service Discovery / mDNS) + TCP Sockets
- Device-to-device transfer: Nearby Connections API (`P2P_STAR`)

> **Petit-managed cloud services are on hold** until there is user demand.
> User-owned Google Drive backup is a separate free capability that uses Google
> authorization without requiring Firebase or a Petit account.

## Delivery status

- **pet-care:** core local workflows are available, but several capabilities
  remain in progress while documented validation and presentation gaps are
  addressed. No sign-in or backend is required.
- **local-sharing:** partially implemented, with Nearby Connections pairing,
  one-shot transfer, and local family-group components still under validation.
  Continuous local-network synchronization through NSD and TCP remains a
  draft. See the [local-sharing family specs](../specs/README.md#local-sharing).
- **identity-access and cloud-sync:** on hold until there is demonstrated user
  demand for Petit-managed services. **backup-recovery:** under renewed design
  for free user-owned Google Drive backup; its updated specs require approval.

---

## Target Architecture

**Native Android + Room + WorkManager + optional user-owned Google Drive**

### Role of each component

* **Room** = local source of truth
* **WorkManager** = background work and task notifications
* **Nearby Connections API** = device pairing + local P2P transfer
* **NSD (mDNS) + TCP** = discovery and continuous synchronization over the local network (home Wi-Fi)
* **JSON Export/Import** = free universal fallback
* **Google Drive appDataFolder** = optional free user-owned backup storage

> **Petit-managed components on hold:**
> hosted identity, Firestore, Analytics, Crashlytics, FCM, Remote Config, and
> other Petit Cloud infrastructure

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
- PDF export
- local sharing between household devices (family group)
- continuous synchronization over the local network (home Wi-Fi)
- data transfer between devices (nearby, one-shot)
- manual and automatic backup in the user's Google Drive
- restore and management of the user's Google Drive backups

### Petit Cloud (future — when there is demand)

- real-time cloud synchronization (Firebase Firestore)
- automatic remote multi-device synchronization
- data sharing with a veterinarian (optional at a later stage)

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
* optional `breedId` (VBO ID or a Petit-owned non-breed category ID)
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
* additive breed identifiers keep a portable breed-name fallback so older
  Petit versions can still display the record

---

# International Breed Catalog

Cat and dog breed selection uses an offline catalog derived from a pinned
release of the [Vertebrate Breed Ontology](https://monarchinitiative.org/ontologies/vbo).
VBO provides stable identifiers, synonyms, source attribution, and
source-specific recognition metadata.

The catalog uses the following registration bodies to decide which entries are
offered for new selection:

* FCI for dog breeds
* FIFe and TICA for cat breeds

Recognition means that a registration body catalogs the breed. It is not a
health, temperament, suitability, or veterinary endorsement.

Petit ships a reviewed snapshot with the application. The app does not download
breed data at runtime. Catalog updates happen through normal app releases and
record the VBO release, source references, checksums, and review date.

Breed names are presentation data. Persisted catalog selections use stable IDs
plus a canonical name fallback. Manual values remain exact user data and are
never translated or matched to a breed by fuzzy inference. Mixed breed, unknown
breed, and manual entry are Petit-owned categories rather than VBO breed
concepts.

For cats and dogs, breed selection opens as a dedicated screen instead of a
dialog over the pet form. The screen gives the catalog, search results, and
non-breed choices the full viewport. A choice remains a draft until the
caregiver confirms it. Confirmation returns to the pet form and applies the
chosen ID and fallback together. Navigating back without confirmation leaves
the form's previous breed value unchanged.

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
2. **Settings** — theme (system/light/dark), language (system/pt-BR/en)
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
* species: cat, dog, rabbit, bird, hamster, or other
* date of birth
* optional species-aware breed preset or manual breed
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

Treatment presets are recording conveniences, not treatment advice. The app
must not offer one commercial-product list to every species, calculate doses,
diagnose disease, or imply that a product is appropriate. When no reviewed,
market-specific catalog exists, the caregiver records the product or treatment
manually from veterinary or product-label information.

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

New-entry vaccine choices are filtered by species. Historical and custom values
remain readable and editable after species or catalog changes. Suggested next
dates are not clinical recommendations and must not be generated without an
approved, traceable source.

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
* short-lived Google authorization tokens managed by Google Identity Services and never stored in backup archives
* no sensitive data in basic preferences
* Google Drive `appDataFolder` with the narrow `drive.appdata` scope for user-owned backups
* no Petit account, client-side encryption, or Firebase dependency for Google Drive backup

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
* optional complete ZIP backup to the user's Google Drive `appDataFolder`
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
* free: local capabilities and user-owned Google Drive backup, restore, management, and automation
* paid Petit Cloud: only capabilities that use infrastructure operated by Petit

## 8. Roadmap

The canonical roadmap is the family-based [specs index](../specs/README.md),
where each capability has its own status. Current priorities are pet care,
local sharing, and approval of the revised user-owned backup design. Petit
identity and hosted cloud synchronization remain on hold until there is
demonstrated user demand.

---

# Execution

1. **define the data model and status rules**
2. **design the offline-first architecture**
3. **write the MVP technical documentation**
4. **design the core pet-care screens**
5. **break the work down into a technical backlog**
