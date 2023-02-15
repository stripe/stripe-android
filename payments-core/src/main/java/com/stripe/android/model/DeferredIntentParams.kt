package com.stripe.android.model

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
data class DeferredIntentParams(
    val mode: DeferredIntent.Mode,
    val setupFutureUsage: StripeIntent.Usage? = null,
    val captureMethod: DeferredIntent.CaptureMethod? = null,
    val customer: String? = null,
    val onBehalfOf: String? = null,
    val paymentMethodTypes: Set<String> = emptySet()
) : StripeModel
