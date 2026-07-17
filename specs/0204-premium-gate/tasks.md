# Tasks: Premium Gate

Spec: [spec.md](./spec.md) · Plan: [plan.md](./plan.md)

> Spec status: **On Hold**. All tasks remain pending until explicit approval.

## Tasks

- [ ] **Scenario 1: See a premium indicator on a locked feature** (test-type: both)
  - blocked-by: spec 0201
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 1: See a premium indicator on a locked feature” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN that I am a free user WHEN I see the "Real-time synchronization" option in settings THEN I see a ⭐ or 🔒 icon indicating that it is premium AND when I tap it, I see information about the premium plan
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 2: Try to use a premium feature** (test-type: both)
  - blocked-by: spec 0201; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 2: Try to use a premium feature” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN that I am a free user WHEN I try to enable "Real-time synchronization" THEN I see a bottom sheet or dialog explaining: - What the feature does - That it is exclusive to premium users - A button to view plans
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 3: List premium benefits** (test-type: both)
  - blocked-by: spec 0201; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 3: List premium benefits” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN that I am in the app WHEN I open "View premium plans" THEN I see a list of benefits: - ☁️ Real-time cloud synchronization - 📱 Multiple devices synchronized automatically - 👨‍👩‍👧 Share with family - 📄 Export PDF (future)
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 4: Check premium status** (test-type: both)
  - blocked-by: spec 0201; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 4: Check premium status” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN that I am a premium user WHEN I open settings THEN I see "Plan: Premium" AND I do not see lock indicators on premium features AND the premium features are available
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`

- [ ] **Scenario 5: Free features available without login** (test-type: both)
  - blocked-by: spec 0201; previous task in this spec
  - summary: deliver this behavior as a vertical slice, including domain, persistence/service, and interface where applicable.
  - desired behavior: the “Scenario 5: Free features available without login” flow works end to end without compromising local data.
  - acceptance criteria: GIVEN that I am not logged in WHEN I use the app THEN I can register pets, record weights and vaccinations, and create reminders AND I can export/import JSON BUT I cannot back up to Google Drive (requires login) AND I cannot use real-time synchronization (premium) ---
  - verification: `./gradlew test` and `./gradlew connectedDebugAndroidTest`
