package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.model.DeferredIntent.CaptureMethod
import kotlinx.parcelize.Parcelize

/**
 * A [DeferredIntent] is a set of intent params for the deferred PaymentSheet flow.
 */
@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class DeferredIntent(
    /**
     * The mode of the [DeferredIntent] one of payment or setup
     */
    val mode: Mode,

    /**
     * Unique identifier for the object.
     */
    override val id: String? = null,

    /**
     * The list of payment method types (e.g. card) that this [DeferredIntent] is allowed to
     * use.
     */
    override val paymentMethodTypes: List<String>,

    /**
     * Controls when the funds will be captured from the customer’s account.
     * See [CaptureMethod].
     */
    val captureMethod: CaptureMethod?,

    /**
     * Country code of the user.
     */
    val countryCode: String?,

    /**
     * Time at which the object was created. Measured in seconds since the Unix epoch.
     */
    override val created: Long,

    /**
     * Has the value `true` if the object exists in live mode or the value
     * `false` if the object exists in test mode.
     */
    override val isLiveMode: Boolean,

    /**
     * Indicates how the payment method is intended to be used in the future.
     *
     * Use [StripeIntent.Usage.OnSession] if you intend to only reuse the payment method when the
     * customer is in your checkout flow. Use [StripeIntent.Usage.OffSession] if your customer may
     * or may not be in your checkout flow. If not provided, this value defaults to
     * [StripeIntent.Usage.OffSession].
     */
    val setupFutureUsage: StripeIntent.Usage?,

    /**
     * Payment types that have not been activated in livemode, but have been activated in testmode.
     */
    override val unactivatedPaymentMethods: List<String>,

    /**
     * Payment types that are accepted when paying with Link.
     */
    override val linkFundingSources: List<String>,
) : StripeIntent {

    override val clientSecret: String?
        get() = null

    override val paymentMethod: PaymentMethod?
        get() = null

    override val paymentMethodId: String?
        get() = null

    override val nextActionData: StripeIntent.NextActionData?
        get() = null

    override val status: StripeIntent.Status?
        get() = null

    override val description: String?
        get() = null

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    sealed interface Mode : Parcelable {
        val code: String

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Parcelize
        data class Payment(
            override val code: String = "payment",
            val amount: Long,
            val currency: String
        ) : Mode

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Parcelize
        data class Setup(
            override val code: String = "setup",
            val currency: String?
        ) : Mode
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class CaptureMethod(val code: String) {
        Manual("manual"),
        Automatic("automatic")
    }

    override val nextActionType: StripeIntent.NextActionType?
        get() = null

    override val isConfirmed: Boolean
        get() = false

    override val lastErrorMessage: String?
        get() = null

    override fun requiresAction(): Boolean {
        return false
    }

    override fun requiresConfirmation(): Boolean {
        return false
    }

    /**
     * SetupFutureUsage is considered to be set if it is on or off session.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun isTopLevelSetupFutureUsageSet() =
        when (setupFutureUsage) {
            StripeIntent.Usage.OnSession -> true
            StripeIntent.Usage.OffSession -> true
            StripeIntent.Usage.OneTime -> false
            null -> false
        }
}
