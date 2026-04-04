package com.stripe.android.core.frauddetection

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface FraudDetectionErrorReporter {
    fun reportFraudDetectionError(error: Throwable)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface FraudDetectionEnabledProvider {
    fun provideFraudDetectionEnabled(): Boolean
}
