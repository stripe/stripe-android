package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class ElementsSessionParams(
    val type: Type,
    val clientSecret: String? = null,
    val locale: String? = null,
    val deferredIntent: DeferredIntent? = null
) : StripeModel {

    // TODO Make this a sealed interface with DeferredIntent(val intent: DeferredIntent)
    enum class Type(val value: String) {
        PaymentIntent("payment_intent"),
        SetupIntent("setup_intent"),
        DeferredIntent("deferred_intent")
    }
}
