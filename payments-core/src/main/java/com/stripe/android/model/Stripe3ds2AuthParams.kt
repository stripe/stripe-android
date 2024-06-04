package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class Stripe3ds2AuthParams(
    private val sourceId: String,
    private val sdkAppId: String,
    private val sdkReferenceNumber: String,
    private val sdkTransactionId: String,
    private val deviceData: String,
    private val sdkEphemeralPublicKey: String,
    private val messageVersion: String,
    private val maxTimeout: Int,
    private val returnUrl: String?
) : StripeParamsModel, Parcelable {

    override fun toParamMap(): Map<String, Any> {
        return mapOf(
            FIELD_SOURCE to sourceId,
            FIELD_APP to appParams.toString()
        ).plus(
            returnUrl?.let {
                mapOf(FIELD_FALLBACK_RETURN_URL to it)
            }.orEmpty()
        )
    }

    internal val appParams: JSONObject
        @JvmSynthetic
        @VisibleForTesting
        get() = runCatching {
            JSONObject()
                .put(FIELD_SDK_APP_ID, sdkAppId)
                .put(FIELD_SDK_TRANS_ID, sdkTransactionId)
                .put(FIELD_SDK_ENC_DATA, deviceData)
                .put(FIELD_SDK_EPHEM_PUB_KEY, JSONObject(sdkEphemeralPublicKey))
                .put(FIELD_SDK_MAX_TIMEOUT, maxTimeout.toString().padStart(2, '0'))
                .put(FIELD_SDK_REFERENCE_NUMBER, sdkReferenceNumber)
                .put(FIELD_MESSAGE_VERSION, messageVersion)
                .put(FIELD_DEVICE_RENDER_OPTIONS, deviceRenderOptions)
        }.getOrDefault(JSONObject())

    private val deviceRenderOptions: JSONObject
        get() = runCatching {
            JSONObject()
                .put(FIELD_SDK_INTERFACE, "03")
                .put(FIELD_SDK_UI_TYPE, JSONArray(listOf("01", "02", "03", "04", "05")))
        }.getOrDefault(JSONObject())

    internal companion object {
        internal const val FIELD_APP = "app"
        internal const val FIELD_SOURCE = "source"
        internal const val FIELD_FALLBACK_RETURN_URL = "fallback_return_url"

        private const val FIELD_SDK_APP_ID = "sdkAppID"
        private const val FIELD_SDK_TRANS_ID = "sdkTransID"
        private const val FIELD_SDK_ENC_DATA = "sdkEncData"
        private const val FIELD_SDK_EPHEM_PUB_KEY = "sdkEphemPubKey"
        private const val FIELD_SDK_MAX_TIMEOUT = "sdkMaxTimeout"
        private const val FIELD_SDK_REFERENCE_NUMBER = "sdkReferenceNumber"
        private const val FIELD_MESSAGE_VERSION = "messageVersion"
        private const val FIELD_DEVICE_RENDER_OPTIONS = "deviceRenderOptions"

        private const val FIELD_SDK_INTERFACE = "sdkInterface"
        private const val FIELD_SDK_UI_TYPE = "sdkUiType"
    }
}
