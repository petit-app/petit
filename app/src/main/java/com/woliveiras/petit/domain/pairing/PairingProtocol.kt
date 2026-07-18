package com.woliveiras.petit.domain.pairing

import org.json.JSONObject

private const val MAX_DEVICE_NAME_LENGTH = 80
private const val MAX_DEVICE_ID_LENGTH = 128
private const val MAX_GROUP_KEY_LENGTH = 512

sealed interface PairingMessage {
  data class AuthorizationRequest(val code: String, val deviceId: String, val deviceName: String) :
    PairingMessage

  data class AuthorizationAccepted(
    val familyGroupKey: String,
    val deviceId: String,
    val deviceName: String,
  ) : PairingMessage

  data class AuthorizationRejected(val reason: PairingRejectionReason) : PairingMessage
}

enum class PairingRejectionReason {
  IncorrectCode,
  CodeExpired,
  MalformedRequest,
  RevokedMember,
}

object PairingProtocol {
  private const val REQUEST = "AUTH_REQUEST"
  private const val ACCEPTED = "AUTH_ACCEPTED"
  private const val REJECTED = "AUTH_REJECTED"

  fun encode(message: PairingMessage): String =
    JSONObject()
      .apply {
        when (message) {
          is PairingMessage.AuthorizationRequest -> {
            put("type", REQUEST)
            put("code", message.code)
            put("deviceId", message.deviceId)
            put("deviceName", message.deviceName)
          }
          is PairingMessage.AuthorizationAccepted -> {
            put("type", ACCEPTED)
            put("familyGroupKey", message.familyGroupKey)
            put("deviceId", message.deviceId)
            put("deviceName", message.deviceName)
          }
          is PairingMessage.AuthorizationRejected -> {
            put("type", REJECTED)
            put("reason", message.reason.name)
          }
        }
      }
      .toString()

  fun decode(encoded: String): PairingMessage? =
    runCatching {
        val json = JSONObject(encoded)
        when (json.getString("type")) {
          REQUEST ->
            PairingMessage.AuthorizationRequest(
                code = json.getString("code"),
                deviceId = json.getString("deviceId"),
                deviceName = json.getString("deviceName"),
              )
              .takeIf {
                it.deviceId.isNotBlank() &&
                  it.deviceId.length <= MAX_DEVICE_ID_LENGTH &&
                  it.deviceName.isNotBlank() &&
                  it.deviceName.length <= MAX_DEVICE_NAME_LENGTH
              }
          ACCEPTED ->
            PairingMessage.AuthorizationAccepted(
                familyGroupKey = json.getString("familyGroupKey"),
                deviceId = json.getString("deviceId"),
                deviceName = json.getString("deviceName"),
              )
              .takeIf {
                it.familyGroupKey.isNotBlank() &&
                  it.familyGroupKey.length <= MAX_GROUP_KEY_LENGTH &&
                  it.deviceId.isNotBlank() &&
                  it.deviceId.length <= MAX_DEVICE_ID_LENGTH &&
                  it.deviceName.isNotBlank() &&
                  it.deviceName.length <= MAX_DEVICE_NAME_LENGTH
              }
          REJECTED ->
            PairingMessage.AuthorizationRejected(
              PairingRejectionReason.valueOf(json.getString("reason"))
            )
          else -> null
        }
      }
      .getOrNull()
}

data class PairingAuthorizationSession(
  val pairingCode: PairingCode,
  val familyGroupKey: String,
  val localDeviceId: String,
  val localDeviceName: String,
  val revokedMemberIds: Set<String> = emptySet(),
) {
  fun authorize(
    request: PairingMessage.AuthorizationRequest,
    nowMillis: Long,
  ): PairingAuthorizationResult =
    when (pairingCode.validate(request.code, nowMillis)) {
      PairingCodeValidation.Valid ->
        if (request.deviceId in revokedMemberIds) {
          PairingAuthorizationResult.Rejected(PairingRejectionReason.RevokedMember)
        } else {
          PairingAuthorizationResult.Accepted(
            response =
              PairingMessage.AuthorizationAccepted(
                familyGroupKey = familyGroupKey,
                deviceId = localDeviceId,
                deviceName = localDeviceName,
              ),
            remoteDeviceId = request.deviceId,
            remoteDeviceName = request.deviceName,
          )
        }
      PairingCodeValidation.Expired ->
        PairingAuthorizationResult.Rejected(PairingRejectionReason.CodeExpired)
      PairingCodeValidation.Incorrect ->
        PairingAuthorizationResult.Rejected(PairingRejectionReason.IncorrectCode)
      PairingCodeValidation.Malformed ->
        PairingAuthorizationResult.Rejected(PairingRejectionReason.MalformedRequest)
    }
}

sealed interface PairingAuthorizationResult {
  data class Accepted(
    val response: PairingMessage.AuthorizationAccepted,
    val remoteDeviceId: String,
    val remoteDeviceName: String,
  ) : PairingAuthorizationResult

  data class Rejected(val reason: PairingRejectionReason) : PairingAuthorizationResult
}
