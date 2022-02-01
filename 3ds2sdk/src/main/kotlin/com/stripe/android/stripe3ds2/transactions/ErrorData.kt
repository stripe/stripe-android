package com.stripe.android.stripe3ds2.transactions

import android.os.Parcelable
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject

@Parcelize
data class ErrorData internal constructor(
    val serverTransId: String? = null,
    val acsTransId: String? = null,
    val dsTransId: String? = null,
    val errorCode: String,
    val errorComponent: ErrorComponent? = null,
    val errorDescription: String,
    val errorDetail: String,
    val errorMessageType: String? = null,
    val messageVersion: String,
    val sdkTransId: SdkTransactionId?
) : Parcelable {
    @Throws(JSONException::class)
    internal fun toJson(): JSONObject {
        val json = JSONObject()
            .put(FIELD_MESSAGE_TYPE, MESSAGE_TYPE)
            .put(FIELD_MESSAGE_VERSION, messageVersion)
            .put(FIELD_SDK_TRANS_ID, sdkTransId)
            .put(FIELD_ERROR_CODE, errorCode)
            .put(FIELD_ERROR_DESCRIPTION, errorDescription)
            .put(FIELD_ERROR_DETAIL, errorDetail)
        serverTransId?.let {
            json.put(FIELD_3DS_SERVER_TRANS_ID, it)
        }
        acsTransId?.let {
            json.put(FIELD_ACS_TRANS_ID, it)
        }
        dsTransId?.let {
            json.put(FIELD_DS_TRANS_ID, it)
        }
        errorComponent?.let {
            json.put(FIELD_ERROR_COMPONENT, it.code)
        }
        errorMessageType?.let {
            json.put(FIELD_ERROR_MESSAGE_TYPE, it)
        }
        return json
    }

    enum class ErrorComponent(val code: String) {
        ThreeDsSdk("C"),
        ThreeDsServer("S"),
        DirectoryServer("D"),
        Acs("A");

        internal companion object {
            fun fromCode(code: String?): ErrorComponent? {
                return values().firstOrNull { it.code == code }
            }
        }
    }

    internal companion object {
        private const val FIELD_3DS_SERVER_TRANS_ID = "threeDSServerTransID"
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
        private const val MESSAGE_TYPE = "Erro"

        internal fun fromJson(payload: JSONObject): ErrorData {
            return ErrorData(
                serverTransId = payload.optString(FIELD_3DS_SERVER_TRANS_ID),
                acsTransId = payload.optString(FIELD_ACS_TRANS_ID),
                dsTransId = payload.optString(FIELD_DS_TRANS_ID),
                errorCode = payload.optString(FIELD_ERROR_CODE),
                errorComponent = ErrorComponent.fromCode(payload.optString(FIELD_ERROR_COMPONENT)),
                errorDescription = payload.optString(FIELD_ERROR_DESCRIPTION),
                errorDetail = payload.optString(FIELD_ERROR_DETAIL),
                errorMessageType = payload.optString(FIELD_ERROR_MESSAGE_TYPE),
                messageVersion = payload.optString(FIELD_MESSAGE_VERSION),
                sdkTransId = payload.optString(FIELD_SDK_TRANS_ID)?.let {
                    SdkTransactionId(it)
                }
            )
        }

        internal fun isErrorMessage(payload: JSONObject): Boolean {
            return MESSAGE_TYPE == payload.optString("messageType")
        }
    }
}
