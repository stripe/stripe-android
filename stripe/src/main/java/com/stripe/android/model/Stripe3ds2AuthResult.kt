package com.stripe.android.model

import com.stripe.android.model.StripeJsonUtils.optString
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal data class Stripe3ds2AuthResult internal constructor(
    val id: String?,
    private val objectType: String?,
    val ares: Ares? = null,
    val created: Long?,
    val source: String?,
    val state: String? = null,
    private val liveMode: Boolean = false,
    val error: ThreeDS2Error? = null,
    val fallbackRedirectUrl: String? = null
) {
    internal data class Ares internal constructor(
        internal val threeDSServerTransId: String?,
        private val acsChallengeMandated: String?,
        internal val acsSignedContent: String? = null,
        internal val acsTransId: String?,
        private val acsUrl: String? = null,
        private val authenticationType: String? = null,
        private val cardholderInfo: String? = null,
        private val messageExtension: List<MessageExtension>? = null,
        private val messageType: String?,
        private val messageVersion: String?,
        private val sdkTransId: String?,
        private val transStatus: String? = null
    ) {
        val isChallenge = VALUE_CHALLENGE == transStatus

        internal companion object {
            private const val FIELD_ACS_CHALLENGE_MANDATED = "acsChallengeMandated"
            private const val FIELD_ACS_SIGNED_CONTENT = "acsSignedContent"
            private const val FIELD_ACS_TRANS_ID = "acsTransID"
            private const val FIELD_ACS_URL = "acsURL"
            private const val FIELD_AUTHENTICATION_TYPE = "authenticationType"
            private const val FIELD_CARDHOLDER_INFO = "cardholderInfo"
            private const val FIELD_MESSAGE_EXTENSION = "messageExtension"
            private const val FIELD_MESSAGE_TYPE = "messageType"
            private const val FIELD_MESSAGE_VERSION = "messageVersion"
            private const val FIELD_SDK_TRANS_ID = "sdkTransID"
            private const val FIELD_TRANS_STATUS = "transStatus"
            private const val FIELD_THREE_DS_SERVER_TRANS_ID = "threeDSServerTransID"

            internal const val VALUE_CHALLENGE = "C"

            @JvmSynthetic
            @Throws(JSONException::class)
            internal fun fromJson(aresJson: JSONObject?): Ares? {
                return if (aresJson == null) {
                    null
                } else {
                    Ares(
                        threeDSServerTransId = aresJson.getString(FIELD_THREE_DS_SERVER_TRANS_ID),
                        acsChallengeMandated = optString(aresJson, FIELD_ACS_CHALLENGE_MANDATED),
                        acsSignedContent = optString(aresJson, FIELD_ACS_SIGNED_CONTENT),
                        acsTransId = aresJson.getString(FIELD_ACS_TRANS_ID),
                        acsUrl = optString(aresJson, FIELD_ACS_URL),
                        authenticationType = optString(aresJson, FIELD_AUTHENTICATION_TYPE),
                        cardholderInfo = optString(aresJson, FIELD_CARDHOLDER_INFO),
                        messageType = aresJson.getString(FIELD_MESSAGE_TYPE),
                        messageVersion = aresJson.getString(FIELD_MESSAGE_VERSION),
                        sdkTransId = optString(aresJson, FIELD_SDK_TRANS_ID),
                        transStatus = optString(aresJson, FIELD_TRANS_STATUS),
                        messageExtension = MessageExtension.fromJson(
                            aresJson.optJSONArray(FIELD_MESSAGE_EXTENSION)
                        )
                    )
                }
            }
        }
    }

    internal data class MessageExtension internal constructor(
        // The name of the extension data set as defined by the extension owner.
        val name: String?,

        // A boolean value indicating whether the recipient must understand the contents of the
        // extension to interpret the entire message.
        private val criticalityIndicator: Boolean,

        // A unique identifier for the extension.
        // Note: Payment System Registered Application Provider Identifier (RID) is required as
        // prefix of the ID.
        val id: String?,

        // The data carried in the extension.
        val data: Map<String, String>?
    ) {
        internal companion object {
            private const val FIELD_NAME = "name"
            private const val FIELD_ID = "id"
            private const val FIELD_CRITICALITY_INDICATOR = "criticalityIndicator"
            private const val FIELD_DATA = "data"

            @JvmSynthetic
            @Throws(JSONException::class)
            internal fun fromJson(messageExtensionsJson: JSONArray?): List<MessageExtension>? {
                if (messageExtensionsJson == null) {
                    return null
                }

                return (0 until messageExtensionsJson.length())
                    .mapNotNull { messageExtensionsJson.optJSONObject(it) }
                    .map { fromJson(it) }
            }

            @Throws(JSONException::class)
            private fun fromJson(json: JSONObject): MessageExtension {
                val dataJson = json.optJSONObject(FIELD_DATA)
                val data = if (dataJson != null) {
                    val keys = dataJson.names() ?: JSONArray()
                    (0 until keys.length())
                        .map { idx -> keys.getString(idx) }
                        .map { key -> mapOf(key to dataJson.getString(key)) }
                        .fold(emptyMap<String, String>()) { acc, map -> acc.plus(map) }
                } else {
                    emptyMap()
                }

                return MessageExtension(
                    name = optString(json, FIELD_NAME),
                    criticalityIndicator = json.optBoolean(FIELD_CRITICALITY_INDICATOR),
                    id = optString(json, FIELD_ID),
                    data = data.toMap()
                )
            }
        }
    }

    data class ThreeDS2Error internal constructor(
        val threeDSServerTransId: String?,
        val acsTransId: String?,
        val dsTransId: String?,
        val errorCode: String?,
        val errorComponent: String?,
        val errorDescription: String?,
        val errorDetail: String? = null,
        val errorMessageType: String?,
        val messageType: String?,
        val messageVersion: String?,
        val sdkTransId: String?
    ) {
        internal companion object {
            private const val FIELD_THREE_DS_SERVER_TRANS_ID = "threeDSServerTransID"
            private const val FIELD_ACS_TRANS_ID = "acsTransID"
            private const val FIELD_DS_TRANS_ID = "dsTransID"
            private const val FIELD_ERROR_CODE = "errorCode"
            private const val FIELD_ERROR_COMPONENT = "errorComponent"
            private const val FIELD_ERROR_DESCRIPTION = "errorDescription"
            private const val FIELD_ERROR_DETAIL = "errorDetail"
            private const val FIELD_ERROR_MESSAGE_TYPE = "errorMessageType"
            private const val FIELD_MESSAGE_TYPE = "messageType"
            private const val FIELD_MESSAGE_VERSION = "messageVersion"
            private const val FIELD_SDK_TRANS_ID = "sdkTransID"

            @JvmSynthetic
            @Throws(JSONException::class)
            internal fun fromJson(errorJson: JSONObject?): ThreeDS2Error? {
                if (errorJson == null) {
                    return null
                } else {
                    return ThreeDS2Error(
                        threeDSServerTransId = errorJson.getString(FIELD_THREE_DS_SERVER_TRANS_ID),
                        acsTransId = optString(errorJson, FIELD_ACS_TRANS_ID),
                        dsTransId = optString(errorJson, FIELD_DS_TRANS_ID),
                        errorCode = errorJson.getString(FIELD_ERROR_CODE),
                        errorComponent = errorJson.getString(FIELD_ERROR_COMPONENT),
                        errorDescription = errorJson.getString(FIELD_ERROR_DESCRIPTION),
                        errorDetail = errorJson.getString(FIELD_ERROR_DETAIL),
                        errorMessageType = optString(errorJson, FIELD_ERROR_MESSAGE_TYPE),
                        messageType = errorJson.getString(FIELD_MESSAGE_TYPE),
                        messageVersion = errorJson.getString(FIELD_MESSAGE_VERSION),
                        sdkTransId = optString(errorJson, FIELD_SDK_TRANS_ID)
                    )
                }
            }
        }
    }

    internal companion object {
        private const val FIELD_ID = "id"
        private const val FIELD_OBJECT = "object"
        private const val FIELD_ARES = "ares"
        private const val FIELD_CREATED = "created"
        private const val FIELD_ERROR = "error"
        private const val FIELD_FALLBACK_REDIRECT_URL = "fallback_redirect_url"
        private const val FIELD_LIVEMODE = "livemode"
        private const val FIELD_SOURCE = "source"
        private const val FIELD_STATE = "state"

        @JvmSynthetic
        @Throws(JSONException::class)
        internal fun fromJson(authResultJson: JSONObject): Stripe3ds2AuthResult {
            val ares =
                if (authResultJson.isNull(FIELD_ARES)) {
                    null
                } else {
                    Ares.fromJson(authResultJson.optJSONObject(FIELD_ARES))
                }
            val error =
                if (authResultJson.isNull(FIELD_ERROR)) {
                    null
                } else {
                    ThreeDS2Error.fromJson(authResultJson.optJSONObject(FIELD_ERROR))
                }
            val fallbackRedirectUrl =
                if (authResultJson.isNull(FIELD_FALLBACK_REDIRECT_URL)) {
                    null
                } else {
                    authResultJson.optString(FIELD_FALLBACK_REDIRECT_URL)
                }
            return Stripe3ds2AuthResult(
                id = authResultJson.getString(FIELD_ID),
                objectType = authResultJson.getString(FIELD_OBJECT),
                created = authResultJson.getLong(FIELD_CREATED),
                liveMode = authResultJson.getBoolean(FIELD_LIVEMODE),
                source = authResultJson.getString(FIELD_SOURCE),
                state = authResultJson.optString(FIELD_STATE),
                ares = ares,
                error = error,
                fallbackRedirectUrl = fallbackRedirectUrl
            )
        }
    }
}
