package com.stripe.android

import androidx.annotation.RestrictTo

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API is under construction. It can be changed or removed at any time (use at your own risk)"
)
@Retention(AnnotationRetention.BINARY)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
annotation class ExperimentalCardBrandFilteringApi
