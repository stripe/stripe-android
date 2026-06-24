package com.stripe.android.common.nfcscan

import com.stripe.android.common.nfcscan.security.NfcSecurityModule
import dagger.Binds
import dagger.Module

@Module(includes = [NfcSecurityModule::class])
internal interface NfcScanningAvailabilityModule {
    @Binds
    fun bindsIsNfcScanningAvailable(isNfcScanningAvailable: DefaultIsNfcScanningAvailable): IsNfcScanningAvailable
}
