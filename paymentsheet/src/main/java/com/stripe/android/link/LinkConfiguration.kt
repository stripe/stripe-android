package com.stripe.android.link

import android.os.Parcelable
import com.stripe.android.CardBrandFilter
import com.stripe.android.model.LinkMode
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class LinkConfiguration(
    val stripeIntent: StripeIntent,
    val merchantName: String,
    val merchantCountryCode: String?,
    val customerInfo: CustomerInfo,
    val shippingDetails: AddressDetails?,
    val passthroughModeEnabled: Boolean,
    val flags: Map<String, Boolean>,
    val cardBrandChoice: CardBrandChoice?,
    val cardBrandFilter: CardBrandFilter,
    val financialConnectionsAvailability: FinancialConnectionsAvailability?,
    val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
    val defaultBillingDetails: PaymentSheet.BillingDetails?,
    val useAttestationEndpointsForLink: Boolean,
    val suppress2faModal: Boolean,
    val disableRuxInFlowController: Boolean,
    val initializationMode: PaymentElementLoader.InitializationMode,
    val elementsSessionId: String,
    val linkMode: LinkMode?,
    val allowDefaultOptIn: Boolean,
    val googlePlacesApiKey: String? = null,
    val collectMissingBillingDetailsForExistingPaymentMethods: Boolean,
    val allowUserEmailEdits: Boolean,
    val enableDisplayableDefaultValuesInEce: Boolean,
    val skipWalletInFlowController: Boolean,
    val linkAppearance: LinkAppearance?,
    val linkSignUpOptInFeatureEnabled: Boolean,
    val linkSignUpOptInInitialValue: Boolean,
    private val customerId: String?
) : Parcelable {

    val customerIdForEceDefaultValues: String?
        get() = if (enableDisplayableDefaultValuesInEce) customerId else null

    val enableLinkPaymentSelectionHint: Boolean
        get() = flags["link_show_prefer_debit_card_hint"] == true

    @Parcelize
    data class CustomerInfo(
        val name: String?,
        val email: String?,
        val phone: String?,
        val billingCountryCode: String?,
    ) : Parcelable

    @Parcelize
    data class CardBrandChoice(
        val eligible: Boolean,
        val preferredNetworks: List<String>,
    ) : Parcelable
}
