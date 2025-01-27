package com.stripe.android.link

import android.os.Parcelable
import com.stripe.android.model.StripeIntent
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
    val useAttestationEndpointsForLink: Boolean,
    val initializationMode: PaymentElementLoader.InitializationMode,
    val elementSessionId: String?,
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
