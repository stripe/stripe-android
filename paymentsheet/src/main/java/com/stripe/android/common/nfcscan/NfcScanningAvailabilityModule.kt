package com.stripe.android.common.nfcscan

import com.stripe.android.common.nfcscan.hardware.NfcHardwareDelegateModule
import com.stripe.android.common.nfcscan.security.NfcSecurityModule
import com.stripe.android.ui.core.cardscan.DefaultIsStripeCardScanAvailable
import com.stripe.android.ui.core.cardscan.IsStripeCardScanAvailable
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

    @Binds
    fun bindsIsStripeCardScanAvailable(
        isStripeCardScanAvailable: DefaultIsStripeCardScanAvailable
    ): IsStripeCardScanAvailable
}
