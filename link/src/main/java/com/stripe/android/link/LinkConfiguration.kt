package com.stripe.android.link

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.model.StripeIntent
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class LinkConfiguration(
    val stripeIntent: StripeIntent,
    val signupMode: LinkSignupMode?,
    val merchantName: String,
    val merchantCountryCode: String?,
    val customerInfo: CustomerInfo,
    val shippingValues: Map<IdentifierSpec, String?>?,
    val passthroughModeEnabled: Boolean,
) : Parcelable {

    val showOptionalLabel: Boolean
        get() = signupMode == LinkSignupMode.AlongsideSaveForFutureUse

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class CustomerInfo(
        val name: String?,
        val email: String?,
        val phone: String?,
        val billingCountryCode: String?,
        val shouldPrefill: Boolean,
    ) : Parcelable
}
