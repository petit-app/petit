package com.woliveiras.petit.data.lan

import com.woliveiras.petit.data.local.db.PetitDatabase
import com.woliveiras.petit.data.repository.FamilyGroupRepository
import com.woliveiras.petit.domain.lan.LanHandshakeClient
import com.woliveiras.petit.domain.lan.LanHandshakeResult
import com.woliveiras.petit.domain.lan.LanHandshakeServer
import com.woliveiras.petit.domain.lan.LanMessage
import com.woliveiras.petit.domain.lan.LanProtocolError
import com.woliveiras.petit.domain.lan.LanProtocolException
import com.woliveiras.petit.domain.lan.LanSecureChannel
import com.woliveiras.petit.domain.lan.LanSessionDirection
import com.woliveiras.petit.domain.lan.LanSessionScope
import com.woliveiras.petit.domain.model.MembershipChange
import com.woliveiras.petit.domain.model.PendingDeparture
import javax.inject.Inject
import javax.inject.Singleton

data class LanSessionResult(
  val batchesSent: Int,
  val batchesReceived: Int,
  val authenticated: Boolean = true,
)

data class LanSessionCredential(
  val groupKey: String,
  val scope: LanSessionScope,
  val departure: PendingDeparture? = null,
)

/** Executes the same authenticated, bidirectional state machine for foreground and worker runs. */
@Singleton
class LanSessionRunner
@Inject
constructor(
  private val database: PetitDatabase,
  private val familyGroupRepository: FamilyGroupRepository,
  private val changesets: LanChangesetService,
) {
  private val nonceRegistry = RoomLanNonceRegistry(database.lanSyncDao())

  suspend fun runClient(peer: NsdResolvedPeer, credential: LanSessionCredential): LanSessionResult {
    val scope = credential.scope
    val identity = localIdentity(credential.groupKey)
    if (
      scope == LanSessionScope.CLINICAL &&
        !LanSessionDirection.shouldInitiate(identity.deviceId, peer.deviceId)
    ) {
      return LanSessionResult(0, 0, authenticated = false)
    }
    val cursor = changesets.outboundCursor(peer.deviceId, credential.groupKey)
    val handshake =
      LanHandshakeClient(
        localDeviceId = identity.deviceId,
        expectedServerDeviceId = peer.deviceId,
        groupKey = identity.groupKey,
        scope = scope,
      )
    LanSocketConnection.connect(peer.hostAddress, peer.port).use { connection ->
      connection.sendPlain(handshake.createHello(cursor))
      val response = connection.receivePlain()
      if (response is LanMessage.Error) {
        throw LanProtocolException(response.error, response.detail)
      }
      val ack =
        response as? LanMessage.HelloAck
          ?: throw LanProtocolException(LanProtocolError.INVALID_SEQUENCE, "Expected HELLO_ACK")
      val channel = handshake.accept(ack)
      val outbound =
        when (scope) {
          LanSessionScope.CLINICAL -> changesets.prepareClinical(peer.deviceId, credential.groupKey)
          LanSessionScope.MEMBERSHIP_ONLY ->
            changesets.prepareDeparture(
              peer.deviceId,
              credential.departure
                ?: throw LanProtocolException(
                  LanProtocolError.AUTHENTICATION_FAILED,
                  "Missing departure for membership-only session",
                ),
            )
        }
      val result =
        runProtected(connection, channel) {
          sendBatches(connection, channel, peer.deviceId, peer.serviceName, credential, outbound)
          val received =
            receiveBatches(
              connection,
              channel,
              peer.deviceId,
              peer.serviceName,
              scope,
              credential.groupKey,
            )
          LanSessionResult(outbound.size, received)
        }
      familyGroupRepository.updateLastSyncAt(peer.deviceId)
      return result
    }
  }

  suspend fun runServer(
    connection: LanSocketConnection,
    credential: LanSessionCredential,
  ): LanSessionResult {
    val hello =
      connection.receivePlain() as? LanMessage.Hello
        ?: throw LanProtocolException(LanProtocolError.INVALID_SEQUENCE, "Expected HELLO")
    if (
      credential.scope == LanSessionScope.MEMBERSHIP_ONLY &&
        hello.scope != LanSessionScope.MEMBERSHIP_ONLY
    ) {
      connection.sendPlain(
        LanMessage.Error(LanProtocolError.AUTHENTICATION_FAILED, "Unexpected session scope")
      )
      connection.sendPlain(LanMessage.Close("handshake rejected"))
      return LanSessionResult(0, 0, authenticated = false)
    }
    val identity = localIdentity(credential.groupKey)
    val members =
      database.familyGroupMemberDao().getAllByGroupKeyIncludingDeleted(identity.groupKey)
    val server =
      LanHandshakeServer(
        localDeviceId = identity.deviceId,
        allowedMemberIds = members.filter { it.deletedAt == null }.map { it.id }.toSet(),
        membershipOnlyMemberIds = members.map { it.id }.toSet(),
        groupKey = identity.groupKey,
        nonceRegistry = nonceRegistry,
      )
    return when (
      val result =
        server.accept(hello, changesets.outboundCursor(hello.deviceId, credential.groupKey))
    ) {
      is LanHandshakeResult.Rejected -> {
        connection.sendPlain(result.response)
        connection.sendPlain(result.close)
        LanSessionResult(0, 0, authenticated = false)
      }
      is LanHandshakeResult.Accepted -> {
        connection.sendPlain(result.ack)
        val peerName = members.firstOrNull { it.id == hello.deviceId }?.deviceName ?: hello.deviceId
        val sessionResult =
          runProtected(connection, result.channel) {
            val received =
              receiveBatches(
                connection,
                result.channel,
                hello.deviceId,
                peerName,
                hello.scope,
                credential.groupKey,
              )
            val outbound =
              when (hello.scope) {
                LanSessionScope.CLINICAL ->
                  changesets.prepareClinical(hello.deviceId, credential.groupKey)
                LanSessionScope.MEMBERSHIP_ONLY -> emptyList()
              }
            sendBatches(connection, result.channel, hello.deviceId, peerName, credential, outbound)
            LanSessionResult(outbound.size, received)
          }
        familyGroupRepository.updateLastSyncAt(hello.deviceId)
        sessionResult
      }
    }
  }

  private suspend fun <T> runProtected(
    connection: LanSocketConnection,
    channel: LanSecureChannel,
    block: suspend () -> T,
  ): T {
    return try {
      block()
    } catch (exception: LanProtocolException) {
      runCatching {
        connection.sendProtected(
          channel.seal(LanMessage.Error(exception.error, exception.message.orEmpty()))
        )
        connection.sendProtected(channel.seal(LanMessage.Close("protocol error")))
      }
      throw exception
    }
  }

  private suspend fun sendBatches(
    connection: LanSocketConnection,
    channel: LanSecureChannel,
    peerId: String,
    peerName: String,
    credential: LanSessionCredential,
    batches: List<com.woliveiras.petit.domain.lan.PreparedLanChangeset>,
  ) {
    for (batch in batches) {
      connection.sendProtected(
        channel.seal(LanMessage.Changeset(batch.batchId, batch.cursor, batch.payload))
      )
      val ack = channel.open(connection.receiveProtected())
      if (
        ack !is LanMessage.Ack ||
          ack.batchId != batch.batchId ||
          ack.newSyncTimestamp != batch.cursor
      ) {
        throw LanProtocolException(LanProtocolError.INVALID_SEQUENCE, "Invalid changeset ACK")
      }
      changesets.acknowledgeOutbound(
        peerId,
        peerName,
        MembershipChange.groupIdForKey(credential.groupKey),
        batch,
      )
    }
    connection.sendProtected(channel.seal(LanMessage.Close(OUTBOUND_COMPLETE)))
  }

  private suspend fun receiveBatches(
    connection: LanSocketConnection,
    channel: LanSecureChannel,
    peerId: String,
    peerName: String,
    scope: LanSessionScope,
    groupKey: String,
  ): Int {
    var received = 0
    while (true) {
      when (val message = channel.open(connection.receiveProtected())) {
        is LanMessage.Changeset -> {
          val applied =
            changesets.apply(
              peerId = peerId,
              peerName = peerName,
              batchId = message.batchId,
              cursor = message.sinceTimestamp,
              payload = message.payload,
              scope = scope,
              groupId = MembershipChange.groupIdForKey(groupKey),
            )
          connection.sendProtected(
            channel.seal(LanMessage.Ack(message.batchId, applied.acknowledgedCursor))
          )
          if (!applied.replayed) received++
        }
        is LanMessage.Close -> {
          if (message.reason != OUTBOUND_COMPLETE) {
            throw LanProtocolException(LanProtocolError.INVALID_SEQUENCE, "Unexpected CLOSE")
          }
          return received
        }
        else ->
          throw LanProtocolException(
            LanProtocolError.INVALID_SEQUENCE,
            "Expected CHANGESET or CLOSE",
          )
      }
    }
  }

  private suspend fun localIdentity(groupKey: String) =
    LocalIdentity(familyGroupRepository.getOrCreateLocalDeviceId(), groupKey)

  private data class LocalIdentity(val deviceId: String, val groupKey: String)

  private companion object {
    const val OUTBOUND_COMPLETE = "OUTBOUND_COMPLETE"
  }
}
