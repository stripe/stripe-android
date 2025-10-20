package com.stripe.android.networking

import androidx.annotation.RestrictTo
import dagger.Module
import dagger.Provides

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Module
class PaymentElementRequestSurfaceModule {
    @Provides
    fun providesRequestSurface(): RequestSurface = RequestSurface.PaymentElement
}
