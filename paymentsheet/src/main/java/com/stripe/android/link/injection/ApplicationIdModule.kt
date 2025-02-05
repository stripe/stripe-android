package com.stripe.android.link.injection

import android.app.Application
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
object ApplicationIdModule {
    @Provides
    @Named(APPLICATION_ID)
    fun provideApplicationId(
        application: Application
    ): String {
        return application.packageName
    }
}