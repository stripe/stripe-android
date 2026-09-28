package com.stripe.android.core.injection

import androidx.annotation.RestrictTo
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
class CoroutineContextModule {
    @Provides
    @IOContext
    fun provideWorkContext(): CoroutineContext = Dispatchers.IO

    @Provides
    @UIContext
    fun provideUIContext(): CoroutineContext = Dispatchers.Main
}
