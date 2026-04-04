package com.stripe.android.core.injection

import androidx.annotation.RestrictTo
import androidx.core.os.LocaleListCompat
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import dagger.Module
import dagger.Provides
import javax.inject.Named

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
class CoreCommonModule {
    @Provides
    fun provideLogger(@Named(ENABLE_LOGGING) enableLogging: Boolean) =
        Logger.getInstance(enableLogging)

    @Provides
    fun provideLocale() =
        LocaleListCompat.getAdjustedDefault().takeUnless { it.isEmpty }?.get(0)

    @Provides
    fun provideApiRequestOptions(
        @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
        @Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider: () -> String?
    ) = ApiRequest.Options(
        publishableKeyProvider = publishableKeyProvider,
        stripeAccountIdProvider = stripeAccountIdProvider
    )
}
