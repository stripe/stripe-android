package com.stripe.android.payments.core.injection

import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.UIContext
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Module
class CoroutineContextModule {
    @Provides
    @Singleton
    @IOContext
    fun provideWorkContext(): CoroutineContext = Dispatchers.IO

    @Provides
    @Singleton
    @UIContext
    fun provideUIContext(): CoroutineContext = Dispatchers.Main
}
