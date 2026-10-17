package com.stripe.android.core.injection

import androidx.annotation.RestrictTo
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.StripeNetworkClient
import dagger.Module
import dagger.Provides
import kotlin.coroutines.CoroutineContext

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
class StripeNetworkClientModule {
    @Provides
    fun provideStripeNetworkClient(
        logger: Logger,
        @IOContext workContext: CoroutineContext,
    ): StripeNetworkClient = DefaultStripeNetworkClient(
        logger = logger,
        workContext = workContext,
    )
}
