package com.stripe.android.connect.example.di

import android.content.Context
import com.stripe.android.connect.example.data.EmbeddedComponentManagerWrapper
import com.stripe.android.connect.example.data.EmbeddedComponentService
import com.stripe.android.connect.example.data.SettingsService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    @Provides
    @Singleton
    fun provideSettingsService(@ApplicationContext context: Context): SettingsService {
        return SettingsService(context)
    }

    @Provides
    @Singleton
    fun provideEmbeddedComponentService(settingsService: SettingsService): EmbeddedComponentService {
        return EmbeddedComponentService(settingsService)
    }

    @Provides
    @Singleton
    fun provideEmbeddedComponentManagerWrapper(
        embeddedComponentService: EmbeddedComponentService,
        settingsService: SettingsService,
    ): EmbeddedComponentManagerWrapper {
        return EmbeddedComponentManagerWrapper(
            embeddedComponentService = embeddedComponentService,
            settingsService = settingsService
        )
    }
}
