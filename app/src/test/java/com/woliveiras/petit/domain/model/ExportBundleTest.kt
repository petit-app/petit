package com.woliveiras.petit.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.LocalDateTime
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExportBundleTest {

  @Test
  fun completedTaskRoundTripsWithItsStatusAndReference() {
    val completed =
      Task(
        id = "task-1",
        petId = "pet-1",
        kind = TaskKind.VACCINATION,
        referenceEntityId = "vacc-1",
        title = "Vaccination",
        scheduledFor = LocalDateTime.of(2026, 7, 20, 9, 0),
        status = TaskStatus.COMPLETED,
        createdAt = 1L,
        updatedAt = 2L,
      )
    val bundle = emptyBundle(tasks = listOf(completed))

    val restored = ExportBundle.fromJson(bundle.toJson())

    assertThat(restored.tasks).containsExactly(completed)
  }

  @Test
  fun tombstoneRoundTripsWithItsDeletionTimestamp() {
    val deleted =
      Task(
        id = "task-1",
        kind = TaskKind.CUSTOM,
        title = "Deleted",
        scheduledFor = LocalDateTime.of(2026, 7, 20, 9, 0),
        createdAt = 1L,
        updatedAt = 20L,
        deletedAt = 20L,
      )

    val restored = ExportBundle.fromJson(emptyBundle(tasks = listOf(deleted)).toJson())

    assertThat(restored.tasks).containsExactly(deleted)
  }

  @Test
  fun careCompatibilityValuesRoundTripWithoutLocalizationOrCatalogRewrite() {
    val pets =
      PetType.entries.mapIndexed { index, petType ->
        Pet(
          id = "pet-$index",
          name = "Pet $index",
          petType = petType,
          sex = Sex.UNKNOWN,
          breed = listOf("PERSIAN", "Custom rescue breed", "legacy_Breed-ç")[index % 3],
          createdAt = 1L,
          updatedAt = 1L,
        )
      }
    val vaccination =
      VaccinationEntry(
        id = "vacc-1",
        petId = pets.first().id,
        vaccineType = VaccineType.OTHER,
        customVaccineTypeName = "Legacy veterinary vaccine",
        applicationDate = java.time.LocalDate.of(2026, 7, 1),
        createdAt = 1L,
        updatedAt = 1L,
      )
    val deworming =
      DewormingEntry(
        id = "dew-1",
        petId = pets.last().id,
        type = DewormingType.BOTH,
        medication = "Legacy active ingredient",
        applicationDate = java.time.LocalDate.of(2026, 7, 1),
        createdAt = 1L,
        updatedAt = 1L,
      )

    val restored =
      ExportBundle.fromJson(
        emptyBundle()
          .copy(
            pets = pets,
            vaccinationEntries = listOf(vaccination),
            dewormingEntries = listOf(deworming),
          )
          .toJson()
      )

    assertThat(restored.pets).isEqualTo(pets)
    assertThat(restored.vaccinationEntries).containsExactly(vaccination)
    assertThat(restored.dewormingEntries).containsExactly(deworming)
  }

  @Test
  fun breedIdentityAndFallbackRoundTripWhileOlderPayloadsRemainCompatible() {
    val pet =
      Pet(
        id = "pet-identity",
        name = "Mimi",
        petType = PetType.CAT,
        breed = "Siamese",
        breedId = "VBO:0100221",
        createdAt = 1L,
        updatedAt = 2L,
      )

    val restored = ExportBundle.fromJson(emptyBundle().copy(pets = listOf(pet)).toJson())
    assertThat(restored.pets).containsExactly(pet)

    val legacyJson = emptyBundle().copy(pets = listOf(pet)).toJson()
    legacyJson.getJSONArray("pets").getJSONObject(0).remove("breedId")
    assertThat(ExportBundle.fromJson(legacyJson).pets.single().breedId).isNull()
  }

  @Test
  fun invalidBreedIdentityIsRejectedInsteadOfSilentlyDiscarded() {
    val pet =
      Pet(
        id = "pet-invalid",
        name = "Mimi",
        breed = "Fallback",
        breedId = "invalid identity",
        createdAt = 1L,
        updatedAt = 2L,
      )

    org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
      ExportBundle.fromJson(emptyBundle().copy(pets = listOf(pet)).toJson())
    }
  }

  @Test
  fun taskSubjectRoundTripsAndOlderBundlesRestoreWithoutOne() {
    val task =
      Task(
        id = "task-subject",
        petId = "pet-1",
        kind = TaskKind.MEDICATION,
        subjectName = "Apoquel",
        title = "Apoquel",
        scheduledFor = LocalDateTime.of(2026, 7, 20, 9, 0),
        createdAt = 1L,
        updatedAt = 2L,
      )

    val json = emptyBundle(tasks = listOf(task)).toJson()
    assertThat(ExportBundle.fromJson(json).tasks).containsExactly(task)

    val legacyJson = emptyBundle(tasks = listOf(task)).toJson()
    legacyJson.getJSONArray("tasks").getJSONObject(0).remove("subjectName")
    legacyJson.getJSONArray("tasks").getJSONObject(0).remove("subjectCode")
    val restored = ExportBundle.fromJson(legacyJson).tasks.single()
    assertThat(restored.subjectName).isNull()
    assertThat(restored.subjectCode).isNull()
  }

  @Test
  fun membershipChangesRoundTripAndContributeToTheTransferredEntityCount() {
    val change =
      MembershipChange(
        groupId = MembershipChange.groupIdForKey("group-key"),
        memberId = "member-1",
        type = MembershipChangeType.RENAME,
        deviceName = "Kitchen tablet",
        timestamp = 20L,
      )
    val bundle = emptyBundle().copy(membershipChanges = listOf(change))

    val restored = ExportBundle.fromJson(bundle.toJson())

    assertThat(restored.membershipChanges).containsExactly(change)
    assertThat(restored.entityCount).isEqualTo(1)
  }

  @Test
  fun legacyRemindersAreConvertedToCurrentTasksBeforeValidation() {
    val legacy =
      emptyBundleJson().apply {
        put(
          "reminders",
          JSONArray()
            .put(
              JSONObject()
                .put("id", "legacy-1")
                .put("petId", "pet-1")
                .put("title", "Weigh Mimi")
                .put("scheduledAt", "2026-08-01T09:00:00")
                .put("completed", true)
                .put("createdAt", 1L)
                .put("updatedAt", 2L)
            ),
        )
      }

    val restored = ExportBundle.fromJson(legacy)

    assertThat(restored.tasks)
      .containsExactly(
        Task(
          id = "legacy-1",
          petId = "pet-1",
          kind = TaskKind.CUSTOM,
          title = "Weigh Mimi",
          scheduledFor = LocalDateTime.of(2026, 8, 1, 9, 0),
          status = TaskStatus.COMPLETED,
          createdAt = 1L,
          updatedAt = 2L,
        )
      )
  }

  @Test(expected = IllegalArgumentException::class)
  fun malformedLegacyReminderIsRejectedBeforeImportCanMutateData() {
    val legacy =
      emptyBundleJson().apply {
        put("reminders", JSONArray().put(JSONObject().put("id", "missing-required-fields")))
      }

    ExportBundle.fromJson(legacy)
  }

  @Test(expected = org.json.JSONException::class)
  fun corruptedCurrentTaskIsRejected() {
    val corrupted =
      emptyBundleJson()
        .put(
          "tasks",
          JSONArray()
            .put(
              JSONObject()
                .put("id", "task-1")
                .put("kind", "CUSTOM")
                .put("title", "Missing schedule")
                .put("createdAt", 1L)
                .put("updatedAt", 1L)
            ),
        )

    ExportBundle.fromJson(corrupted)
  }

  @Test
  fun unsupportedSchemaAndOrphanTaskReferencesAreRejectedByValidation() {
    val orphanTask =
      Task(
        id = "task-1",
        petId = "missing-pet",
        kind = TaskKind.CUSTOM,
        title = "Orphan",
        scheduledFor = LocalDateTime.of(2026, 8, 1, 9, 0),
        createdAt = 1L,
        updatedAt = 1L,
      )
    val bundle =
      emptyBundle(tasks = listOf(orphanTask))
        .copy(metadata = ExportMetadata("1.0", "2026-07-17T00:00:00Z", schemaVersion = 99))

    val errors = ExportBundle.validate(bundle)

    assertThat(errors).hasSize(2)
    assertThat(errors[0]).contains("99")
    assertThat(errors[1]).contains("missing-pet")
  }

  @Test
  fun aCraftedTaskSubjectIsRejectedByValidation() {
    val craftedTask =
      Task(
        id = "task-1",
        kind = TaskKind.MEDICATION,
        subjectCode = "'; DROP TABLE tasks; --",
        subjectName = "a".repeat(101),
        title = "Crafted",
        scheduledFor = LocalDateTime.of(2026, 8, 1, 9, 0),
        createdAt = 1L,
        updatedAt = 1L,
      )

    val errors = ExportBundle.validate(emptyBundle(tasks = listOf(craftedTask)))

    assertThat(errors).hasSize(2)
    assertThat(errors[0]).contains("task-1")
    assertThat(errors[1]).contains("task-1")
  }

  private fun emptyBundle(tasks: List<Task> = emptyList()) =
    ExportBundle(
      metadata = ExportMetadata(appVersion = "1.0", exportDate = "2026-07-17T00:00:00Z"),
      pets = emptyList(),
      weightEntries = emptyList(),
      vaccinationEntries = emptyList(),
      dewormingEntries = emptyList(),
      tasks = tasks,
    )

  private fun emptyBundleJson() =
    JSONObject()
      .put(
        "metadata",
        ExportMetadata(appVersion = "1.0", exportDate = "2026-07-17T00:00:00Z").toJson(),
      )
      .put("pets", JSONArray())
      .put("weightEntries", JSONArray())
      .put("vaccinationEntries", JSONArray())
      .put("dewormingEntries", JSONArray())
}
