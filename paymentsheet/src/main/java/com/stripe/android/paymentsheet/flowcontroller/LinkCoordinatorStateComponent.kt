package com.stripe.android.paymentsheet.flowcontroller

import android.app.Application
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.injection.PaymentSheetCommonModule
import com.stripe.android.paymentsheet.viewmodels.LinkCoordinatorViewModel
import com.stripe.android.ui.core.forms.resources.injection.ResourceRepositoryModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        StripeRepositoryModule::class,
        PaymentSheetCommonModule::class,
        ResourceRepositoryModule::class,
        CoreCommonModule::class,
        CoroutineContextModule::class,
        LinkCoordinatorModule::class,
    ]
)
internal interface LinkCoordinatorStateComponent {
    val linkCoordinatorComponentBuilder: LinkCoordinatorComponent.Builder

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun linkCoordinatorViewModel(viewModel: LinkCoordinatorViewModel): Builder

        @BindsInstance
        fun linkElementCallbackIdentifier(
            @PaymentElementCallbackIdentifier linkElementCallbackIdentifier: String
        ): Builder

        fun build(): LinkCoordinatorStateComponent
    }
} 