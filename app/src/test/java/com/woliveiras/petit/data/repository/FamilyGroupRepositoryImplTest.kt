package com.woliveiras.petit.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.local.entity.PetEntity
import com.woliveiras.petit.domain.model.FamilyGroupMember
import com.woliveiras.petit.domain.model.MembershipChange
import com.woliveiras.petit.domain.model.MembershipChangeType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FamilyGroupRepositoryImplTest {
  private lateinit var database: PetitDatabase
  private lateinit var repository: FamilyGroupRepositoryImpl

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database =
      Room.inMemoryDatabaseBuilder(context, PetitDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    repository =
      FamilyGroupRepositoryImpl(
        context,
        database.familyGroupMemberDao(),
        database.syncLogDao(),
        database,
      )
    runBlocking { repository.resetLocalPreferences() }
  }

  @After
  fun tearDown() {
    runBlocking { repository.resetLocalPreferences() }
    database.close()
  }

  @Test
  fun renamePersistsTheNameWithoutChangingStableIdentityOrGroupKey() = runTest {
    persistPair("local-id", "Old name", "remote-id", "Peer")

    repository.renameLocalDevice("Kitchen tablet")

    val local = repository.localDevice.first()
    assertThat(local?.id).isEqualTo("local-id")
    assertThat(local?.familyGroupKey).isEqualTo("group-key")
    assertThat(local?.deviceName).isEqualTo("Kitchen tablet")
    assertThat(repository.getFamilyGroupKey()).isEqualTo("group-key")
    assertThat(repository.getMembershipChanges().single().type)
      .isEqualTo(MembershipChangeType.RENAME)
  }

  @Test
  fun repeatedRemovalProducesOneEffectiveRevocationAndRejectsTheOldIdentity() = runTest {
    persistPair("local-id", "This device", "remote-id", "Peer")

    repository.removeMember("remote-id")
    repository.removeMember("remote-id")

    assertThat(repository.isMemberAuthorized("remote-id", "group-key")).isFalse()
    assertThat(repository.getMembershipChanges().filter { it.memberId == "remote-id" })
      .containsExactly(repository.getMembershipChanges().single { it.memberId == "remote-id" })
    assertThat(repository.getMembershipChanges().single().type)
      .isEqualTo(MembershipChangeType.REMOVE)
  }

  @Test
  fun authorizedPairingCannotResurrectARemovedIdentity() = runTest {
    persistPair("local-id", "This device", "remote-id", "Peer")
    repository.removeMember("remote-id")

    val result = runCatching {
      repository.persistAuthorizedPairing(
        familyGroupKey = "group-key",
        localMember = member("local-id", "This device", isLocal = true),
        remoteMember = member("remote-id", "Peer again", isLocal = false),
      )
    }

    assertThat(result.exceptionOrNull()).isInstanceOf(SecurityException::class.java)
    assertThat(repository.isMemberAuthorized("remote-id", "group-key")).isFalse()
  }

  @Test
  fun removedStableIdentityCanPairInANewGroupWithoutRegainingOldKey() = runTest {
    persistPair("local-id", "This device", "remote-id", "Peer")
    repository.removeMember("remote-id")

    repository.persistAuthorizedPairing(
      familyGroupKey = "new-group-key",
      localMember = member("local-id", "This device", isLocal = true, key = "new-group-key"),
      remoteMember = member("remote-id", "Peer", isLocal = false, key = "new-group-key"),
    )

    assertThat(repository.isMemberAuthorized("remote-id", "new-group-key")).isTrue()
    assertThat(repository.isMemberAuthorized("remote-id", "group-key")).isFalse()
  }

  @Test
  fun leaveIsIdempotentAndPreservesPetDataWhileQueuingDeparture() = runTest {
    persistPair("local-id", "This device", "remote-id", "Peer")
    database.petDao().insertPet(PetEntity(id = "pet-1", name = "Mimi"))

    repository.leaveFamilyGroup()
    repository.leaveFamilyGroup()

    assertThat(repository.getFamilyGroupKey()).isNull()
    assertThat(repository.familyGroupInfo.first()).isNull()
    assertThat(database.petDao().getPetById("pet-1")).isNotNull()
    assertThat(
        database.familyGroupMemberDao().getAllByGroupKeyIncludingDeleted("group-key").all {
          it.deletedAt != null
        }
      )
      .isTrue()
    assertThat(repository.getMembershipChanges().single().type)
      .isEqualTo(MembershipChangeType.LEAVE)
    val departure = repository.getPendingDepartures().single()
    assertThat(departure.change.memberId).isEqualTo("local-id")
    assertThat(departure.deliveryKey).isEqualTo("group-key")

    repository.acknowledgeDeparture(departure.change.groupId, departure.change.memberId)

    assertThat(repository.getPendingDepartures()).isEmpty()
    assertThat(repository.getMembershipChanges().single().type)
      .isEqualTo(MembershipChangeType.LEAVE)
  }

  @Test
  fun incomingMembershipChangeDoesNotErasePendingDepartureCredential() = runTest {
    persistPair("local-id", "This device", "remote-id", "Peer")
    repository.leaveFamilyGroup()
    val pending = repository.getPendingDepartures().single()

    repository.applyMembershipChanges(
      listOf(
        MembershipChange(
          groupId = pending.change.groupId,
          memberId = "remote-id",
          type = MembershipChangeType.REMOVE,
          timestamp = Long.MAX_VALUE,
        )
      )
    )

    assertThat(repository.getPendingDepartures().single().deliveryKey).isEqualTo("group-key")
  }

  @Test
  fun unknownGroupMembershipEventIsNotRetained() = runTest {
    persistPair("local-id", "This device", "remote-id", "Peer")

    repository.applyMembershipChanges(
      listOf(
        MembershipChange(
          groupId = "b".repeat(64),
          memberId = "remote-id",
          type = MembershipChangeType.REMOVE,
          timestamp = Long.MAX_VALUE,
        )
      )
    )

    assertThat(repository.getMembershipChanges()).isEmpty()
    assertThat(repository.isMemberAuthorized("remote-id", "group-key")).isTrue()
  }

  @Test
  fun incomingChangesConvergeIdempotentlyAndSelfRevocationClearsOnlyAssociation() = runTest {
    persistPair("local-id", "This device", "remote-id", "Old peer")
    database.petDao().insertPet(PetEntity(id = "pet-1", name = "Mimi"))
    val rename =
      MembershipChange(
        groupId = MembershipChange.groupIdForKey("group-key"),
        memberId = "remote-id",
        type = MembershipChangeType.RENAME,
        deviceName = "New peer",
        timestamp = 20L,
      )
    val revokeLocal =
      MembershipChange(
        groupId = MembershipChange.groupIdForKey("group-key"),
        memberId = "local-id",
        type = MembershipChangeType.REMOVE,
        timestamp = Long.MAX_VALUE,
      )

    repository.applyMembershipChanges(listOf(rename, rename))
    assertThat(
        repository.familyGroupInfo.first()?.members?.single { it.id == "remote-id" }?.deviceName
      )
      .isEqualTo("New peer")
    repository.applyMembershipChanges(listOf(revokeLocal, revokeLocal))

    assertThat(repository.getFamilyGroupKey()).isNull()
    assertThat(database.petDao().getPetById("pet-1")).isNotNull()
  }

  @Test
  fun staleVisibleAssociationRecoversAfterRoomCommit() = runTest {
    persistPair("local-id", "This device", "remote-id", "Peer")
    database.familyGroupMemberDao().softDeleteAllByGroupKey("group-key", Long.MAX_VALUE)

    assertThat(repository.getFamilyGroupKey()).isNull()
    assertThat(repository.familyGroupInfo.first()).isNull()
  }

  @Test
  fun destructiveMembershipChangeIsTerminalAgainstALaterRename() {
    val groupId = MembershipChange.groupIdForKey("group-key")
    val removed =
      MembershipChange(groupId, "member-1", MembershipChangeType.REMOVE, timestamp = 20L)
    val laterRename =
      MembershipChange(
        groupId,
        "member-1",
        MembershipChangeType.RENAME,
        deviceName = "Attempted return",
        timestamp = 30L,
      )

    assertThat(MembershipChange.normalize(listOf(removed, laterRename))).containsExactly(removed)
    assertThat(MembershipChange.normalize(listOf(laterRename, removed))).containsExactly(removed)
  }

  private suspend fun persistPair(
    localId: String,
    localName: String,
    remoteId: String,
    remoteName: String,
  ) {
    repository.persistAuthorizedPairing(
      familyGroupKey = "group-key",
      localMember = member(localId, localName, isLocal = true),
      remoteMember = member(remoteId, remoteName, isLocal = false),
    )
  }

  private fun member(id: String, name: String, isLocal: Boolean, key: String = "group-key") =
    FamilyGroupMember(
      id = id,
      deviceName = name,
      familyGroupKey = key,
      isLocalDevice = isLocal,
      lastSyncAt = null,
      createdAt = 1L,
      updatedAt = 1L,
    )
}
