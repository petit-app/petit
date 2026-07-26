package com.woliveiras.petit.domain.transfer

import com.woliveiras.petit.domain.model.ExportBundle
import org.json.JSONObject

/** Exact JSON byte boundary shared by Nearby send and receive paths. */
internal object NearbyTransferPayloadCodec {
  fun encode(bundle: ExportBundle): ByteArray =
    bundle.toJson().toString().toByteArray(Charsets.UTF_8)

  fun decode(data: String): ExportBundle = ExportBundle.fromJson(JSONObject(data))
}
