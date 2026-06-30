package com.stripe.android.common.nfcscan.tapzone

import android.os.Build
import dagger.Binds
import dagger.Module
import dagger.Provides

@Module
internal interface TapZoneModule {
    @Binds
    fun bindsTapZoneResolver(resolver: DefaultTapZoneResolver): TapZoneResolver

    companion object {
        @DeviceManufacturer
        @Provides
        fun providesDeviceManufacturer(): String = Build.MANUFACTURER

        @DeviceModel
        @Provides
        fun providesDeviceModel(): String = Build.MODEL

        @SdkVersion
        @Provides
        fun providesSdkVersion(): Int = Build.VERSION.SDK_INT
    }
}
