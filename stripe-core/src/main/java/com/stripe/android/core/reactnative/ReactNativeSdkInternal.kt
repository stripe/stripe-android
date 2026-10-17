package com.stripe.android.core.reactnative

import androidx.annotation.RestrictTo

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API should only be used internally by the react-native sdk."
)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Retention(AnnotationRetention.BINARY)
annotation class ReactNativeSdkInternal
