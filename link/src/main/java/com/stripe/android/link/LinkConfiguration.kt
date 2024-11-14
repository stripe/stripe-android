package com.stripe.android.link

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.model.StripeIntent
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class LinkConfiguration(
    val stripeIntent: StripeIntent,
    val merchantName: String,
    val merchantCountryCode: String?,
    val customerInfo: CustomerInfo,
    val shippingValues: Map<IdentifierSpec, String?>?,
    val passthroughModeEnabled: Boolean,
    val flags: Map<String, Boolean>,
    val cardBrandChoice: CardBrandChoice?,
) : Parcelable {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class CustomerInfo(
        val name: String?,
        val email: String?,
        val phone: String?,
        val billingCountryCode: String?,
    ) : Parcelable

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class CardBrandChoice(
        val eligible: Boolean,
        val preferredNetworks: List<String>,
    ) : Parcelable
}
