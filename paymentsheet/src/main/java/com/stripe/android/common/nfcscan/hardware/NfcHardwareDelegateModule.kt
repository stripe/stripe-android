package com.stripe.android.common.nfcscan.hardware

import dagger.Binds
import dagger.Module

@Module
internal interface NfcHardwareDelegateModule {
    @Binds
    fun bindsNfcHardwareDelegate(
        delegate: DefaultNfcHardwareDelegate
    ): NfcHardwareDelegate
}
