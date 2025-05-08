package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class LinkPaymentDetails(
    val expMonth: Int,
    val expYear: Int,
    val last4: String,
    val brand: CardBrand,
) : Parcelable
