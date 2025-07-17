package com.stripe.android.crypto.onramp.di

import android.app.Application
import androidx.activity.result.ActivityResultRegistryOwner
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.crypto.onramp.OnrampCoordinator
import com.stripe.android.crypto.onramp.model.OnrampCallbacks
import com.stripe.android.crypto.onramp.viewmodels.OnrampCoordinatorViewModel
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.ui.core.forms.resources.injection.ResourceRepositoryModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        StripeRepositoryModule::class,
        ResourceRepositoryModule::class,
        CoreCommonModule::class,
        CoroutineContextModule::class,
    ]
)
internal interface OnrampComponent {
    val onrampCoordinator: OnrampCoordinator

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun onRampCoordinatorViewModel(viewModel: OnrampCoordinatorViewModel): Builder

        @BindsInstance
        fun linkElementCallbackIdentifier(
            @PaymentElementCallbackIdentifier linkElementCallbackIdentifier: String
        ): Builder

        @BindsInstance
        fun activityResultRegistryOwner(
            activityResultRegistryOwner: ActivityResultRegistryOwner
        ): Builder

        @BindsInstance
        fun onrampCallbacks(onrampCallbacks: OnrampCallbacks): Builder

        fun build(): OnrampComponent
    }
}
