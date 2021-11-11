package com.stripe.android.googlepaylauncher.injection

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.Logger
import com.stripe.android.googlepaylauncher.DefaultGooglePayRepository
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayRepository
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GooglePayLauncherModule {

    // At the point at which the DefaultFlowControllerInitializer is created the configuration
    // is not known.
    @Provides
    @Singleton
    fun provideGooglePayRepositoryFactory(
        appContext: Context,
        logger: Logger
    ): (GooglePayEnvironment) -> GooglePayRepository = { environment ->
        DefaultGooglePayRepository(
            appContext,
            environment,
            GooglePayJsonFactory.BillingAddressParameters(),
            true,
            logger
        )
    }
}
