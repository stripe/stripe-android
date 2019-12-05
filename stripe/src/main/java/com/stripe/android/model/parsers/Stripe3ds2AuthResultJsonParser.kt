package com.stripe.android.model.parsers

import com.stripe.android.model.Stripe3ds2AuthResult
import com.stripe.android.model.StripeJsonUtils
import org.json.JSONArray
import org.json.JSONObject

internal class Stripe3ds2AuthResultJsonParser : ModelJsonParser<Stripe3ds2AuthResult> {
    override fun parse(json: JSONObject): Stripe3ds2AuthResult {
        val ares =
            if (json.isNull(FIELD_ARES)) {
                null
            } else {
                json.optJSONObject(FIELD_ARES)?.let {
                    AresJsonParser().parse(it)
                }
            }
        val error =
            if (json.isNull(FIELD_ERROR)) {
                null
            } else {
                json.optJSONObject(FIELD_ERROR)?.let {
                    ThreeDS2ErrorJsonParser().parse(it)
                }
            }
        val fallbackRedirectUrl =
            if (json.isNull(FIELD_FALLBACK_REDIRECT_URL)) {
                null
            } else {
                json.optString(FIELD_FALLBACK_REDIRECT_URL)
            }
        return Stripe3ds2AuthResult(
            id = json.getString(FIELD_ID),
            objectType = json.getString(FIELD_OBJECT),
            created = json.getLong(FIELD_CREATED),
            liveMode = json.getBoolean(FIELD_LIVEMODE),
            source = json.getString(FIELD_SOURCE),
            state = json.optString(FIELD_STATE),
            ares = ares,
            error = error,
            fallbackRedirectUrl = fallbackRedirectUrl
        )
    }

    internal class AresJsonParser : ModelJsonParser<Stripe3ds2AuthResult.Ares> {
        override fun parse(json: JSONObject): Stripe3ds2AuthResult.Ares {
            return Stripe3ds2AuthResult.Ares(
                threeDSServerTransId = json.getString(FIELD_THREE_DS_SERVER_TRANS_ID),
                acsChallengeMandated = StripeJsonUtils.optString(json, FIELD_ACS_CHALLENGE_MANDATED),
                acsSignedContent = StripeJsonUtils.optString(json, FIELD_ACS_SIGNED_CONTENT),
                acsTransId = json.getString(FIELD_ACS_TRANS_ID),
                acsUrl = StripeJsonUtils.optString(json, FIELD_ACS_URL),
                authenticationType = StripeJsonUtils.optString(json, FIELD_AUTHENTICATION_TYPE),
                cardholderInfo = StripeJsonUtils.optString(json, FIELD_CARDHOLDER_INFO),
                messageType = json.getString(FIELD_MESSAGE_TYPE),
                messageVersion = json.getString(FIELD_MESSAGE_VERSION),
                sdkTransId = StripeJsonUtils.optString(json, FIELD_SDK_TRANS_ID),
                transStatus = StripeJsonUtils.optString(json, FIELD_TRANS_STATUS),
                messageExtension = Stripe3ds2AuthResult.MessageExtension.fromJson(
                    json.optJSONArray(FIELD_MESSAGE_EXTENSION)
                )
            )
        }

        private companion object {
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
        }
    }

    internal class MessageExtensionJsonParser : ModelJsonParser<Stripe3ds2AuthResult.MessageExtension> {
        override fun parse(json: JSONObject): Stripe3ds2AuthResult.MessageExtension {
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

            return Stripe3ds2AuthResult.MessageExtension(
                name = StripeJsonUtils.optString(json, FIELD_NAME),
                criticalityIndicator = json.optBoolean(FIELD_CRITICALITY_INDICATOR),
                id = StripeJsonUtils.optString(json, FIELD_ID),
                data = data.toMap()
            )
        }

        private companion object {
            private const val FIELD_NAME = "name"
            private const val FIELD_ID = "id"
            private const val FIELD_CRITICALITY_INDICATOR = "criticalityIndicator"
            private const val FIELD_DATA = "data"
        }
    }

    internal class ThreeDS2ErrorJsonParser : ModelJsonParser<Stripe3ds2AuthResult.ThreeDS2Error> {
        override fun parse(json: JSONObject): Stripe3ds2AuthResult.ThreeDS2Error {
            return Stripe3ds2AuthResult.ThreeDS2Error(
                threeDSServerTransId = json.getString(FIELD_THREE_DS_SERVER_TRANS_ID),
                acsTransId = StripeJsonUtils.optString(json, FIELD_ACS_TRANS_ID),
                dsTransId = StripeJsonUtils.optString(json, FIELD_DS_TRANS_ID),
                errorCode = json.getString(FIELD_ERROR_CODE),
                errorComponent = json.getString(FIELD_ERROR_COMPONENT),
                errorDescription = json.getString(FIELD_ERROR_DESCRIPTION),
                errorDetail = json.getString(FIELD_ERROR_DETAIL),
                errorMessageType = StripeJsonUtils.optString(json, FIELD_ERROR_MESSAGE_TYPE),
                messageType = json.getString(FIELD_MESSAGE_TYPE),
                messageVersion = json.getString(FIELD_MESSAGE_VERSION),
                sdkTransId = StripeJsonUtils.optString(json, FIELD_SDK_TRANS_ID)
            )
        }

        private companion object {
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
        }
    }

    private companion object {
        private const val FIELD_ID = "id"
        private const val FIELD_OBJECT = "object"
        private const val FIELD_ARES = "ares"
        private const val FIELD_CREATED = "created"
        private const val FIELD_ERROR = "error"
        private const val FIELD_FALLBACK_REDIRECT_URL = "fallback_redirect_url"
        private const val FIELD_LIVEMODE = "livemode"
        private const val FIELD_SOURCE = "source"
        private const val FIELD_STATE = "state"
    }
}
