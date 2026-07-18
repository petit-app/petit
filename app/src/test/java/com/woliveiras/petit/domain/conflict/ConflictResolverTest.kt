package com.woliveiras.petit.domain.conflict

import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.domain.model.DewormingEntry
import com.woliveiras.petit.domain.model.DewormingType
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.Task
import com.woliveiras.petit.domain.model.TaskKind
import com.woliveiras.petit.domain.model.VaccinationEntry
import com.woliveiras.petit.domain.model.VaccineType
import com.woliveiras.petit.domain.model.WeightEntry
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Test

class ConflictResolverTest {
  private val resolver = ConflictResolver()

  @Test
  fun missingUuidIsInsertedExactlyOnce() {
    val remote = task(title = "Remote", updatedAt = 10).asConflictVersion()

    val inserted = resolver.resolve(local = null, remote = remote)
    val retried = resolver.resolve(local = inserted.selected, remote = remote)

    assertThat(inserted.outcome).isEqualTo(ConflictOutcome.Inserted)
    assertThat(retried.outcome).isEqualTo(ConflictOutcome.Identical)
  }

  @Test
  fun newestEditWinsInBothInputOrders() {
    val older = task(title = "Older", updatedAt = 10).asConflictVersion()
    val newer = task(title = "Newer", updatedAt = 20).asConflictVersion()

    assertThat(resolver.resolve(older, newer).selected.value.title).isEqualTo("Newer")
    assertThat(resolver.resolve(newer, older).selected.value.title).isEqualTo("Newer")
  }

  @Test
  fun editDeleteUsesTheNewestEffectiveEvent() {
    val deletion = task(title = "Deleted", updatedAt = 20, deletedAt = 20).asConflictVersion()
    val olderEdit = task(title = "Older edit", updatedAt = 10).asConflictVersion()
    val newerEdit = task(title = "Restored", updatedAt = 30).asConflictVersion()

    assertThat(resolver.resolve(olderEdit, deletion).selected.value.deletedAt).isEqualTo(20)
    assertThat(resolver.resolve(deletion, newerEdit).selected.value.title).isEqualTo("Restored")
  }

  @Test
  fun equalTimestampDeletionWinsOverActiveVersionSymmetrically() {
    val active = task(title = "Active", updatedAt = 20).asConflictVersion()
    val deleted = task(title = "Active", updatedAt = 20, deletedAt = 20).asConflictVersion()

    assertThat(resolver.resolve(active, deleted).selected).isEqualTo(deleted)
    assertThat(resolver.resolve(deleted, active).selected).isEqualTo(deleted)
  }

  @Test
  fun equalTimestampActivePayloadUsesCanonicalLexicographicOrder() {
    val alpha = task(title = "Alpha", updatedAt = 20).asConflictVersion()
    val omega = task(title = "Omega", updatedAt = 20).asConflictVersion()

    val firstOrder = resolver.resolve(alpha, omega).selected
    val reverseOrder = resolver.resolve(omega, alpha).selected

    assertThat(firstOrder).isEqualTo(reverseOrder)
    assertThat(firstOrder.value.title).isEqualTo("Omega")
  }

  @Test
  fun canonicalPayloadEscapesDelimiterLikeUserTextWithoutCollisions() {
    val first =
      task(title = "x, description=y", updatedAt = 20).copy(description = "z").asConflictVersion()
    val second =
      task(title = "x", updatedAt = 20).copy(description = "y, description=z").asConflictVersion()

    assertThat(first.canonicalPayload).isNotEqualTo(second.canonicalPayload)
    assertThat(resolver.resolve(first, second).selected)
      .isEqualTo(resolver.resolve(second, first).selected)
  }

