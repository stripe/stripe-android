package com.stripe.android.common.nfcscan.injection

import android.app.Application
import com.stripe.android.common.nfcscan.NfcScanningViewModel
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [NfcScanningViewModelModule::class]
)
internal interface NfcScanningViewModelComponent {
    val viewModel: NfcScanningViewModel

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance
            application: Application,
        ): NfcScanningViewModelComponent
    }
}
