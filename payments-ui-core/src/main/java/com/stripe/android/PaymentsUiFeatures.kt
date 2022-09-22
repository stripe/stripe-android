package com.stripe.android

import androidx.annotation.RestrictTo
import com.stripe.android.features.FeatureAvailability
import com.stripe.android.features.FeatureFlag

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object PaymentsUiFeatures {
    val upi = FeatureFlag("UPI in Payment Sheet", availability = FeatureAvailability.Debug)
}
