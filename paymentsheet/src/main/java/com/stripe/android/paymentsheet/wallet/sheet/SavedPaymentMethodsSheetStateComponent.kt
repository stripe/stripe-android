package com.stripe.android.paymentsheet.wallet.sheet

import android.content.Context
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.googlepaylauncher.injection.GooglePayLauncherModule
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.PaymentOptionsViewModel
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.injection.PaymentSheetCommonModule
import com.stripe.android.ui.core.forms.resources.injection.ResourceRepositoryModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        StripeRepositoryModule::class,
        PaymentSheetCommonModule::class,
        SavedPaymentMethodsSheetModule::class,
        GooglePayLauncherModule::class,
        CoroutineContextModule::class,
        CoreCommonModule::class,
        ResourceRepositoryModule::class,
        SavedPaymentMethodsSheetAbstractModule::class,
    ]
)
internal interface SavedPaymentMethodsSheetStateComponent {
    val savedPaymentMethodsSheetComponentBuilder: SavedPaymentMethodsSheetComponent.Builder

    fun inject(savedPaymentMethodsSheetViewModel: SavedPaymentMethodsSheetViewModel.Factory)
    fun inject(factory: FormViewModel.Factory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun appContext(appContext: Context): Builder

        @BindsInstance
        fun savedPaymentMethodsViewModel(viewModel: SavedPaymentMethodsViewModel): Builder

        fun build(): SavedPaymentMethodsSheetStateComponent
    }
}
