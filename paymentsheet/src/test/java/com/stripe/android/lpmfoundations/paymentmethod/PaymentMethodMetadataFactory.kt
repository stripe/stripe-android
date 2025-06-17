package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.common.model.SHOP_PAY_CONFIGURATION
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.model.PaymentMethodIncentive
import com.stripe.android.paymentsheet.state.LinkState
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
        paymentMethodSaveConsentBehavior: PaymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
        linkConfiguration: PaymentSheet.LinkConfiguration = PaymentSheet.LinkConfiguration(),
        linkMode: LinkMode? = LinkMode.LinkPaymentMethod,
        linkState: LinkState? = null,
        cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
        defaultBillingDetails: PaymentSheet.BillingDetails = PaymentSheet.BillingDetails(),
        paymentMethodIncentive: PaymentMethodIncentive? = null,
        isPaymentMethodSetAsDefaultEnabled: Boolean = IS_PAYMENT_METHOD_SET_AS_DEFAULT_ENABLED_DEFAULT_VALUE,
        elementsSessionId: String = "session_1234",
        financialConnectionsAvailability: FinancialConnectionsAvailability? = FinancialConnectionsAvailability.Lite,
        customerMetadataPermissions: CustomerMetadata.Permissions =
            PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA_PERMISSIONS,
        shopPayConfiguration: PaymentSheet.ShopPayConfiguration = SHOP_PAY_CONFIGURATION
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
            defaultBillingDetails = defaultBillingDetails,
            shippingDetails = shippingDetails,
            customerMetadata = CustomerMetadata(
                hasCustomerConfiguration = hasCustomerConfiguration,
                isPaymentMethodSetAsDefaultEnabled = isPaymentMethodSetAsDefaultEnabled,
                permissions = customerMetadataPermissions
            ),
            sharedDataSpecs = sharedDataSpecs,
            paymentMethodSaveConsentBehavior = paymentMethodSaveConsentBehavior,
            externalPaymentMethodSpecs = externalPaymentMethodSpecs,
            displayableCustomPaymentMethods = displayableCustomPaymentMethods,
            isGooglePayReady = isGooglePayReady,
            linkConfiguration = linkConfiguration,
            linkMode = linkMode,
            linkState = linkState,
            cardBrandFilter = cardBrandFilter,
            paymentMethodIncentive = paymentMethodIncentive,
            elementsSessionId = elementsSessionId,
            financialConnectionsAvailability = financialConnectionsAvailability,
            shopPayConfiguration = shopPayConfiguration
        )
    }

    private fun createSharedDataSpecs(): List<SharedDataSpec> {
        val inputStream = PaymentMethodMetadataFactory::class.java.classLoader!!.getResourceAsStream("lpms.json")
        val specsString = inputStream.bufferedReader().use { it.readText() }
        return LpmSerializer.deserializeList(specsString).getOrThrow()
    }
}
