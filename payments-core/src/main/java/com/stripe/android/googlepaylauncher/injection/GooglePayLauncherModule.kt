package com.stripe.android.googlepaylauncher.injection

import androidx.annotation.RestrictTo
import dagger.Binds
import dagger.Module

@Module
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface GooglePayLauncherModule {

    // At the point at which the DefaultFlowControllerInitializer is created the configuration
    // is not known.
    @Binds
    fun bindGooglePayRepositoryFactory(
        factory: DefaultGooglePayRepositoryFactory
    ): GooglePayRepositoryFactory
}
