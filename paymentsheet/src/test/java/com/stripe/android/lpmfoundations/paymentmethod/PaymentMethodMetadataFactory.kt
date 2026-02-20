package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.CardBrandFilter
import com.stripe.android.CardFundingFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PassiveCaptchaParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.model.PaymentMethodIncentive
import com.stripe.android.paymentsheet.state.LinkStateResult
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.ExternalPaymentMethodSpec
import com.stripe.android.ui.core.elements.LpmSerializer
import com.stripe.android.ui.core.elements.SharedDataSpec

internal object PaymentMethodMetadataFactory {
    fun create(
        stripeIntent: StripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration =
            PaymentSheet.BillingDetailsCollectionConfiguration(),
        allowsDelayedPaymentMethods: Boolean = true,
        allowsPaymentMethodsRequiringShippingAddress: Boolean = false,
        allowsLinkInSavedPaymentMethods: Boolean = false,
        availableWallets: List<WalletType> = listOf(WalletType.GooglePay),
        paymentMethodOrder: List<String> = emptyList(),
        shippingDetails: AddressDetails? = null,
        cbcEligibility: CardBrandChoiceEligibility = CardBrandChoiceEligibility.Ineligible,
        hasCustomerConfiguration: Boolean = false,
        sharedDataSpecs: List<SharedDataSpec> = createSharedDataSpecs(),
        externalPaymentMethodSpecs: List<ExternalPaymentMethodSpec> = emptyList(),
        displayableCustomPaymentMethods: List<DisplayableCustomPaymentMethod> = emptyList(),
        isGooglePayReady: Boolean = false,
        linkConfiguration: PaymentSheet.LinkConfiguration = PaymentSheet.LinkConfiguration(),
        linkMode: LinkMode? = LinkMode.LinkPaymentMethod,
        linkState: LinkStateResult? = null,
        cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
        cardFundingFilter: CardFundingFilter = DefaultCardFundingFilter,
        defaultBillingDetails: PaymentSheet.BillingDetails = PaymentSheet.BillingDetails(),
        paymentMethodIncentive: PaymentMethodIncentive? = null,
        isPaymentMethodSetAsDefaultEnabled: Boolean = IS_PAYMENT_METHOD_SET_AS_DEFAULT_ENABLED_DEFAULT_VALUE,
        financialConnectionsAvailability: FinancialConnectionsAvailability? = FinancialConnectionsAvailability.Lite,
        customerMetadataPermissions: CustomerMetadata.Permissions =
            PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA_PERMISSIONS,
        customerSessionClientSecret: String? = null,
        termsDisplay: Map<PaymentMethod.Type, PaymentSheet.TermsDisplay> = emptyMap(),
        forceSetupFutureUseBehaviorAndNewMandate: Boolean = false,
        passiveCaptchaParams: PassiveCaptchaParams? = null,
        openCardScanAutomatically: Boolean = false,
        clientAttributionMetadata: ClientAttributionMetadata? = null,
        attestOnIntentConfirmation: Boolean = false,
        appearance: PaymentSheet.Appearance = PaymentSheet.Appearance(),
        onBehalfOf: String? = null,
        integrationMetadata: IntegrationMetadata = stripeIntent.integrationMetadata(),
        sellerBusinessName: String? = null,
        analyticsMetadata: AnalyticsMetadata = AnalyticsMetadata(emptyMap()),
        isTapToAddSupported: Boolean = false,
        experimentsData: ElementsSession.ExperimentsData? = null,
    ): PaymentMethodMetadata {
        return PaymentMethodMetadata(
            stripeIntent = stripeIntent,
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
            allowsDelayedPaymentMethods = allowsDelayedPaymentMethods,
            allowsPaymentMethodsRequiringShippingAddress = allowsPaymentMethodsRequiringShippingAddress,
            allowsLinkInSavedPaymentMethods = allowsLinkInSavedPaymentMethods,
            availableWallets = availableWallets,
            paymentMethodOrder = paymentMethodOrder,
            cbcEligibility = cbcEligibility,
            merchantName = PaymentSheetFixtures.MERCHANT_DISPLAY_NAME,
            sellerBusinessName = sellerBusinessName,
            defaultBillingDetails = defaultBillingDetails,
            shippingDetails = shippingDetails,
            customerMetadata = if (hasCustomerConfiguration) {
                PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA.copy(
                    isPaymentMethodSetAsDefaultEnabled = isPaymentMethodSetAsDefaultEnabled,
                    permissions = customerMetadataPermissions,
                    customerSessionClientSecret = customerSessionClientSecret,
                )
            } else {
                null
            },
            sharedDataSpecs = sharedDataSpecs,
            externalPaymentMethodSpecs = externalPaymentMethodSpecs,
            displayableCustomPaymentMethods = displayableCustomPaymentMethods,
            isGooglePayReady = isGooglePayReady,
            linkConfiguration = linkConfiguration,
            linkMode = linkMode,
            linkStateResult = linkState,
            cardBrandFilter = cardBrandFilter,
            cardFundingFilter = cardFundingFilter,
            paymentMethodIncentive = paymentMethodIncentive,
            financialConnectionsAvailability = financialConnectionsAvailability,
            termsDisplay = termsDisplay,
            forceSetupFutureUseBehaviorAndNewMandate = forceSetupFutureUseBehaviorAndNewMandate,
            passiveCaptchaParams = passiveCaptchaParams,
            openCardScanAutomatically = openCardScanAutomatically,
            clientAttributionMetadata =
            clientAttributionMetadata ?: PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA,
            attestOnIntentConfirmation = attestOnIntentConfirmation,
            appearance = appearance,
            onBehalfOf = onBehalfOf,
            integrationMetadata = integrationMetadata,
            analyticsMetadata = analyticsMetadata,
            isTapToAddSupported = isTapToAddSupported,
            experimentsData = experimentsData,
        )
    }

    private fun createSharedDataSpecs(): List<SharedDataSpec> {
        val inputStream = PaymentMethodMetadataFactory::class.java.classLoader!!.getResourceAsStream("lpms.json")
        val specsString = inputStream.bufferedReader().use { it.readText() }
        return LpmSerializer.deserializeList(specsString).getOrThrow()
    }

    private fun StripeIntent.integrationMetadata(): IntegrationMetadata {
        clientSecret?.let { return IntegrationMetadata.IntentFirst(it) }
        return when (this) {
            is PaymentIntent -> {
                IntegrationMetadata.DeferredIntent.WithPaymentMethod(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = amount ?: 5000,
                            currency = currency ?: "usd"
                        )
                    )
                )
            }
            is SetupIntent -> {
                IntegrationMetadata.DeferredIntent.WithPaymentMethod(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        mode = PaymentSheet.IntentConfiguration.Mode.Setup(
                            currency = "usd"
                        )
                    )
                )
            }
        }
    }
}
