package com.stripe.android.common.model

import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails

internal object CommonConfigurationFactory {
    fun create(
        merchantDisplayName: String = "Example, Inc.",
        customer: PaymentSheet.CustomerConfiguration? = null,
        googlePay: PaymentSheet.GooglePayConfiguration? = null,
        defaultBillingDetails: PaymentSheet.BillingDetails? = null,
        shippingDetails: AddressDetails? = null,
        allowsDelayedPaymentMethods: Boolean = true,
        allowsPaymentMethodsRequiringShippingAddress: Boolean = true,
        billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration =
            PaymentSheet.BillingDetailsCollectionConfiguration(),
        preferredNetworks: List<CardBrand> = emptyList(),
        allowsRemovalOfLastSavedPaymentMethod: Boolean = true,
        paymentMethodOrder: List<String> = emptyList(),
        externalPaymentMethods: List<String> = emptyList(),
        customPaymentMethods: List<PaymentSheet.CustomPaymentMethod> = emptyList(),
        cardBrandAcceptance: PaymentSheet.CardBrandAcceptance = PaymentSheet.CardBrandAcceptance.all(),
        link: PaymentSheet.LinkConfiguration = PaymentSheet.LinkConfiguration(),
    ): CommonConfiguration = CommonConfiguration(
        merchantDisplayName = merchantDisplayName,
        customer = customer,
        googlePay = googlePay,
        defaultBillingDetails = defaultBillingDetails,
        shippingDetails = shippingDetails,
        allowsDelayedPaymentMethods = allowsDelayedPaymentMethods,
        allowsPaymentMethodsRequiringShippingAddress = allowsPaymentMethodsRequiringShippingAddress,
        billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
        preferredNetworks = preferredNetworks,
        allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod,
        paymentMethodOrder = paymentMethodOrder,
        externalPaymentMethods = externalPaymentMethods,
        customPaymentMethods = customPaymentMethods,
        cardBrandAcceptance = cardBrandAcceptance,
        link = link,
    )
}
