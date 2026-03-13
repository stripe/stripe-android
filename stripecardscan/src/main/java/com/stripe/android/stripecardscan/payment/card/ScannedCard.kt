package com.stripe.android.stripecardscan.payment.card

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Card details from the scanner
 */
@Parcelize
@Deprecated("This is deprecated and will be removed in a future release.")
data class ScannedCard(
    val pan: String
) : Parcelable
