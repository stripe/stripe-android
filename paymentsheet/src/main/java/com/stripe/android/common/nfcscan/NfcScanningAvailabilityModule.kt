package com.stripe.android.common.nfcscan

import com.stripe.android.common.nfcscan.hardware.NfcHardwareDelegateModule
import com.stripe.android.common.nfcscan.security.NfcSecurityModule
import dagger.Binds
import dagger.Module

@Module(
    includes = [
        NfcSecurityModule::class,
        NfcHardwareDelegateModule::class,
    ]
)
internal interface NfcScanningAvailabilityModule {
    @Binds
    fun bindsIsNfcScanningAvailable(isNfcScanningAvailable: DefaultIsNfcScanningAvailable): IsNfcScanningAvailable
}
