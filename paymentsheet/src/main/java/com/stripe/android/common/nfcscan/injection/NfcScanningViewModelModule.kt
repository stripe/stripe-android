package com.stripe.android.common.nfcscan.injection

import android.app.Application
import android.content.Context
import android.os.Build
import com.stripe.android.common.nfcscan.apdu.DefaultIsoCardReader
import com.stripe.android.common.nfcscan.apdu.DefaultIsoNfcTagTransceiver
import com.stripe.android.common.nfcscan.apdu.IsoCardReader
import com.stripe.android.common.nfcscan.apdu.IsoNfcTagTransceiver
import com.stripe.android.common.nfcscan.hardware.DefaultNfcHardwareDelegate
import com.stripe.android.common.nfcscan.hardware.NfcHardwareDelegate
import com.stripe.android.common.nfcscan.DefaultNfcCardScanner
import com.stripe.android.common.nfcscan.NfcCardScanner
import com.stripe.android.common.nfcscan.apdu.DefaultIsoCardDataParser
import com.stripe.android.common.nfcscan.apdu.IsoCardDataParser
import com.stripe.android.common.nfcscan.tapzone.DefaultTapZoneResolver
import com.stripe.android.common.nfcscan.tapzone.TapZoneResolver
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.ViewModelScope
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module(
    includes = [CoroutineContextModule::class]
)
internal interface NfcScanningViewModelModule {
    @Binds
    fun bindsNfcCardScanner(scanner: DefaultNfcCardScanner): NfcCardScanner

    @Binds
    fun bindsIsoNfcTagTransceiverFactory(
        transceiver: DefaultIsoNfcTagTransceiver.Factory
    ): IsoNfcTagTransceiver.Factory

    @Binds
    fun bindsTapZoneResolver(resolver: DefaultTapZoneResolver): TapZoneResolver

    @Binds
    fun bindsNfcHardwareDelegate(delegate: DefaultNfcHardwareDelegate): NfcHardwareDelegate

    @Binds
    fun bindsIsoCardReader(reader: DefaultIsoCardReader): IsoCardReader

    @Binds
    fun bindsIsoCardDataParser(reader: DefaultIsoCardDataParser): IsoCardDataParser

    companion object {
        @Provides
        fun providesApplicationContext(application: Application): Context = application.applicationContext

        @DeviceManufacturer
        @Provides
        fun providesDeviceManufacturer(): String = Build.MANUFACTURER

        @DeviceModel
        @Provides
        fun providesDeviceModel(): String = Build.MODEL

        @SdkVersion
        @Provides
        fun providesSdkVersion(): Int = Build.VERSION.SDK_INT

        @Provides
        @ViewModelScope
        fun provideViewModelScope(): CoroutineScope {
            return CoroutineScope(Dispatchers.Main)
        }
    }
}
