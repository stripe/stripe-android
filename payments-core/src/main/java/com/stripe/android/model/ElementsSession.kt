package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class ElementsSession(
    val linkSettings: LinkSettings?,
    val paymentMethodSpecs: String?,
    val stripeIntent: StripeIntent,
    val merchantCountry: String?,
    val isEligibleForCardBrandChoice: Boolean,
    val isGooglePayEnabled: Boolean,
) : StripeModel {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class LinkSettings(
        val linkFundingSources: List<String>,
        val linkPassthroughModeEnabled: Boolean,
    ) : StripeModel

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        fun default(stripeIntent: StripeIntent): ElementsSession {
            return ElementsSession(
                linkSettings = null,
                paymentMethodSpecs = null,
                stripeIntent = stripeIntent,
                merchantCountry = null,
                isEligibleForCardBrandChoice = false,
                isGooglePayEnabled = true,
            )
        }
    }
}
