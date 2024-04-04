package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import com.stripe.android.model.PaymentMethod.Type.Link
import kotlinx.parcelize.Parcelize

private val LinkSupportedFundingSources = setOf("card", "bank_account")

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class ElementsSession(
    val linkSettings: LinkSettings?,
    val paymentMethodSpecs: String?,
    val stripeIntent: StripeIntent,
    val merchantCountry: String?,
    val isEligibleForCardBrandChoice: Boolean,
    val isGooglePayEnabled: Boolean,
    val sessionsError: Throwable? = null,
) : StripeModel {

    val linkPassthroughModeEnabled: Boolean
        get() = linkSettings?.linkPassthroughModeEnabled ?: false

    val linkFlags: Map<String, Boolean>
        get() = linkSettings?.linkFlags ?: emptyMap()

    val disableLinkSignup: Boolean
        get() = linkSettings?.disableLinkSignup ?: false

    val isLinkEnabled: Boolean
        get() {
            val allowsLink = Link.code in stripeIntent.paymentMethodTypes
            val hasValidFundingSource = stripeIntent.linkFundingSources.any { it in LinkSupportedFundingSources }
            return (allowsLink && hasValidFundingSource) || linkPassthroughModeEnabled
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class LinkSettings(
        val linkFundingSources: List<String>,
        val linkPassthroughModeEnabled: Boolean,
        val linkFlags: Map<String, Boolean>,
        val disableLinkSignup: Boolean,
        val useRebrand: Boolean = useRebrandDefault,
    ) : StripeModel {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        companion object {
            // Should default to true in case we remove the server-side flag someday.
            const val useRebrandDefault: Boolean = true
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        fun createFromFallback(
            stripeIntent: StripeIntent,
            sessionsError: Throwable?,
        ): ElementsSession {
            return ElementsSession(
                linkSettings = null,
                paymentMethodSpecs = null,
                stripeIntent = stripeIntent,
                merchantCountry = null,
                isEligibleForCardBrandChoice = false,
                isGooglePayEnabled = true,
                sessionsError = sessionsError,
            )
        }
    }
}
