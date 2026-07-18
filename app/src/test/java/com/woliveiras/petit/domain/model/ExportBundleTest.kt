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
