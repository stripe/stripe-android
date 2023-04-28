package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class DeferredIntentParams(
    val mode: Mode,
    val paymentMethodTypes: List<String>,
    val onBehalfOf: String?,
) : StripeModel {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    sealed interface Mode : Parcelable {
        val code: String

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Parcelize
        data class Payment(
            val amount: Long,
            val currency: String,
            val setupFutureUsage: StripeIntent.Usage?,
            val captureMethod: CaptureMethod?,
        ) : Mode {
            override val code: String get() = "payment"
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Parcelize
        data class Setup(
            val currency: String?,
            val setupFutureUsage: StripeIntent.Usage,
        ) : Mode {
            override val code: String get() = "setup"
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class CaptureMethod(val code: String) {
        Manual("manual"),
        Automatic("automatic"),
    }

    fun toQueryParams(): Map<String, Any?> {
        return mapOf(
            "deferred_intent[mode]" to mode.code,
            "deferred_intent[amount]" to (mode as? Mode.Payment)?.amount,
            "deferred_intent[currency]" to (mode as? Mode.Payment)?.currency,
            "deferred_intent[setup_future_usage]" to (mode as? Mode.Payment)?.setupFutureUsage?.code,
            "deferred_intent[capture_method]" to (mode as? Mode.Payment)?.captureMethod?.code,
            "deferred_intent[on_behalf_of]" to onBehalfOf,
        ) + paymentMethodTypes.mapIndexed { index, paymentMethodType ->
            "deferred_intent[payment_method_types][$index]" to paymentMethodType
        }
    }
}
