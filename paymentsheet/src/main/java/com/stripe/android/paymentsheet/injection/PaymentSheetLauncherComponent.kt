package com.stripe.android.paymentsheet.injection

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.common.di.ApplicationIdModule
import com.stripe.android.common.di.MobileSessionIdModule
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.googlepaylauncher.injection.GooglePayLauncherModule
import com.stripe.android.networking.PaymentElementRequestSurfaceModule
import com.stripe.android.paymentelement.confirmation.injection.PaymentElementConfirmationModule
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.PaymentSheetContract
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import com.stripe.android.ui.core.forms.resources.injection.ResourceRepositoryModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        StripeRepositoryModule::class,
        PaymentSheetCommonModule::class,
        PaymentElementRequestSurfaceModule::class,
        PaymentSheetLauncherModule::class,
        GooglePayLauncherModule::class,
        CoroutineContextModule::class,
        CoreCommonModule::class,
        ResourceRepositoryModule::class,
        ApplicationIdModule::class,
        MobileSessionIdModule::class,
        LinkHoldbackExposureModule::class,
        PaymentSheetViewModelModule::class,
        PaymentElementConfirmationModule::class,
    ]
)
internal interface PaymentSheetLauncherComponent {
    val viewModel: PaymentSheetViewModel

    @Component.Factory
    interface Factory {
        fun build(
            @BindsInstance application: Application,
            @BindsInstance handle: SavedStateHandle,
            @BindsInstance starterArgs: PaymentSheetContract.Args,
        ): PaymentSheetLauncherComponent
    }
}
