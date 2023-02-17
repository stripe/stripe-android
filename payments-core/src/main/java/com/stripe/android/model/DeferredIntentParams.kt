package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class DeferredIntentParams(
    val mode: DeferredIntent.Mode,
    val setupFutureUsage: StripeIntent.Usage? = null,
    val captureMethod: DeferredIntent.CaptureMethod? = null,
    val customer: String? = null,
    val onBehalfOf: String? = null,
    val paymentMethodTypes: Set<String> = emptySet()
) : StripeModel {
    fun toQueryParams(): Map<String, Any?> {
        return mapOf(
            "deferred_intent[mode]" to mode.code,
            "deferred_intent[amount]" to (mode as? DeferredIntent.Mode.Payment)?.amount,
            "deferred_intent[currency]" to (mode as? DeferredIntent.Mode.Payment)?.currency,
            "deferred_intent[setup_future_usage]" to setupFutureUsage?.code,
            "deferred_intent[capture_method]" to captureMethod?.code,
            "deferred_intent[customer]" to customer,
            "deferred_intent[on_behalf_of]" to onBehalfOf
        ) + paymentMethodTypes.mapIndexed { index, paymentMethodType ->
            "deferred_intent[payment_method_types][$index]" to paymentMethodType
        }
    }
}
