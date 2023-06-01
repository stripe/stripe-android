package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class Stripe3ds2AuthResult internal constructor(
    val id: String?,
    val ares: Ares? = null,
    val created: Long?,
    val source: String?,
    val state: String? = null,
    private val liveMode: Boolean = false,
    val error: ThreeDS2Error? = null,
    val fallbackRedirectUrl: String? = null,
    val creq: String? = null
) : StripeModel {

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Ares internal constructor(
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
    ) : StripeModel

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
