package com.stripe.android.link

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.CardBrandFilter
import com.stripe.android.model.LinkMode
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.state.PaymentElementLoader.InitializationMode
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class LinkConfiguration(
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
    val initializationMode: InitializationMode,
    val elementsSessionId: String,
    val linkMode: LinkMode?,
    val allowDefaultOptIn: Boolean,
    val googlePlacesApiKey: String? = null,
) : Parcelable {
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
