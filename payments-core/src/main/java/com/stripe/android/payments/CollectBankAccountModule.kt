package com.stripe.android.payments

import com.stripe.android.BuildConfig
import com.stripe.android.core.injection.ENABLE_LOGGING
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
internal object CollectBankAccountModule {

    @Provides
    @Named(ENABLE_LOGGING)
    fun providesEnableLogging(): Boolean = BuildConfig.DEBUG
}
