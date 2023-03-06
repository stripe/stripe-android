package com.stripe.android.paymentsheet.flowcontroller

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
        FlowControllerModule::class,
        GooglePayLauncherModule::class,
        CoroutineContextModule::class,
        CoreCommonModule::class,
        ResourceRepositoryModule::class,
    ]
)
internal interface FlowControllerStateComponent {
    val flowControllerComponentBuilder: FlowControllerComponent.Builder

    fun inject(paymentOptionsViewModel: PaymentOptionsViewModel.Factory)
    fun inject(factory: FormViewModel.Factory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun appContext(appContext: Context): Builder

        @BindsInstance
        fun flowControllerViewModel(viewModel: FlowControllerViewModel): Builder

        fun build(): FlowControllerStateComponent
    }
}
