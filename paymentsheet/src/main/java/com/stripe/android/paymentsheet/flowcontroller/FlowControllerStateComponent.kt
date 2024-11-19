package com.stripe.android.paymentsheet.flowcontroller

import android.app.Application
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.googlepaylauncher.injection.GooglePayLauncherModule
import com.stripe.android.paymentelement.confirmation.BacsConfirmationModule
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.ConfirmationModule
import com.stripe.android.paymentelement.confirmation.ExternalPaymentMethodConfirmationModule
import com.stripe.android.paymentelement.confirmation.GooglePayConfirmationModule
import com.stripe.android.paymentelement.confirmation.IntentConfirmationModule
import com.stripe.android.paymentelement.confirmation.STATUS_BAR_COLOR_PROVIDER
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.PaymentOptionsViewModel
import com.stripe.android.paymentsheet.injection.PaymentSheetCommonModule
import com.stripe.android.ui.core.forms.resources.injection.ResourceRepositoryModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        StripeRepositoryModule::class,
        ExternalPaymentMethodConfirmationModule::class,
        GooglePayConfirmationModule::class,
        BacsConfirmationModule::class,
        IntentConfirmationModule::class,
        ConfirmationModule::class,
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
    val confirmationHandler: ConfirmationHandler

    fun inject(paymentOptionsViewModel: PaymentOptionsViewModel.Factory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun statusBarColor(
            @Named(STATUS_BAR_COLOR_PROVIDER) statusBarColor: () -> Int?
        ): Builder

        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun flowControllerViewModel(viewModel: FlowControllerViewModel): Builder

        fun build(): FlowControllerStateComponent
    }
}
