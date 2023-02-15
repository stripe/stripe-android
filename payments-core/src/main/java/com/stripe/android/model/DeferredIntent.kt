package com.stripe.android.model

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class DeferredIntent(
    val mode: Mode,
    val amount: Long? = null,
    val currency: String? = null,
    val setupFutureUsage: SetupFutureUsage? = null,
    val captureMethod: CaptureMethod? = null,
    val customer: String? = null,
    val onBehalfOf: String? = null,
    val paymentMethodTypes: Set<String> = emptySet()
) : StripeModel {
    enum class Mode {
        Payment,
        Setup
    }

    enum class SetupFutureUsage {
        OnSession,
        OffSession
    }

    enum class CaptureMethod {
        Manual,
        Automatic
    }
}
