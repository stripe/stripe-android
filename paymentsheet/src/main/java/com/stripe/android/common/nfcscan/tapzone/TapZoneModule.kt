package com.stripe.android.common.nfcscan.tapzone

import dagger.Binds
import dagger.Module

@Module
internal interface TapZoneModule {
    @Binds
    fun bindsTapZoneResolver(resolver: DefaultTapZoneResolver): TapZoneResolver
}
