package com.stripe.android.common.model

import android.os.Parcelable
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class CommonConfiguration(
    val merchantDisplayName: String,
    val customer: PaymentSheet.CustomerConfiguration?,
    val googlePay: PaymentSheet.GooglePayConfiguration?,
    val defaultBillingDetails: PaymentSheet.BillingDetails?,
    val shippingDetails: AddressDetails?,
    val allowsDelayedPaymentMethods: Boolean,
    val allowsPaymentMethodsRequiringShippingAddress: Boolean,
    val appearance: PaymentSheet.Appearance,
    val primaryButtonLabel: String?,
    val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
    val preferredNetworks: List<CardBrand>,
    val allowsRemovalOfLastSavedPaymentMethod: Boolean,
    val paymentMethodOrder: List<String>,
    val externalPaymentMethods: List<String>,
    val cardBrandAcceptance: PaymentSheet.CardBrandAcceptance,
) : Parcelable

internal fun PaymentSheet.Configuration.asCommonConfiguration(): CommonConfiguration = CommonConfiguration(
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
    cardBrandAcceptance = cardBrandAcceptance,
)
