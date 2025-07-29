package com.stripe.android.common.model

import com.stripe.android.elements.BillingDetails
import com.stripe.android.elements.BillingDetailsCollectionConfiguration
import com.stripe.android.elements.CardBrandAcceptance
import com.stripe.android.elements.CustomerConfiguration
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails

internal object CommonConfigurationFactory {
    fun create(
        merchantDisplayName: String = "Example, Inc.",
        customer: CustomerConfiguration? = null,
        googlePay: PaymentSheet.GooglePayConfiguration? = null,
        defaultBillingDetails: BillingDetails? = null,
        shippingDetails: AddressDetails? = null,
        allowsDelayedPaymentMethods: Boolean = true,
        allowsPaymentMethodsRequiringShippingAddress: Boolean = true,
        billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration =
            BillingDetailsCollectionConfiguration(),
        preferredNetworks: List<CardBrand> = emptyList(),
        allowsRemovalOfLastSavedPaymentMethod: Boolean = true,
        paymentMethodOrder: List<String> = emptyList(),
        externalPaymentMethods: List<String> = emptyList(),
        customPaymentMethods: List<PaymentSheet.CustomPaymentMethod> = emptyList(),
        cardBrandAcceptance: CardBrandAcceptance = CardBrandAcceptance.all(),
        link: PaymentSheet.LinkConfiguration = PaymentSheet.LinkConfiguration(),
        shopPayConfiguration: PaymentSheet.ShopPayConfiguration? = null,
        googlePlacesApiKey: String? = null,
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
        shopPayConfiguration = shopPayConfiguration,
        googlePlacesApiKey = googlePlacesApiKey,
    )
}
