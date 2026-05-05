package com.stripe.android.common.di

import com.stripe.android.paymentsheet.repositories.ElementsSessionClientParams
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Provider

@Module(includes = [ApplicationIdModule::class, MobileSessionIdModule::class])
internal object ElementsSessionClientParamsModule {
    @Provides
    fun provideElementsSessionClientParams(
        @Named(APPLICATION_ID) appId: String,
        @Named(MOBILE_SESSION_ID) mobileSessionIdProvider: Provider<String>,
    ): ElementsSessionClientParams = ElementsSessionClientParams(
        mobileAppId = appId,
        mobileSessionIdProvider = { mobileSessionIdProvider.get() },
    )
}
