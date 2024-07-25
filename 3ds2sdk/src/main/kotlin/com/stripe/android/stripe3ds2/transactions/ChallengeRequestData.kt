package com.stripe.android.stripe3ds2.transactions

import android.os.Parcelable
import com.stripe.android.stripe3ds2.exceptions.SDKRuntimeException
import com.stripe.android.stripe3ds2.security.DefaultMessageTransformer
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import java.io.Serializable

/**
 * Model representing CReq message
 *
 * Note:
 * - `sdkCounterStoA` will be added by [DefaultMessageTransformer]
 */
@Parcelize
data class ChallengeRequestData constructor(
    val messageVersion: String,
    val threeDsServerTransId: String,
    val acsTransId: String,
    val sdkTransId: SdkTransactionId,
    val challengeDataEntry: String? = null,
    val cancelReason: CancelReason? = null,
    val challengeHtmlDataEntry: String? = null,
    val messageExtensions: List<MessageExtension>? = null,
    val oobContinue: Boolean? = null,
    val shouldResendChallenge: Boolean? = null
) : Serializable, Parcelable {

    internal fun toJson(): JSONObject {
        runCatching {
            val json = JSONObject()
                .put(FIELD_MESSAGE_TYPE, MESSAGE_TYPE)
                .put(FIELD_MESSAGE_VERSION, messageVersion)
                .put(FIELD_SDK_TRANS_ID, sdkTransId.value)
                .put(FIELD_3DS_SERVER_TRANS_ID, threeDsServerTransId)
                .put(FIELD_ACS_TRANS_ID, acsTransId)

            if (cancelReason != null) {
                json.put(FIELD_CHALLENGE_CANCEL, cancelReason.code)
            }

            if (challengeDataEntry != null) {
                json.put(FIELD_CHALLENGE_DATA_ENTRY, challengeDataEntry)
            }

            if (challengeHtmlDataEntry != null) {
                json.put(FIELD_CHALLENGE_HTML_DATA_ENTRY, challengeHtmlDataEntry)
            }

            MessageExtension.toJsonArray(messageExtensions)?.let {
                json.put(FIELD_MESSAGE_EXTENSION, it)
            }

            if (oobContinue != null) {
                json.put(FIELD_OOB_CONTINUE, oobContinue)
            }

            if (shouldResendChallenge != null) {
                json.put(FIELD_RESEND_CHALLENGE, if (shouldResendChallenge) "Y" else "N")
            }

            return json
        }.getOrElse {
            throw SDKRuntimeException(it)
        }
    }

    /**
     * Create a copy of [ChallengeRequestData] with any potentially sensitive data removed.
     */
    internal fun sanitize(): ChallengeRequestData = copy(
        challengeDataEntry = null,
        challengeHtmlDataEntry = null
    )

    enum class CancelReason(val code: String) {
        UserSelected("01"),
        Reserved("02"),
        TransactionTimedOutDecoupled("03"),
        TransactionTimedOutOther("04"),
        TransactionTimedOutFirstCreq("05"),
        TransactionError("06"),
        Unknown("07")
    }

    internal companion object {
        internal const val FIELD_ACS_TRANS_ID: String = "acsTransID"
        internal const val FIELD_3DS_SERVER_TRANS_ID: String = "threeDSServerTransID"
        internal const val FIELD_CHALLENGE_CANCEL: String = "challengeCancel"
        internal const val FIELD_CHALLENGE_DATA_ENTRY: String = "challengeDataEntry"
        internal const val FIELD_CHALLENGE_HTML_DATA_ENTRY: String = "challengeHTMLDataEntry"
        internal const val FIELD_MESSAGE_EXTENSION: String = "messageExtensions"
        internal const val FIELD_MESSAGE_TYPE: String = "messageType"
        internal const val FIELD_MESSAGE_VERSION: String = "messageVersion"
        internal const val FIELD_OOB_CONTINUE: String = "oobContinue"
        internal const val FIELD_RESEND_CHALLENGE: String = "resendChallenge"
        internal const val FIELD_SDK_TRANS_ID: String = "sdkTransID"

        internal const val MESSAGE_TYPE: String = "CReq"
    }
}
