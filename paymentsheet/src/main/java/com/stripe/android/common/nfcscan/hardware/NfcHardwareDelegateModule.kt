package com.stripe.android.common.nfcscan.hardware

import android.app.Application
import dagger.Module
import dagger.Provides

@Module
internal class NfcHardwareDelegateModule {
    @Provides
    fun providesNfcHardwareDelegate(
        application: Application
    ): NfcHardwareDelegate = NfcHardwareDelegate.create(application)
}
