package com.stripe.android.googlepaylauncher

import android.content.Context
import com.stripe.android.Logger
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class GooglePayLauncherModule {

    @Provides
    @Singleton
    fun provideGooglePayRepositoryFactory(
        appContext: Context,
        logger: Logger
    ): (GooglePayEnvironment) -> GooglePayRepository = { environment ->
        DefaultGooglePayRepository(
            appContext,
            environment,
            logger
        )
    }
}
