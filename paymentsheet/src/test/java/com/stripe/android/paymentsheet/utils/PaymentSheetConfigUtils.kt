package com.stripe.android.paymentsheet.utils

import com.stripe.android.ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.GooglePayConfiguration
import com.stripe.android.paymentsheet.PaymentSheet.GooglePayConfiguration.ButtonType
import com.stripe.android.paymentsheet.PaymentSheet.GooglePayConfiguration.Environment

@OptIn(ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi::class)
@Suppress("DEPRECATION")
fun PaymentSheet.Configuration.prefilledBuilder() =
    PaymentSheet.Configuration.Builder(merchantDisplayName)
        .customer(customer)
        .googlePay(googlePay)
        .primaryButtonColor(primaryButtonColor)
        .defaultBillingDetails(defaultBillingDetails)
        .shippingDetails(shippingDetails)
        .allowsDelayedPaymentMethods(allowsDelayedPaymentMethods)
        .allowsPaymentMethodsRequiringShippingAddress(allowsPaymentMethodsRequiringShippingAddress)
        .appearance(appearance)
        .billingDetailsCollectionConfiguration(billingDetailsCollectionConfiguration)
        .preferredNetworks(preferredNetworks)
        .allowsRemovalOfLastSavedPaymentMethod(allowsRemovalOfLastSavedPaymentMethod)
        .preferredNetworks(preferredNetworks)
        .paymentMethodOrder(paymentMethodOrder)
        .externalPaymentMethods(externalPaymentMethods)
        .paymentMethodLayout(paymentMethodLayout)
        .cardBrandAcceptance(cardBrandAcceptance)
        .apply {
            primaryButtonLabel?.let {
                primaryButtonLabel(it)
            }
        }

fun GooglePayConfiguration.prefillCreate(
    environment: Environment = this.environment,
    countryCode: String = this.countryCode,
    currencyCode: String? = this.currencyCode,
    amount: Long? = this.amount,
    label: String? = this.label,
    buttonType: ButtonType = this.buttonType
): GooglePayConfiguration {
    return GooglePayConfiguration(
        environment = environment,
        countryCode = countryCode,
        currencyCode = currencyCode,
        amount = amount,
        label = label,
        buttonType = buttonType,
    )
}
