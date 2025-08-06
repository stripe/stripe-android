package com.stripe.android.common.model

import androidx.annotation.RestrictTo
import com.stripe.android.elements.payment.FlowController
import com.stripe.android.elements.payment.PaymentSheet

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun FlowController.Configuration.asPaymentSheetConfiguration(): PaymentSheet.Configuration {
    return PaymentSheet.Configuration(
        merchantDisplayName = merchantDisplayName,
        customer = customer,
        googlePay = googlePay,
        defaultBillingDetails = defaultBillingDetails,
        shippingDetails = shippingDetails,
        allowsDelayedPaymentMethods = allowsDelayedPaymentMethods,
        allowsPaymentMethodsRequiringShippingAddress = allowsPaymentMethodsRequiringShippingAddress,
        appearance = appearance,
        primaryButtonLabel = primaryButtonLabel,
        billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
        preferredNetworks = preferredNetworks,
        allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod,
        paymentMethodOrder = paymentMethodOrder,
        externalPaymentMethods = externalPaymentMethods,
        paymentMethodLayout = paymentMethodLayout,
        cardBrandAcceptance = cardBrandAcceptance,
        customPaymentMethods = customPaymentMethods,
        link = link,
        shopPayConfiguration = shopPayConfiguration,
        googlePlacesApiKey = googlePlacesApiKey,
        walletButtons = walletButtons,
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun PaymentSheet.Configuration.asFlowControllerConfiguration(): FlowController.Configuration {
    return FlowController.Configuration(
        merchantDisplayName = merchantDisplayName,
        customer = customer,
        googlePay = googlePay,
        defaultBillingDetails = defaultBillingDetails,
        shippingDetails = shippingDetails,
        allowsDelayedPaymentMethods = allowsDelayedPaymentMethods,
        allowsPaymentMethodsRequiringShippingAddress = allowsPaymentMethodsRequiringShippingAddress,
        appearance = appearance,
        primaryButtonLabel = primaryButtonLabel,
        billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
        preferredNetworks = preferredNetworks,
        allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod,
        paymentMethodOrder = paymentMethodOrder,
        externalPaymentMethods = externalPaymentMethods,
        paymentMethodLayout = paymentMethodLayout,
        cardBrandAcceptance = cardBrandAcceptance,
        customPaymentMethods = customPaymentMethods,
        link = link,
        shopPayConfiguration = shopPayConfiguration,
        googlePlacesApiKey = googlePlacesApiKey,
        walletButtons = walletButtons,
    )
}
