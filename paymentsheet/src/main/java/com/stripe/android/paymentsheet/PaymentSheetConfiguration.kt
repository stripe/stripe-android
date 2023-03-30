package com.stripe.android.paymentsheet

import android.content.res.ColorStateList
import android.os.Parcelable
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import kotlinx.parcelize.Parcelize

/** Configuration for [PaymentSheet] **/
@Parcelize
internal data class PaymentSheetConfiguration constructor(
    val merchantDisplayName: String,
    val customer: PaymentSheet.CustomerConfiguration?,
    val googlePay: PaymentSheet.GooglePayConfiguration?,
    val primaryButtonColor: ColorStateList?,
    val defaultBillingDetails: PaymentSheet.BillingDetails?,
    val shippingDetails: AddressDetails?,
    val allowsDelayedPaymentMethods: Boolean,
    val allowsPaymentMethodsRequiringShippingAddress: Boolean,
    val appearance: PaymentSheet.Appearance,
    val primaryButtonLabel: String?,
    val billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration,
) : Parcelable

internal fun PaymentSheet.Configuration.toInternalConfiguration(): PaymentSheetConfiguration {
    return PaymentSheetConfiguration(
        merchantDisplayName = merchantDisplayName,
        customer = customer,
        googlePay = googlePay,
        primaryButtonColor = primaryButtonColor,
        defaultBillingDetails = defaultBillingDetails,
        shippingDetails = shippingDetails,
        allowsDelayedPaymentMethods = allowsDelayedPaymentMethods,
        allowsPaymentMethodsRequiringShippingAddress = allowsPaymentMethodsRequiringShippingAddress,
        appearance = appearance,
        primaryButtonLabel = primaryButtonLabel,
        billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
    )
}

internal fun PaymentSheetConfiguration.toPublicConfiguration(): PaymentSheet.Configuration {
    return PaymentSheet.Configuration(
        merchantDisplayName = merchantDisplayName,
        customer = customer,
        googlePay = googlePay,
        primaryButtonColor = primaryButtonColor,
        defaultBillingDetails = defaultBillingDetails,
        shippingDetails = shippingDetails,
        allowsDelayedPaymentMethods = allowsDelayedPaymentMethods,
        allowsPaymentMethodsRequiringShippingAddress = allowsPaymentMethodsRequiringShippingAddress,
        appearance = appearance,
        primaryButtonLabel = primaryButtonLabel,
        billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
    )
}
