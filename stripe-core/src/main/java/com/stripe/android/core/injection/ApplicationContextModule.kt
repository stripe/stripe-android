package com.stripe.android.core.injection

import android.app.Application
import androidx.annotation.RestrictTo
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
class ApplicationContextModule {
    @Singleton
    @ApplicationContext
    @Provides
    fun providesApplicationContext(application: Application) = application.applicationContext
}
