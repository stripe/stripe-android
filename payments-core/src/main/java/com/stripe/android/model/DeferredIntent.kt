package com.stripe.android.model

import android.os.Parcelable
import android.os.SystemClock
import androidx.annotation.RestrictTo
import com.stripe.android.model.DeferredIntent.CaptureMethod
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

/**
 * A [DeferredIntent] is a set of intent params for the deferred PaymentSheet flow.
 */
@Parcelize
data class DeferredIntent
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    val mode: Mode,
    /**
     * Unique identifier for the object.
     */
    override val id: String? = null,

    /**
     * The list of payment method types (e.g. card) that this [DeferredIntent] is allowed to
     * use.
     */
    override val paymentMethodTypes: List<String> = emptyList(),

    /**
     * Controls when the funds will be captured from the customer’s account.
     * See [CaptureMethod].
     */
    val captureMethod: CaptureMethod?,

    /**
     * The [DeferredIntent] does not have a client secret, it will be retrieved from the merchants
     * server.
     */
    override val clientSecret: String? = null,

    /**
     * Country code of the user.
     */
    val countryCode: String?,

    /**
     * Time at which the object was created. Measured in seconds since the Unix epoch.
     */
    override val created: Long,

    /**
     * An arbitrary string attached to the object. Often useful for displaying to users.
     */
    override val description: String? = null,

    /**
     * Has the value `true` if the object exists in live mode or the value
     * `false` if the object exists in test mode.
     */
    override val isLiveMode: Boolean,

    override val paymentMethod: PaymentMethod? = null,

    /**
     * ID of the payment method (a PaymentMethod, Card, BankAccount, or saved Source object)
     * to attach to this [DeferredIntent].
     */
    override val paymentMethodId: String? = null,

    /**
     * Status of this [DeferredIntent].
     */
    override val status: StripeIntent.Status? = null,

    val setupFutureUsage: StripeIntent.Usage?,

    /**
     * Payment types that have not been activated in livemode, but have been activated in testmode.
     */
    override val unactivatedPaymentMethods: List<String>,

    /**
     * Payment types that are accepted when paying with Link.
     */
    override val linkFundingSources: List<String> = emptyList(),

    override val nextActionData: StripeIntent.NextActionData? = null,

    private val paymentMethodOptionsJsonString: String? = null

) : StripeIntent {

    @Parcelize
    sealed interface Mode : Parcelable{
        @Parcelize
        data class Payment(val amount: Long, val currency: String) : Mode

        @Parcelize
        data class Setup(val currency: String?) : Mode
    }

    enum class CaptureMethod {
        Manual,
        Automatic
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
    private fun isTopLevelSetupFutureUsageSet() =
        when (setupFutureUsage) {
            StripeIntent.Usage.OnSession -> true
            StripeIntent.Usage.OffSession -> true
            StripeIntent.Usage.OneTime -> false
            null -> false
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun isLpmLevelSetupFutureUsageSet(code: PaymentMethodCode): Boolean {
        return isTopLevelSetupFutureUsageSet() || paymentMethodOptionsJsonString?.let {
            JSONObject(paymentMethodOptionsJsonString)
                .optJSONObject(code)
                ?.optString("setup_future_usage")?.let {
                    true
                } ?: false
        } ?: false
    }
}
