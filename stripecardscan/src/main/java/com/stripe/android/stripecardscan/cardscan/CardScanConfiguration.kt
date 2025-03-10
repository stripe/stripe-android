package com.stripe.android.stripecardscan.cardscan

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CardScanConfiguration(
    val sessionId: String?
) : Parcelable
