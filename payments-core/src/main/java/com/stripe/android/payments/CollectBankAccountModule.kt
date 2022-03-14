package com.stripe.android.payments

import android.app.Application
import android.content.Context
import com.stripe.android.BuildConfig
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.payments.bankaccount.CollectBankAccountContract
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
internal object CollectBankAccountModule {

    @Provides
    fun providesAppContext(application: Application): Context = application

    @Provides
    @Named(PUBLISHABLE_KEY)
    fun providePublishableKey(
        args: CollectBankAccountContract.Args
    ): () -> String = { args.publishableKey }

    @Provides
    @Named(PRODUCT_USAGE)
    fun providesProductUsage(): Set<String> = emptySet() // TODO ensure this is not needed.

    @Provides
    @Named(ENABLE_LOGGING)
    fun providesEnableLogging(): Boolean = BuildConfig.DEBUG
}