  @Test
  fun duplicateBatchIsNormalizedDeterministically() {
    val old = task(title = "Old", updatedAt = 10).asConflictVersion()
    val deleted = task(title = "Deleted", updatedAt = 30, deletedAt = 30).asConflictVersion()
    val middle = task(title = "Middle", updatedAt = 20).asConflictVersion()

    val forward = resolver.normalize(listOf(old, deleted, middle))
    val reverse = resolver.normalize(listOf(middle, deleted, old))

    assertThat(forward).containsExactly(deleted)
    assertThat(reverse).isEqualTo(forward)
  }

  @Test
  fun deleteDeleteIsIdempotentAndChoosesTheNewestTombstone() {
    val older = task(title = "Old", updatedAt = 10, deletedAt = 10).asConflictVersion()
    val newer = task(title = "New", updatedAt = 20, deletedAt = 20).asConflictVersion()

    val resolved = resolver.resolve(older, newer)
    val retried = resolver.resolve(resolved.selected, newer)

    assertThat(resolved.selected).isEqualTo(newer)
    assertThat(retried.outcome).isEqualTo(ConflictOutcome.Identical)
  }

  @Test
  fun everyShareableEntityHasACompleteSymmetricCanonicalPayload() {
    assertSymmetric(
      Pet("pet-1", "Alpha", createdAt = 1, updatedAt = 20).asConflictVersion(),
      Pet("pet-1", "Omega", createdAt = 1, updatedAt = 20).asConflictVersion(),
    )
    assertSymmetric(
      WeightEntry(
          "weight-1",
          "pet-1",
          LocalDate.of(2026, 7, 18),
          4_200,
          note = "Alpha",
          createdAt = 1,
          updatedAt = 20,
        )
        .asConflictVersion(),
      WeightEntry(
          "weight-1",
          "pet-1",
          LocalDate.of(2026, 7, 18),
          4_200,
          note = "Omega",
          createdAt = 1,
          updatedAt = 20,
        )
        .asConflictVersion(),
    )
    assertSymmetric(
      VaccinationEntry(
          "vaccination-1",
          "pet-1",
          VaccineType.RABIES,
          applicationDate = LocalDate.of(2026, 7, 18),
          note = "Alpha",
          createdAt = 1,
          updatedAt = 20,
        )
        .asConflictVersion(),
      VaccinationEntry(
          "vaccination-1",
          "pet-1",
          VaccineType.RABIES,
          applicationDate = LocalDate.of(2026, 7, 18),
          note = "Omega",
          createdAt = 1,
          updatedAt = 20,
        )
        .asConflictVersion(),
    )
    assertSymmetric(
      DewormingEntry(
          "deworming-1",
          "pet-1",
          DewormingType.INTERNAL,
          applicationDate = LocalDate.of(2026, 7, 18),
          note = "Alpha",
          createdAt = 1,
          updatedAt = 20,
        )
        .asConflictVersion(),
      DewormingEntry(
          "deworming-1",
          "pet-1",
          DewormingType.INTERNAL,
          applicationDate = LocalDate.of(2026, 7, 18),
          note = "Omega",
          createdAt = 1,
          updatedAt = 20,
        )
        .asConflictVersion(),
    )
    assertSymmetric(task("Alpha", 20).asConflictVersion(), task("Omega", 20).asConflictVersion())
  }

  private fun <T> assertSymmetric(first: ConflictVersion<T>, second: ConflictVersion<T>) {
    assertThat(first.canonicalPayload).isNotEqualTo(second.canonicalPayload)
    assertThat(resolver.resolve(first, second).selected.canonicalPayload)
      .isEqualTo(resolver.resolve(second, first).selected.canonicalPayload)
  }

  private fun task(title: String, updatedAt: Long, deletedAt: Long? = null): Task =
    Task(
      id = "task-1",
      kind = TaskKind.CUSTOM,
      title = title,
      scheduledFor = LocalDateTime.of(2026, 7, 18, 10, 0),
      createdAt = 1,
      updatedAt = updatedAt,
      deletedAt = deletedAt,
    )
}
