package com.stripe.android.paymentsheet.internal

import android.content.res.ColorStateList
import android.os.Parcelable
import com.stripe.android.paymentsheet.PaymentSheet
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
) : Parcelable {

    fun validate() {
        // These are not localized as they are not intended to be displayed to a user.
        require(merchantDisplayName.isNotBlank()) {
            "merchantDisplayName can't be empty"
        }

        require(customer == null || customer.id.isNotBlank()) {
            "customer.id can't be empty"
        }

        require(customer == null || customer.ephemeralKeySecret.isNotBlank()) {
            "customer.ephemeralKeySecret can't be empty"
        }
    }
}

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
