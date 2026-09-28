package com.stripe.android.core.reactnative

import androidx.annotation.RestrictTo

/**
 * Holds metadata set by the Stripe React Native SDK for inclusion in analytics payloads.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object ReactNativeAnalytics {
    var isNewArchitecture: Boolean? = null
        @ReactNativeSdkInternal set

    var reactNativeVersion: String? = null
        @ReactNativeSdkInternal set
}
