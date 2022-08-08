package com.stripe.android.stripecardscan.payment.card

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Card details from the scanner
 */
@Parcelize
data class ScannedCard(
    val pan: String
) : Parcelable
