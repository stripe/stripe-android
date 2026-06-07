package com.stripe.android.common.nfcscan.injection

import android.app.Application
import android.content.Context
import android.os.Build
import com.stripe.android.common.nfcscan.hardware.DefaultNfcHardwareDelegate
import com.stripe.android.common.nfcscan.hardware.NfcHardwareDelegate
import com.stripe.android.common.nfcscan.tapzone.DefaultTapZoneResolver
import com.stripe.android.common.nfcscan.tapzone.TapZoneResolver
import dagger.Binds
import dagger.Module
import dagger.Provides

@Module
internal interface NfcScanningViewModelModule {
    @Binds
    fun bindsTapZoneResolver(resolver: DefaultTapZoneResolver): TapZoneResolver

    @Binds
    fun bindsNfcHardwareDelegate(delegate: DefaultNfcHardwareDelegate): NfcHardwareDelegate

    companion object {
        @Provides
        fun providesApplicationContext(application: Application): Context = application.applicationContext

        @DeviceManufacturer
        @Provides
        fun providesDeviceManufacturer(): String = Build.MANUFACTURER

        @DeviceModel
        @Provides
        fun providesDeviceModel(): String = Build.MODEL
    }
}
