package com.stripe.android.common.nfcscan

import android.app.Application
import com.stripe.android.common.nfcscan.hardware.NfcHardwareDelegateModule
import com.stripe.android.common.nfcscan.tapzone.TapZoneModule
import dagger.BindsInstance
import dagger.Component
import dagger.Module

@Component(
    modules = [NfcScanningViewModelModule::class]
)
internal interface NfcScanningViewModelComponent {
    val viewModel: NfcScanningViewModel

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance application: Application,
        ): NfcScanningViewModelComponent
    }
}

@Module(
    includes = [
        NfcHardwareDelegateModule::class,
        TapZoneModule::class,
    ]
)
internal interface NfcScanningViewModelModule
