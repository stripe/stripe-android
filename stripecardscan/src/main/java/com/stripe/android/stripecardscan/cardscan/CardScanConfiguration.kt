package com.stripe.android.stripecardscan.cardscan

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class CardScanConfiguration(
    val elementsSessionId: String?
) : Parcelable
