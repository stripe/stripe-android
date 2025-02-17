package com.stripe.android.common.di

import android.app.Application
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
internal object ApplicationIdModule {
    @Provides
    @Named(APPLICATION_ID)
    fun provideApplicationId(
        application: Application
    ): String {
        return application.packageName
    }
}

internal const val APPLICATION_ID = "application_id"
