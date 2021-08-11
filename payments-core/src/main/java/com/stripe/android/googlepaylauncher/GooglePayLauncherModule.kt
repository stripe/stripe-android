package com.stripe.android.googlepaylauncher

import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class GooglePayLauncherModule {

    @Provides
    @Singleton
    fun provideGooglePayRepositoryFactory(
        appContext: Context,
    ): (GooglePayEnvironment) -> GooglePayRepository = { environment ->
        DefaultGooglePayRepository(
            appContext,
            environment
        )
    }
}
