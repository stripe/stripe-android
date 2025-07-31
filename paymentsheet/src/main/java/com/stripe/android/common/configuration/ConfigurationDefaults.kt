package com.stripe.android.common.configuration

import com.stripe.android.elements.AddressDetails
import com.stripe.android.elements.Appearance
import com.stripe.android.elements.BillingDetails
import com.stripe.android.elements.BillingDetailsCollectionConfiguration
import com.stripe.android.elements.CardBrandAcceptance
import com.stripe.android.elements.CustomerConfiguration
import com.stripe.android.elements.payment.GooglePayConfiguration
import com.stripe.android.elements.payment.LinkConfiguration
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.PaymentMethodLayout

internal object ConfigurationDefaults {
    const val allowsDelayedPaymentMethods: Boolean = true
    const val allowsPaymentMethodsRequiringShippingAddress: Boolean = false
    const val allowsRemovalOfLastSavedPaymentMethod: Boolean = true
    val appearance: Appearance = Appearance()
    val billingDetails: BillingDetails = BillingDetails()
    val billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration =
        BillingDetailsCollectionConfiguration()
    val customer: CustomerConfiguration? = null
    val googlePay: GooglePayConfiguration? = null
    val link: LinkConfiguration = LinkConfiguration()
    const val googlePayEnabled: Boolean = false
    val headerTextForSelectionScreen: String? = null
    val paymentMethodOrder: List<String> = emptyList()
    val preferredNetworks: List<CardBrand> = emptyList()
    val primaryButtonLabel: String? = null
    val shippingDetails: AddressDetails? = null
    val externalPaymentMethods: List<String> = emptyList()
    val paymentMethodLayout: PaymentMethodLayout = PaymentMethodLayout.Automatic
    val cardBrandAcceptance: CardBrandAcceptance = CardBrandAcceptance.All
    val customPaymentMethods: List<PaymentSheet.CustomPaymentMethod> = emptyList()
    val walletButtons: PaymentSheet.WalletButtonsConfiguration = PaymentSheet.WalletButtonsConfiguration()
    val shopPayConfiguration: PaymentSheet.ShopPayConfiguration? = null
    val googlePlacesApiKey: String? = null

    const val embeddedViewDisplaysMandateText: Boolean = true
}
