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
    val externalPaymentMethodData: String?,
    val stripeIntent: StripeIntent,
    val customer: Customer?,
    val merchantCountry: String?,
    val cardBrandChoice: CardBrandChoice?,
    val isGooglePayEnabled: Boolean,
    val sessionsError: Throwable? = null,
    val elementSessionId: String?,
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

    val useAttestationEndpointsForLink: Boolean
        get() = linkSettings?.useAttestationEndpoints ?: false

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class LinkSettings(
        val linkFundingSources: List<String>,
        val linkPassthroughModeEnabled: Boolean,
        val linkMode: LinkMode?,
        val linkFlags: Map<String, Boolean>,
        val disableLinkSignup: Boolean,
        val linkConsumerIncentive: LinkConsumerIncentive?,
        val useAttestationEndpoints: Boolean
    ) : StripeModel

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class CardBrandChoice(
        val eligible: Boolean,
        val preferredNetworks: List<String>,
    ) : StripeModel

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class Customer(
        val paymentMethods: List<PaymentMethod>,
        val defaultPaymentMethod: String?,
        val session: Session,
    ) : StripeModel {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Parcelize
        data class Session(
            val id: String,
            val liveMode: Boolean,
            val apiKey: String,
            val apiKeyExpiry: Int,
            val customerId: String,
            val components: Components,
        ) : StripeModel

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Parcelize
        data class Components(
            val mobilePaymentElement: MobilePaymentElement,
            val customerSheet: CustomerSheet,
        ) : StripeModel {
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            sealed interface MobilePaymentElement : StripeModel {
                @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                @Parcelize
                data object Disabled : MobilePaymentElement

                @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                @Parcelize
                data class Enabled(
                    val isPaymentMethodSaveEnabled: Boolean,
                    val isPaymentMethodRemoveEnabled: Boolean,
                    val canRemoveLastPaymentMethod: Boolean,
                    val allowRedisplayOverride: PaymentMethod.AllowRedisplay?
                ) : MobilePaymentElement
            }

            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            sealed interface CustomerSheet : StripeModel {
                @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                @Parcelize
                data object Disabled : CustomerSheet

                @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                @Parcelize
                data class Enabled(
                    val isPaymentMethodRemoveEnabled: Boolean,
                    val canRemoveLastPaymentMethod: Boolean,
                ) : CustomerSheet
            }
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
                externalPaymentMethodData = null,
                stripeIntent = stripeIntent,
                customer = null,
                merchantCountry = null,
                cardBrandChoice = null,
                isGooglePayEnabled = true,
                sessionsError = sessionsError,
                elementSessionId = null
            )
        }
    }
}
