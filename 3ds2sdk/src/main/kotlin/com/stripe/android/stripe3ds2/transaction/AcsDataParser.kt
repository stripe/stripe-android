package com.stripe.android.stripe3ds2.transaction

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.util.JSONObjectUtils
import com.stripe.android.stripe3ds2.observability.ErrorReporter
import org.json.JSONException
import org.json.JSONObject
import java.security.interfaces.ECPublicKey
import java.text.ParseException

internal fun interface AcsDataParser {
    fun parse(payloadJson: JSONObject): AcsData
}

internal class DefaultAcsDataParser(
    private val errorReporter: ErrorReporter
) : AcsDataParser {
    @Throws(JSONException::class, ParseException::class, JOSEException::class)
    override fun parse(payloadJson: JSONObject): AcsData {
        return runCatching {
            val payload = JSONObjectUtils.parse(payloadJson.toString()).toMap()
            AcsData(
                acsUrl = payload[FIELD_ACS_URL].toString(),
                acsEphemPubKey = parsePublicKey(payload[FIELD_ACS_EPHEM_PUB_KEY]),
                sdkEphemPubKey = parsePublicKey(payload[FIELD_SDK_EPHEM_PUB_KEY])
            )
        }.onFailure {
            errorReporter.reportError(
                IllegalArgumentException("Failed to parse ACS data: $payloadJson", it)
            )
        }.getOrThrow()
    }

    @Suppress("UNCHECKED_CAST")
    private fun parsePublicKey(ephemPubkey: Any?): ECPublicKey {
        return when (ephemPubkey) {
            is Map<*, *> -> ECKey.parse(ephemPubkey as Map<String, Any>)
            else -> ECKey.parse(ephemPubkey?.toString().orEmpty())
        }.toECPublicKey()
    }

    internal companion object {
        internal const val FIELD_ACS_URL = "acsURL"
        internal const val FIELD_ACS_EPHEM_PUB_KEY = "acsEphemPubKey"
        internal const val FIELD_SDK_EPHEM_PUB_KEY = "sdkEphemPubKey"
    }
}
