package com.stripe.android.model

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class ElementsSession(
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
