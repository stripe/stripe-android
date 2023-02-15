package com.stripe.android.model

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class DeferredIntent(
    val mode: Type,
    val amount: Long? = null,
    val currency: String? = null,
    val setupFutureUsage: SetupFutureUsage? = null,
    val captureMethod: CaptureMethod? = null,
    val customer: String? = null,
    val onBehalfOf: String? = null,
    val paymentMethodTypes: Set<String> = setOf()
) : StripeModel {
    enum class Type {
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
