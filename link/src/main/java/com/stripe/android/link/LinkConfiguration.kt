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
    val customerName: String?,
    val customerEmail: String?,
    val customerPhone: String?,
    val customerBillingCountryCode: String?,
    val shippingValues: Map<IdentifierSpec, String?>?
) : Parcelable
