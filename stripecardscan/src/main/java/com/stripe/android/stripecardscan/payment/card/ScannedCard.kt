package com.stripe.android.stripecardscan.payment.card

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

/**
 * Card details from the scanner
 */
@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ScannedCard(
    val pan: String,
    val expiryMonth: Int? = null,
    val expiryYear: Int? = null,
) : Parcelable
