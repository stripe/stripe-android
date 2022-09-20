package com.stripe.android.paymentsheet

import androidx.annotation.RestrictTo
import androidx.compose.runtime.mutableStateListOf
import com.stripe.android.features.FeatureAvailability
import com.stripe.android.features.FeatureFlag

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object PaymentSheetFeatures {
    val upi = FeatureFlag("UPI in Payment Sheet", availability = FeatureAvailability.Debug)
    val link = FeatureFlag("Link in Payment Sheet", availability = FeatureAvailability.Debug)

    val all = mutableStateListOf(link, upi)
}
