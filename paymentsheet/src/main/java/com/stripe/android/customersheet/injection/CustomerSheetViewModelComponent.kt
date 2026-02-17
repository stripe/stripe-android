package com.stripe.android.customersheet.injection

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.common.di.ApplicationIdModule
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetIntegration
import com.stripe.android.customersheet.CustomerSheetViewModel
import com.stripe.android.googlepaylauncher.injection.GooglePayLauncherModule
import com.stripe.android.networking.PaymentElementRequestSurfaceModule
import com.stripe.android.paymentelement.confirmation.injection.CustomerSheetConfirmationModule
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named

@CustomerSheetViewModelScope
@Component(
    modules = [
        ApplicationIdModule::class,
        CustomerSheetConfirmationModule::class,
        CustomerSheetViewModelModule::class,
        StripeRepositoryModule::class,
        PaymentElementRequestSurfaceModule::class,
        GooglePayLauncherModule::class,
    ],
)
internal interface CustomerSheetViewModelComponent {
    val viewModel: CustomerSheetViewModel

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance
            application: Application,
            @BindsInstance
            configuration: CustomerSheet.Configuration,
            @BindsInstance
            @Named(STATUS_BAR_COLOR)
            statusBarColor: Int?,
            @BindsInstance
            integrationType: CustomerSheetIntegration.Type,
            @BindsInstance
            savedStateHandle: SavedStateHandle,
        ): CustomerSheetViewModelComponent
    }
}
