package com.stripe.android.connect.example.data.di

import com.stripe.android.connect.example.data.EmbeddedComponentService
import com.stripe.android.connect.example.data.FakeEmbeddedComponentService
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DataModule::class]
)
abstract class TestDataModule {
    @Singleton
    @Binds
    abstract fun bindEmbeddedComponentService(
        impl: FakeEmbeddedComponentService
    ): EmbeddedComponentService
}
