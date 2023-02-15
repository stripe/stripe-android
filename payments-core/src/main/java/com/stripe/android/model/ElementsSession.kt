package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class ElementsSession(
    val linkSettings: LinkSettings?,
    val paymentMethodTypes: List<String>,
    val unactivatedPaymentMethodTypes: List<String>,
    val paymentMethodSpecs: String?,
    val stripeIntent: StripeIntent?
) : StripeModel {

    @Parcelize
    data class LinkSettings(
        val linkFundingSources: List<String>
    ) : StripeModel
}
