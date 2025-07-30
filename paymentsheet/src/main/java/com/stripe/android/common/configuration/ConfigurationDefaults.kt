package com.stripe.android.common.configuration

import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.PaymentMethodLayout
import com.stripe.android.paymentsheet.addresselement.AddressDetails

internal object ConfigurationDefaults {
    const val allowsDelayedPaymentMethods: Boolean = true
    const val allowsPaymentMethodsRequiringShippingAddress: Boolean = false
    const val allowsRemovalOfLastSavedPaymentMethod: Boolean = true
    val appearance: PaymentSheet.Appearance = PaymentSheet.Appearance()
    val billingDetails: PaymentSheet.BillingDetails = PaymentSheet.BillingDetails()
    val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration =
        PaymentSheet.BillingDetailsCollectionConfiguration()
    val customer: PaymentSheet.CustomerConfiguration? = null
    val googlePay: PaymentSheet.GooglePayConfiguration? = null
    val link: PaymentSheet.LinkConfiguration = PaymentSheet.LinkConfiguration()
    const val googlePayEnabled: Boolean = false
    val headerTextForSelectionScreen: String? = null
    val paymentMethodOrder: List<String> = emptyList()
    val preferredNetworks: List<CardBrand> = emptyList()
    val primaryButtonLabel: String? = null
    val shippingDetails: AddressDetails? = null
    val externalPaymentMethods: List<String> = emptyList()
    val paymentMethodLayout: PaymentMethodLayout = PaymentMethodLayout.Automatic
    val cardBrandAcceptance: PaymentSheet.CardBrandAcceptance = PaymentSheet.CardBrandAcceptance.All
    val customPaymentMethods: List<PaymentSheet.CustomPaymentMethod> = emptyList()
    val walletButtons: PaymentSheet.WalletButtonsConfiguration = PaymentSheet.WalletButtonsConfiguration()
    val shopPayConfiguration: PaymentSheet.ShopPayConfiguration? = null
    val googlePlacesApiKey: String? = null

    const val embeddedViewDisplaysMandateText: Boolean = true
}
