package com.stripe.android.connect.example.data.di

import com.stripe.android.connect.example.data.EmbeddedComponentService
import com.stripe.android.connect.example.data.EmbeddedComponentServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindEmbeddedComponentService(
        impl: EmbeddedComponentServiceImpl
    ): EmbeddedComponentService
}
