package com.stripe.android.common.nfcscan

import com.stripe.android.common.nfcscan.hardware.NfcHardwareDelegateModule
import dagger.Binds
import dagger.Module

@Module(
    includes = [
        NfcHardwareDelegateModule::class,
    ]
)
internal interface NfcScanningAvailabilityModule {
    @Binds
    fun bindsIsNfcScanningAvailable(isNfcScanningAvailable: DefaultIsNfcScanningAvailable): IsNfcScanningAvailable
}
