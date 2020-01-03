package com.stripe.android

import android.os.Parcelable
import com.stripe.android.model.StripeParamsModel
import java.text.DecimalFormat
import kotlinx.android.parcel.Parcelize
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

@Parcelize
internal data class Stripe3ds2AuthParams(
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
        val params = mapOf(
            FIELD_SOURCE to sourceId,
            FIELD_APP to createAppParams().toString()
        )

        return returnUrl?.let {
            params.plus(FIELD_FALLBACK_RETURN_URL to it)
        } ?: params
    }

    private fun createAppParams(): JSONObject {
        return try {
            JSONObject()
                .put(FIELD_SDK_APP_ID, sdkAppId)
                .put(FIELD_SDK_TRANS_ID, sdkTransactionId)
                .put(FIELD_SDK_ENC_DATA, deviceData)
                .put(FIELD_SDK_EPHEM_PUB_KEY, JSONObject(sdkEphemeralPublicKey))
                .put(FIELD_SDK_MAX_TIMEOUT, MAX_TIMEOUT_FORMATTER.format(maxTimeout.toLong()))
                .put(FIELD_SDK_REFERENCE_NUMBER, sdkReferenceNumber)
                .put(FIELD_MESSAGE_VERSION, messageVersion)
                .put(FIELD_DEVICE_RENDER_OPTIONS, createDeviceRenderOptions())
        } catch (ignore: JSONException) {
            JSONObject()
        }
    }

    private fun createDeviceRenderOptions(): JSONObject {
        return try {
            JSONObject()
                .put(FIELD_SDK_INTERFACE, "03")
                .put(FIELD_SDK_UI_TYPE, JSONArray(listOf("01", "02", "03", "04", "05")))
        } catch (ignore: JSONException) {
            JSONObject()
        }
    }

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

        private val MAX_TIMEOUT_FORMATTER = DecimalFormat("00")
    }
}
