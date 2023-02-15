package com.stripe.android.model

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class ElementsSessionParams(
    val type: Type,
    val clientSecret: String? = null,
    val locale: String? = null,
    val deferredIntent: DeferredIntent? = null
) : StripeModel {
    enum class Type(val value: String) {
        PaymentIntent("payment_intent"),
        SetupIntent("setup_intent"),
        DeferredIntent("deferred_intent")
    }
}
