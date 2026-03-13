package com.stripe.android.ui.core.di

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.DefaultIsStripeCardScanAvailable
import com.stripe.android.ui.core.IsStripeCardScanAvailable
import dagger.Module
import dagger.Provides

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
object CardScanModule {
    @Provides
    fun providesIsStripeCardScanAvailable(): IsStripeCardScanAvailable = DefaultIsStripeCardScanAvailable()
}
