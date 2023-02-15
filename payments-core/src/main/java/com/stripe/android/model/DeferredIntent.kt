package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class DeferredIntent(
    val mode: Mode,
    val countryCode: String?,
    val amount: Long? = null,
    val currency: String? = null,
    val setupFutureUsage: StripeIntent.Usage? = null,
    val captureMethod: CaptureMethod? = null,
    val customer: String? = null,
    val onBehalfOf: String? = null,
    override val isLiveMode: Boolean,
    override val paymentMethodTypes: List<String> = emptyList(),
    override val linkFundingSources: List<String> = emptyList(),
    override val unactivatedPaymentMethods: List<String> = emptyList(),
    val paymentMethodOptionsJsonString: String?,
) : StripeModel, StripeIntent {

    enum class Mode {
        Payment,
        Setup
    }

//    enum class SetupFutureUsage {
//        OnSession,
//        OffSession
//    }

    enum class CaptureMethod {
        Manual,
        Automatic
    }

    override val id: String?
        get() = null // TODO SessionId maybe?
    override val created: Long
        get() = 0L
    override val description: String?
        get() = null
    override val paymentMethod: PaymentMethod?
        get() = null
    override val paymentMethodId: String?
        get() = null
    override val nextActionType: StripeIntent.NextActionType?
        get() = null
    override val clientSecret: String?
        get() = null
    override val status: StripeIntent.Status?
        get() = null
    override val nextActionData: StripeIntent.NextActionData?
        get() = null
    override val isConfirmed: Boolean
        get() = false
    override val lastErrorMessage: String?
        get() = null

    override fun requiresAction(): Boolean {
        // TODO maybe make nullable
        return false
    }

    override fun requiresConfirmation(): Boolean {
        // TODO maybe make nullable
        return false
    }
}
