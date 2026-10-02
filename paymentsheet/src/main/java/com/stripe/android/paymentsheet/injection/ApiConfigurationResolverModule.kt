package com.stripe.android.paymentsheet.injection

import dagger.Binds
import dagger.Module

@Module
internal interface ApiConfigurationResolverModule {
    @Binds
    fun bindsApiConfigurationResolver(
        resolver: DefaultApiConfigurationResolver,
    ): ApiConfigurationResolver
}
