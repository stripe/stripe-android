package com.stripe.android.model

import com.stripe.android.model.parsers.Stripe3ds2AuthResultJsonParser
import kotlinx.android.parcel.Parcelize
import org.json.JSONArray
import org.json.JSONException

@Parcelize
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
) : StripeModel {
    @Parcelize
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
    ) : StripeModel {
        val isChallenge: Boolean
            get() = VALUE_CHALLENGE == transStatus

        internal companion object {
            internal const val VALUE_CHALLENGE = "C"
        }
    }

    @Parcelize
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
    ) : StripeModel {
        internal companion object {

            @JvmSynthetic
            @Throws(JSONException::class)
            internal fun fromJson(messageExtensionsJson: JSONArray?): List<MessageExtension>? {
                if (messageExtensionsJson == null) {
                    return null
                }

                return (0 until messageExtensionsJson.length())
                    .mapNotNull { messageExtensionsJson.optJSONObject(it) }
                    .map { Stripe3ds2AuthResultJsonParser.MessageExtensionJsonParser().parse(it) }
            }
        }
    }

    @Parcelize
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
    ) : StripeModel
}
