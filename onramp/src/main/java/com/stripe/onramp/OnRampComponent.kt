package com.stripe.onramp

import android.app.Application
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.payments.core.injection.StripeRepositoryModule
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
        ResourceRepositoryModule::class,
        CoreCommonModule::class,
        CoroutineContextModule::class,
        OnRampCoordinatorModule::class,
        OnRampRepositoryModule::class,
    ]
)
internal interface OnRampComponent {
    val linkCoordinator: OnRampCoordinator

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun onRampCoordinatorViewModel(viewModel: OnRampCoordinatorViewModel): Builder

        @BindsInstance
        fun linkElementCallbackIdentifier(
            @PaymentElementCallbackIdentifier linkElementCallbackIdentifier: String
        ): Builder

        @BindsInstance
        fun lifecycleOwner(lifecycleOwner: LifecycleOwner): Builder

        @BindsInstance
        fun activityResultRegistryOwner(
            activityResultRegistryOwner: ActivityResultRegistryOwner
        ): Builder

        @BindsInstance
        fun onRampCallbacks(onRampCallbacks: OnRampCoordinator.OnRampCallbacks): Builder

        fun build(): OnRampComponent
    }
}
