package com.stripe.android.paymentsheet.flowcontroller

import android.app.Application
import com.stripe.android.common.di.ApplicationIdModule
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.googlepaylauncher.injection.GooglePayLauncherModule
import com.stripe.android.link.injection.LinkExtrasModule
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.injection.ExtendedPaymentElementConfirmationModule
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.LinkHandler
import com.stripe.android.paymentsheet.PaymentOptionsViewModel
import com.stripe.android.paymentsheet.injection.PaymentSheetCommonModule
import com.stripe.android.ui.core.di.CardScanModule
import com.stripe.android.ui.core.forms.resources.injection.ResourceRepositoryModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        StripeRepositoryModule::class,
        ExtendedPaymentElementConfirmationModule::class,
        PaymentSheetCommonModule::class,
        FlowControllerModule::class,
        GooglePayLauncherModule::class,
        CoroutineContextModule::class,
        CoreCommonModule::class,
        ResourceRepositoryModule::class,
        ApplicationIdModule::class,
        CardScanModule::class,
        LinkExtrasModule::class
    ]
)
internal interface FlowControllerStateComponent {
    val flowControllerComponentBuilder: FlowControllerComponent.Builder
    val confirmationHandler: ConfirmationHandler
    val linkHandler: LinkHandler

    fun inject(paymentOptionsViewModel: PaymentOptionsViewModel.Factory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun statusBarColor(
            @Named(STATUS_BAR_COLOR) statusBarColor: Int?
        ): Builder

        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun paymentElementCallbackIdentifier(
            @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: String
        ): Builder

        @BindsInstance
        fun flowControllerViewModel(viewModel: FlowControllerViewModel): Builder

        fun build(): FlowControllerStateComponent
    }
}
