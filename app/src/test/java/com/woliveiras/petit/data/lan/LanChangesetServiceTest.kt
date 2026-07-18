package com.woliveiras.petit.data.lan

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.mapper.toEntity
import com.woliveiras.petit.data.repository.DewormingEntryRepositoryImpl
import com.woliveiras.petit.data.repository.FamilyGroupRepository
import com.woliveiras.petit.data.repository.PetRepositoryImpl
import com.woliveiras.petit.data.repository.TaskRepositoryImpl
import com.woliveiras.petit.data.repository.VaccinationEntryRepositoryImpl
import com.woliveiras.petit.data.repository.WeightEntryRepositoryImpl
import com.woliveiras.petit.domain.lan.LanChangesetBatcher
import com.woliveiras.petit.domain.lan.LanProtocolException
import com.woliveiras.petit.domain.lan.LanSessionScope
import com.woliveiras.petit.domain.model.ExportBundle
import com.woliveiras.petit.domain.model.ExportMetadata
import com.woliveiras.petit.domain.model.FamilyGroupInfo
import com.woliveiras.petit.domain.model.FamilyGroupMember
import com.woliveiras.petit.domain.model.MembershipChange
import com.woliveiras.petit.domain.model.MembershipChangeType
import com.woliveiras.petit.domain.model.PendingDeparture
import com.woliveiras.petit.domain.model.Pet
import com.woliveiras.petit.domain.model.SyncLog
import com.woliveiras.petit.domain.usecase.ExportImportUseCase
import com.woliveiras.petit.domain.usecase.MergeDataUseCase
import java.time.Clock
import java.util.Base64
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LanChangesetServiceTest {
  private lateinit var database: PetitDatabase
  private lateinit var family: FakeFamilyGroupRepository
  private lateinit var service: LanChangesetService

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database =
      Room.inMemoryDatabaseBuilder(context, PetitDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    family = FakeFamilyGroupRepository()
    val export = createExportUseCase(context, database)
    service =
      LanChangesetService(database, export, MergeDataUseCase(export, database, family), family)
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun lostAckRetryReusesDurableResultWithoutDuplicateLog() = runTest {
    val batch = LanChangesetBatcher.create(PEER_ID, 0L, bundle("pet-1")).single()

    val first =
      service.apply(
        PEER_ID,
        "Kitchen",
        batch.batchId,
        batch.cursor,
        batch.payload,
        LanSessionScope.CLINICAL,
        GROUP_ID,
      )
    val retry =
      service.apply(
        PEER_ID,
        "Kitchen",
        batch.batchId,
        batch.cursor,
        batch.payload,
        LanSessionScope.CLINICAL,
        GROUP_ID,
      )

    assertThat(first.replayed).isFalse()
    assertThat(retry.replayed).isTrue()
    assertThat(database.petDao().getByIdIncludingDeleted("pet-1")).isNotNull()
    val logs = database.syncLogDao().getAllSyncLogs().first()
    assertThat(logs).hasSize(1)
    assertThat(logs.single().syncType).isEqualTo("LAN")
    assertThat(logs.single().id).isEqualTo(batch.batchId)
  }

  @Test
  fun retryReconcilesAssociationAfterPostCommitDataStoreFailure() = runTest {
    val batch = LanChangesetBatcher.create(PEER_ID, 0L, bundle("pet-reconcile")).single()
    family.reconcileFailures = 1

    val first = runCatching {
      service.apply(
        PEER_ID,
        "Kitchen",
        batch.batchId,
        batch.cursor,
        batch.payload,
        LanSessionScope.CLINICAL,
        GROUP_ID,
      )
    }
    val retry =
      service.apply(
        PEER_ID,
        "Kitchen",
        batch.batchId,
        batch.cursor,
        batch.payload,
        LanSessionScope.CLINICAL,
        GROUP_ID,
      )

    assertThat(first.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    assertThat(retry.replayed).isTrue()
    assertThat(family.reconcileCalls).isEqualTo(2)
    assertThat(database.syncLogDao().getAllSyncLogs().first()).hasSize(1)
  }

  @Test
  fun membershipOnlyScopeRejectsClinicalPayloadBeforeRoomChanges() = runTest {
    val batch = LanChangesetBatcher.create(PEER_ID, 0L, bundle("forbidden-pet")).single()

    val result = runCatching {
      service.apply(
        PEER_ID,
        "Kitchen",
        batch.batchId,
        batch.cursor,
        batch.payload,
        LanSessionScope.MEMBERSHIP_ONLY,
        GROUP_ID,
      )
    }

    assertThat(result.exceptionOrNull()).isInstanceOf(LanProtocolException::class.java)
    assertThat(database.petDao().getByIdIncludingDeleted("forbidden-pet")).isNull()
    assertThat(database.syncLogDao().getAllSyncLogs().first()).isEmpty()
  }

  @Test
  fun authenticatedGroupCannotInjectMembershipEventForAnotherGroup() = runTest {
    val poisoned =
      ExportBundle(
        metadata = ExportMetadata("1", "2026-07-18T00:00:00Z"),
        pets = emptyList(),
        weightEntries = emptyList(),
        vaccinationEntries = emptyList(),
        dewormingEntries = emptyList(),
        tasks = emptyList(),
        membershipChanges =
          listOf(
            MembershipChange(
              groupId = "b".repeat(64),
              memberId = LOCAL_ID,
              type = MembershipChangeType.RENAME,
              deviceName = "Poisoned",
              timestamp = 1L,
            )
          ),
      )
    val batch = LanChangesetBatcher.create(PEER_ID, 0L, poisoned).single()

    val result = runCatching {
      service.apply(
        PEER_ID,
        "Kitchen",
        batch.batchId,
        batch.cursor,
        batch.payload,
        LanSessionScope.CLINICAL,
        GROUP_ID,
      )
    }

    assertThat(result.exceptionOrNull()).isInstanceOf(LanProtocolException::class.java)
    assertThat(database.membershipChangeDao().getAll()).isEmpty()
  }

  @Test
  fun outboundCursorAndDepartureCredentialAdvanceOnlyAfterAck() = runTest {
    val departure =
      MembershipChange(
        groupId = GROUP_ID,
        memberId = LOCAL_ID,
        type = MembershipChangeType.LEAVE,
        timestamp = 15L,
      )
    family.pending += PendingDeparture(departure, GROUP_KEY)
    val pending = PendingDeparture(departure, GROUP_KEY)
    val batch = service.prepareDeparture(PEER_ID, pending).single()

    assertThat(service.outboundCursor(PEER_ID, GROUP_KEY)).isEqualTo(0L)
    assertThat(family.acknowledged).isEmpty()

    service.acknowledgeOutbound(PEER_ID, "Kitchen", departure.groupId, batch)

    assertThat(service.outboundCursor(PEER_ID, GROUP_KEY)).isEqualTo(15L)
    assertThat(family.acknowledged).containsExactly(departure.groupId to LOCAL_ID)
    assertThat(family.updatedPeers).containsExactly(PEER_ID)
    val outboundLog = database.syncLogDao().getAllSyncLogs().first().single()
    assertThat(outboundLog.entitiesSent).isEqualTo(1)
    assertThat(outboundLog.syncType).isEqualTo("LAN")
  }

  @Test
  fun acknowledgedInclusiveBoundaryConvergesButStillSendsNewEntityAtSameTimestamp() = runTest {
    database.petDao().insertPet(bundle("pet-1").pets.single().toEntity())
    val first = service.prepareClinical(PEER_ID, GROUP_KEY).single()

    service.acknowledgeOutbound(PEER_ID, "Kitchen", GROUP_ID, first)

    assertThat(service.prepareClinical(PEER_ID, GROUP_KEY)).isEmpty()
    database.petDao().insertPet(bundle("pet-2").pets.single().toEntity())
    val boundaryAddition = service.prepareClinical(PEER_ID, GROUP_KEY)
    assertThat(boundaryAddition).hasSize(1)
    assertThat(boundaryAddition.single().bundle.pets.single().id).isEqualTo("pet-2")
  }

  @Test
  fun clockRollbackAndNewGroupCannotHideAnUnacknowledgedEntity() = runTest {
    database.petDao().insertPet(bundle("pet-1").pets.single().toEntity())
    val first = service.prepareClinical(PEER_ID, GROUP_KEY).single()
    service.acknowledgeOutbound(PEER_ID, "Kitchen", GROUP_ID, first)
    database.petDao().insertPet(bundle("late-write").pets.single().copy(updatedAt = 5L).toEntity())

    val clockRollback = service.prepareClinical(PEER_ID, GROUP_KEY).single()
    assertThat(clockRollback.bundle.pets.single().id).isEqualTo("late-write")
    service.acknowledgeOutbound(PEER_ID, "Kitchen", GROUP_ID, clockRollback)
    assertThat(service.outboundCursor(PEER_ID, GROUP_KEY)).isEqualTo(10L)

    val newGroupKey = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32) { 8 })
    assertThat(service.prepareClinical(PEER_ID, newGroupKey)).isNotEmpty()
  }

  private fun bundle(petId: String) =
    ExportBundle(
      metadata = ExportMetadata("1", "2026-07-18T00:00:00Z"),
      pets = listOf(Pet(id = petId, name = "Mimi", createdAt = 1L, updatedAt = 10L)),
      weightEntries = emptyList(),
      vaccinationEntries = emptyList(),
      dewormingEntries = emptyList(),
      tasks = emptyList(),
    )

  private fun createExportUseCase(context: Context, database: PetitDatabase) =
    ExportImportUseCase(
      context,
      database,
      PetRepositoryImpl(database.petDao()),
      WeightEntryRepositoryImpl(database.weightEntryDao()),
      VaccinationEntryRepositoryImpl(database.vaccinationEntryDao(), Clock.systemUTC()),
      DewormingEntryRepositoryImpl(database.dewormingEntryDao(), Clock.systemUTC()),
      TaskRepositoryImpl(database.taskDao()),
    )

  private class FakeFamilyGroupRepository : FamilyGroupRepository {
    override val familyGroupInfo: Flow<FamilyGroupInfo?> = MutableStateFlow(null)
    override val localDevice: Flow<FamilyGroupMember?> = MutableStateFlow(null)
    override val isSyncEnabled: Flow<Boolean> = MutableStateFlow(true)
    val pending = mutableListOf<PendingDeparture>()
    val acknowledged = mutableListOf<Pair<String, String>>()
    val updatedPeers = mutableListOf<String>()
    var reconcileFailures = 0
    var reconcileCalls = 0

    override suspend fun getFamilyGroupKey(): String = GROUP_KEY

    override suspend fun getOrCreateLocalDeviceId(): String = LOCAL_ID

    override suspend fun createFamilyGroup(deviceName: String): String = error("unused")

    override suspend fun joinFamilyGroup(familyGroupKey: String, deviceName: String) = Unit

    override suspend fun persistAuthorizedPairing(
      familyGroupKey: String,
      localMember: FamilyGroupMember,
      remoteMember: FamilyGroupMember,
    ) = Unit

    override suspend fun addRemoteMember(member: FamilyGroupMember) = Unit

    override suspend fun leaveFamilyGroup() = Unit

    override suspend fun removeMember(memberId: String) = Unit

    override suspend fun getPendingDepartures(): List<PendingDeparture> = pending.toList()

    override suspend fun acknowledgeDeparture(groupId: String, memberId: String) {
      acknowledged += groupId to memberId
    }

    override suspend fun applyMembershipChanges(changes: List<MembershipChange>) = Unit

    override suspend fun reconcileActiveAssociation() {
      reconcileCalls++
      if (reconcileFailures > 0) {
        reconcileFailures--
        error("simulated DataStore failure")
      }
    }

    override suspend fun updateLastSyncAt(memberId: String) {
      updatedPeers += memberId
    }

    override suspend fun setSyncEnabled(enabled: Boolean) = Unit

    override suspend fun recordSyncLog(syncLog: SyncLog) = Unit

    override fun getSyncLogs(): Flow<List<SyncLog>> = emptyFlow()

    override suspend fun getLatestSyncLog(): SyncLog? = null

    override suspend fun resetLocalPreferences() = Unit
  }

  private companion object {
    val GROUP_KEY: String =
      Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32) { 7 })
    const val LOCAL_ID = "00000000-0000-0000-0000-000000000001"
    const val PEER_ID = "00000000-0000-0000-0000-000000000002"
    val GROUP_ID: String = MembershipChange.groupIdForKey(GROUP_KEY)
  }
}
