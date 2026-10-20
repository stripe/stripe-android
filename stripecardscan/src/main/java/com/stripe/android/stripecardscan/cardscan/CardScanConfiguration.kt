package com.stripe.android.stripecardscan.cardscan

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.ApiConfiguration
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class CardScanConfiguration(
    val elementsSessionId: String?,
    val apiConfiguration: ApiConfiguration.State? = null,
    val enableMlKitTextRecognition: Boolean = false,
    val disableSsdOcr: Boolean = false,
) : Parcelable
