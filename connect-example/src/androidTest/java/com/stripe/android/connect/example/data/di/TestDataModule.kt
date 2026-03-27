package com.stripe.android.connect.example.data.di

import com.stripe.android.connect.example.data.EmbeddedComponentService
import com.stripe.android.connect.example.data.FakeEmbeddedComponentService
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DataModule::class]
)
abstract class TestDataModule {
    @Binds
    abstract fun bindEmbeddedComponentService(
        impl: FakeEmbeddedComponentService
    ): EmbeddedComponentService
}
