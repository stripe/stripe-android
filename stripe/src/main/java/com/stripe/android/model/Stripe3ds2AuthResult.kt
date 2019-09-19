package com.stripe.android.model

import com.stripe.android.ObjectBuilder
import com.stripe.android.model.StripeJsonUtils.optString
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal data class Stripe3ds2AuthResult constructor(
    val id: String?,
    private val objectType: String?,
    val ares: Ares?,
    val created: Long?,
    val source: String?,
    val state: String?,
    private val liveMode: Boolean,
    val error: ThreeDS2Error?,
    val fallbackRedirectUrl: String?
) {
    internal class Builder : ObjectBuilder<Stripe3ds2AuthResult> {
        private var id: String? = null
        private var objectType: String? = null
        private var ares: Ares? = null
        private var created: Long? = null
        private var source: String? = null
        private var state: String? = null
        private var liveMode: Boolean = false
        private var error: ThreeDS2Error? = null
        private var fallbackRedirectUrl: String? = null

        fun setId(id: String): Builder {
            this.id = id
            return this
        }

        fun setObjectType(objectType: String): Builder {
            this.objectType = objectType
            return this
        }

        fun setAres(ares: Ares?): Builder {
            this.ares = ares
            return this
        }

        fun setCreated(created: Long): Builder {
            this.created = created
            return this
        }

        fun setSource(source: String): Builder {
            this.source = source
            return this
        }

        fun setState(state: String?): Builder {
            this.state = state
            return this
        }

        fun setLiveMode(liveMode: Boolean): Builder {
            this.liveMode = liveMode
            return this
        }

        fun setError(error: ThreeDS2Error?): Builder {
            this.error = error
            return this
        }

        fun setFallbackRedirectUrl(fallbackRedirectUrl: String?): Builder {
            this.fallbackRedirectUrl = fallbackRedirectUrl
            return this
        }

        override fun build(): Stripe3ds2AuthResult {
            return Stripe3ds2AuthResult(id, objectType, ares, created, source, state,
                liveMode, error, fallbackRedirectUrl)
        }
    }

    data class Ares constructor(
        val threeDSServerTransId: String?,
        private val acsChallengeMandated: String?,
        val acsSignedContent: String?,
        val acsTransId: String?,
        private val acsUrl: String?,
        private val authenticationType: String?,
        private val cardholderInfo: String?,
        private val messageExtension: List<MessageExtension>?,
        private val messageType: String?,
        private val messageVersion: String?,
        private val sdkTransId: String?,
        private val transStatus: String?
    ) {
        val isChallenge = VALUE_CHALLENGE == transStatus

        internal class Builder : ObjectBuilder<Ares> {
            private var threeDSServerTransId: String? = null
            private var acsChallengeMandated: String? = null
            private var acsSignedContent: String? = null
            private var acsTransId: String? = null
            private var acsUrl: String? = null
            private var authenticationType: String? = null
            private var cardholderInfo: String? = null
            private var messageExtension: List<MessageExtension>? = null
            private var messageType: String? = null
            private var messageVersion: String? = null
            private var sdkTransId: String? = null
            private var transStatus: String? = null

            fun setThreeDSServerTransId(threeDSServerTransId: String?): Builder {
                this.threeDSServerTransId = threeDSServerTransId
                return this
            }

            fun setAcsChallengeMandated(acsChallengeMandated: String?): Builder {
                this.acsChallengeMandated = acsChallengeMandated
                return this
            }

            fun setAcsSignedContent(acsSignedContent: String?): Builder {
                this.acsSignedContent = acsSignedContent
                return this
            }

            fun setAcsTransId(acsTransId: String?): Builder {
                this.acsTransId = acsTransId
                return this
            }

            fun setAcsUrl(acsUrl: String?): Builder {
                this.acsUrl = acsUrl
                return this
            }

            fun setAuthenticationType(authenticationType: String?): Builder {
                this.authenticationType = authenticationType
                return this
            }

            fun setCardholderInfo(cardholderInfo: String?): Builder {
                this.cardholderInfo = cardholderInfo
                return this
            }

            fun setMessageExtension(messageExtension: List<MessageExtension>?): Builder {
                this.messageExtension = messageExtension
                return this
            }

            fun setMessageType(messageType: String?): Builder {
                this.messageType = messageType
                return this
            }

            fun setMessageVersion(messageVersion: String?): Builder {
                this.messageVersion = messageVersion
                return this
            }

            fun setSdkTransId(sdkTransId: String?): Builder {
                this.sdkTransId = sdkTransId
                return this
            }

            fun setTransStatus(transStatus: String?): Builder {
                this.transStatus = transStatus
                return this
            }

            override fun build(): Ares {
                return Ares(
                    threeDSServerTransId, acsChallengeMandated, acsSignedContent, acsTransId,
                    acsUrl, authenticationType, cardholderInfo, messageExtension, messageType,
                    messageVersion, sdkTransId, transStatus
                )
            }
        }

        companion object {
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

            @JvmStatic
            @Throws(JSONException::class)
            internal fun fromJson(aresJson: JSONObject?): Ares? {
                return if (aresJson == null) {
                    null
                } else Builder()
                    .setThreeDSServerTransId(aresJson.getString(FIELD_THREE_DS_SERVER_TRANS_ID))
                    .setAcsChallengeMandated(optString(aresJson, FIELD_ACS_CHALLENGE_MANDATED))
                    .setAcsSignedContent(optString(aresJson, FIELD_ACS_SIGNED_CONTENT))
                    .setAcsTransId(aresJson.getString(FIELD_ACS_TRANS_ID))
                    .setAcsUrl(optString(aresJson, FIELD_ACS_URL))
                    .setAuthenticationType(optString(aresJson, FIELD_AUTHENTICATION_TYPE))
                    .setCardholderInfo(optString(aresJson, FIELD_CARDHOLDER_INFO))
                    .setMessageType(aresJson.getString(FIELD_MESSAGE_TYPE))
                    .setMessageVersion(aresJson.getString(FIELD_MESSAGE_VERSION))
                    .setSdkTransId(optString(aresJson, FIELD_SDK_TRANS_ID))
                    .setTransStatus(optString(aresJson, FIELD_TRANS_STATUS))
                    .setMessageExtension(MessageExtension.fromJson(
                        aresJson.optJSONArray(FIELD_MESSAGE_EXTENSION)))
                    .build()
            }
        }
    }

    data class MessageExtension constructor(
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

        internal class Builder : ObjectBuilder<MessageExtension> {
            private var name: String? = null
            private var criticalityIndicator: Boolean = false
            private var id: String? = null
            private var data: Map<String, String>? = null

            fun setName(name: String?): Builder {
                this.name = name
                return this
            }

            fun setCriticalityIndicator(criticalityIndicator: Boolean): Builder {
                this.criticalityIndicator = criticalityIndicator
                return this
            }

            fun setId(id: String?): Builder {
                this.id = id
                return this
            }

            fun setData(data: Map<String, String>?): Builder {
                this.data = data
                return this
            }

            override fun build(): MessageExtension {
                return MessageExtension(name, criticalityIndicator, id, data)
            }
        }

        companion object {
            private const val FIELD_NAME = "name"
            private const val FIELD_ID = "id"
            private const val FIELD_CRITICALITY_INDICATOR = "criticalityIndicator"
            private const val FIELD_DATA = "data"

            @JvmStatic
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

    data class ThreeDS2Error constructor(
        val threeDSServerTransId: String?,
        val acsTransId: String?,
        val dsTransId: String?,
        val errorCode: String?,
        val errorComponent: String?,
        val errorDescription: String?,
        val errorDetail: String?,
        val errorMessageType: String?,
        val messageType: String?,
        val messageVersion: String?,
        val sdkTransId: String?
    ) {
        internal class Builder : ObjectBuilder<ThreeDS2Error> {
            private var threeDSServerTransId: String? = null
            private var acsTransId: String? = null
            private var dsTransId: String? = null
            private var errorCode: String? = null
            private var errorComponent: String? = null
            private var errorDescription: String? = null
            private var errorDetail: String? = null
            private var errorMessageType: String? = null
            private var messageType: String? = null
            private var messageVersion: String? = null
            private var sdkTransId: String? = null

            fun setThreeDSServerTransId(threeDSServerTransId: String?): Builder {
                this.threeDSServerTransId = threeDSServerTransId
                return this
            }

            fun setAcsTransId(acsTransId: String?): Builder {
                this.acsTransId = acsTransId
                return this
            }

            fun setDsTransId(dsTransId: String?): Builder {
                this.dsTransId = dsTransId
                return this
            }

            fun setErrorCode(errorCode: String?): Builder {
                this.errorCode = errorCode
                return this
            }

            fun setErrorComponent(errorComponent: String?): Builder {
                this.errorComponent = errorComponent
                return this
            }

            fun setErrorDescription(errorDescription: String?): Builder {
                this.errorDescription = errorDescription
                return this
            }

            fun setErrorDetail(errorDetail: String?): Builder {
                this.errorDetail = errorDetail
                return this
            }

            fun setErrorMessageType(errorMessageType: String?): Builder {
                this.errorMessageType = errorMessageType
                return this
            }

            fun setMessageType(messageType: String?): Builder {
                this.messageType = messageType
                return this
            }

            fun setMessageVersion(messageVersion: String?): Builder {
                this.messageVersion = messageVersion
                return this
            }

            fun setSdkTransId(sdkTransId: String?): Builder {
                this.sdkTransId = sdkTransId
                return this
            }

            override fun build(): ThreeDS2Error {
                return ThreeDS2Error(threeDSServerTransId, acsTransId,
                    dsTransId, errorCode, errorComponent, errorDescription,
                    errorDetail,
                    errorMessageType, messageType, messageVersion, sdkTransId)
            }
        }

        companion object {
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

            @JvmStatic
            @Throws(JSONException::class)
            internal fun fromJson(errorJson: JSONObject): ThreeDS2Error {
                return Builder()
                    .setThreeDSServerTransId(errorJson.getString(FIELD_THREE_DS_SERVER_TRANS_ID))
                    .setAcsTransId(optString(errorJson, FIELD_ACS_TRANS_ID))
                    .setDsTransId(optString(errorJson, FIELD_DS_TRANS_ID))
                    .setErrorCode(errorJson.getString(FIELD_ERROR_CODE))
                    .setErrorComponent(errorJson.getString(FIELD_ERROR_COMPONENT))
                    .setErrorDescription(errorJson.getString(FIELD_ERROR_DESCRIPTION))
                    .setErrorDetail(errorJson.getString(FIELD_ERROR_DETAIL))
                    .setErrorMessageType(optString(errorJson, FIELD_ERROR_MESSAGE_TYPE))
                    .setMessageType(errorJson.getString(FIELD_MESSAGE_TYPE))
                    .setMessageVersion(errorJson.getString(FIELD_MESSAGE_VERSION))
                    .setSdkTransId(optString(errorJson, FIELD_SDK_TRANS_ID))
                    .build()
            }
        }
    }

    companion object {
        private const val FIELD_ID = "id"
        private const val FIELD_OBJECT = "object"
        private const val FIELD_ARES = "ares"
        private const val FIELD_CREATED = "created"
        private const val FIELD_ERROR = "error"
        private const val FIELD_FALLBACK_REDIRECT_URL = "fallback_redirect_url"
        private const val FIELD_LIVEMODE = "livemode"
        private const val FIELD_SOURCE = "source"
        private const val FIELD_STATE = "state"

        @JvmStatic
        @Throws(JSONException::class)
        fun fromJson(authResultJson: JSONObject): Stripe3ds2AuthResult {
            return Builder()
                .setId(authResultJson.getString(FIELD_ID))
                .setObjectType(authResultJson.getString(FIELD_OBJECT))
                .setCreated(authResultJson.getLong(FIELD_CREATED))
                .setLiveMode(authResultJson.getBoolean(FIELD_LIVEMODE))
                .setSource(authResultJson.getString(FIELD_SOURCE))
                .setState(authResultJson.optString(FIELD_STATE))
                .setAres(
                    if (authResultJson.isNull(FIELD_ARES))
                        null
                    else
                        Ares.fromJson(authResultJson.optJSONObject(FIELD_ARES))
                )
                .setError(
                    if (authResultJson.isNull(FIELD_ERROR))
                        null
                    else
                        ThreeDS2Error.fromJson(authResultJson.optJSONObject(FIELD_ERROR))
                )
                .setFallbackRedirectUrl(
                    if (authResultJson.isNull(FIELD_FALLBACK_REDIRECT_URL))
                        null
                    else
                        authResultJson.optString(FIELD_FALLBACK_REDIRECT_URL))
                .build()
        }
    }
}
