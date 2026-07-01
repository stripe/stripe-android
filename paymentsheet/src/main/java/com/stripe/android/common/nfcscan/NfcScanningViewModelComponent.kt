package com.stripe.android.common.nfcscan

import android.app.Application
import com.stripe.android.common.nfcscan.hardware.NfcHardwareDelegateModule
import com.stripe.android.common.nfcscan.scanner.NfcCardScannerModule
import com.stripe.android.common.nfcscan.tapzone.TapZoneModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.ViewModelScope
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

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
        CoroutineContextModule::class,
        NfcHardwareDelegateModule::class,
        NfcCardScannerModule::class,
        TapZoneModule::class,
    ]
)
internal object NfcScanningViewModelModule {
    @Provides
    @ViewModelScope
    fun provideViewModelScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.Main)
    }
}
