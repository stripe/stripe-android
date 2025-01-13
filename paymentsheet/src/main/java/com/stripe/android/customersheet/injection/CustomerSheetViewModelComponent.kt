package com.stripe.android.customersheet.injection

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetIntegration
import com.stripe.android.customersheet.CustomerSheetViewModel
import com.stripe.android.googlepaylauncher.injection.GooglePayLauncherModule
import com.stripe.android.paymentelement.confirmation.injection.DefaultConfirmationModule
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named

@CustomerSheetViewModelScope
@Component(
    modules = [
        DefaultConfirmationModule::class,
        CustomerSheetViewModelModule::class,
        StripeRepositoryModule::class,
        GooglePayLauncherModule::class,
    ],
)
internal interface CustomerSheetViewModelComponent {
    val viewModel: CustomerSheetViewModel

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun configuration(configuration: CustomerSheet.Configuration): Builder

        @BindsInstance
        fun statusBarColor(@Named(STATUS_BAR_COLOR) statusBarColor: Int?): Builder

        @BindsInstance
        fun integrationType(integrationType: CustomerSheetIntegration.Type): Builder

        @BindsInstance
        fun savedStateHandle(savedStateHandle: SavedStateHandle): Builder

        fun build(): CustomerSheetViewModelComponent
    }
}
