package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import com.stripe.android.model.PaymentMethod.Type.Link
import kotlinx.parcelize.Parcelize
import java.util.UUID

private val LinkSupportedFundingSources = setOf("card", "bank_account")

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class ElementsSession(
    val linkSettings: LinkSettings?,
    val paymentMethodSpecs: String?,
    val externalPaymentMethodData: String?,
    val stripeIntent: StripeIntent,
    val orderedPaymentMethodTypesAndWallets: List<String>,
    val flags: Map<Flag, Boolean>,
    val experimentsData: ExperimentsData?,
    val customer: Customer?,
    val merchantCountry: String?,
    val cardBrandChoice: CardBrandChoice?,
    val isGooglePayEnabled: Boolean,
    val sessionsError: Throwable? = null,
    val customPaymentMethods: List<CustomPaymentMethod>,
    val elementsSessionId: String,
    private val passiveCaptcha: PassiveCaptchaParams?
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

    val suppressLink2faModal: Boolean
        get() = linkSettings?.suppress2faModal ?: false

    val disableRuxInFlowController: Boolean
        get() = linkSettings?.disableLinkRuxInFlowController ?: false

    val enableLinkInSpm: Boolean
        get() = flags[Flag.ELEMENTS_ENABLE_LINK_SPM] == true

    val allowLinkDefaultOptIn: Boolean
        get() = linkSettings?.linkFlags?.get("link_mobile_disable_default_opt_in") != true

    val linkEnableDisplayableDefaultValuesInEce: Boolean
        get() = linkSettings?.linkEnableDisplayableDefaultValuesInEce ?: false

    val passiveCaptchaParams: PassiveCaptchaParams?
        get() = passiveCaptcha.takeIf { flags[Flag.ELEMENTS_ENABLE_PASSIVE_CAPTCHA] == true }

    val linkSignUpOptInFeatureEnabled: Boolean
        get() = linkSettings?.linkSignUpOptInFeatureEnabled ?: false

    val linkSignUpOptInInitialValue: Boolean
        get() = linkSettings?.linkSignUpOptInInitialValue ?: false

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class LinkSettings(
        val linkFundingSources: List<String>,
        val linkPassthroughModeEnabled: Boolean,
        val linkMode: LinkMode?,
        val linkFlags: Map<String, Boolean>,
        val disableLinkSignup: Boolean,
        val linkConsumerIncentive: LinkConsumerIncentive?,
        val useAttestationEndpoints: Boolean,
        val suppress2faModal: Boolean,
        val disableLinkRuxInFlowController: Boolean,
        val linkEnableDisplayableDefaultValuesInEce: Boolean,
        val linkSignUpOptInFeatureEnabled: Boolean,
        val linkSignUpOptInInitialValue: Boolean
    ) : StripeModel

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class ExperimentsData(
        val arbId: String,
        val experimentAssignments: Map<ExperimentAssignment, String>
    ) : StripeModel

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class CardBrandChoice(
        val eligible: Boolean,
        val preferredNetworks: List<String>,
    ) : StripeModel

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    sealed interface CustomPaymentMethod : StripeModel {
        val type: String

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Parcelize
        data class Available(
            override val type: String,
            val displayName: String,
            val logoUrl: String,
        ) : CustomPaymentMethod

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Parcelize
        data class Unavailable(
            override val type: String,
            val error: String,
        ) : CustomPaymentMethod
    }

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
                    val allowRedisplayOverride: PaymentMethod.AllowRedisplay?,
                    val isPaymentMethodSetAsDefaultEnabled: Boolean,
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
                    val isPaymentMethodSyncDefaultEnabled: Boolean,
                ) : CustomerSheet
            }
        }
    }

    /**
     * Flags declared here will be parsed and include in the [ElementsSession] object.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class Flag(val flagValue: String) {
        ELEMENTS_DISABLE_FC_LITE("elements_disable_fc_lite"),
        ELEMENTS_PREFER_FC_LITE("elements_prefer_fc_lite"),
        ELEMENTS_DISABLE_LINK_GLOBAL_HOLDBACK_LOOKUP("elements_disable_link_global_holdback_lookup"),
        ELEMENTS_ENABLE_LINK_SPM("elements_enable_link_spm"),
        ELEMENTS_ENABLE_PASSIVE_CAPTCHA("elements_enable_passive_captcha")
    }

    /**
     * Experiments declared here will be parsed and include in the [ElementsSession] object.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class ExperimentAssignment(val experimentValue: String) {
        LINK_GLOBAL_HOLD_BACK("link_global_holdback"),
        LINK_AB_TEST("link_ab_test"),
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        fun createFromFallback(
            stripeIntent: StripeIntent,
            sessionsError: Throwable?,
            elementsSessionId: String = UUID.randomUUID().toString(),
        ): ElementsSession {
            return ElementsSession(
                linkSettings = null,
                paymentMethodSpecs = null,
                externalPaymentMethodData = null,
                flags = emptyMap(),
                stripeIntent = stripeIntent,
                orderedPaymentMethodTypesAndWallets = stripeIntent.paymentMethodTypes,
                experimentsData = null,
                customer = null,
                customPaymentMethods = listOf(),
                merchantCountry = null,
                cardBrandChoice = null,
                isGooglePayEnabled = true,
                sessionsError = sessionsError,
                elementsSessionId = elementsSessionId,
                passiveCaptcha = null
            )
        }
    }
}
