package com.stripe.android.paymentsheet.flowcontroller

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.paymentsheet.model.PaymentOption
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent(
    modules = [
        LinkCoordinatorModule::class,
    ]
)
internal interface LinkCoordinatorComponent {
    val linkCoordinator: DefaultLinkCoordinator

    @Subcomponent.Builder
    interface Builder {
        @BindsInstance
        fun lifecycleOwner(lifecycleOwner: LifecycleOwner): Builder

        @BindsInstance
        fun activityResultCaller(activityResultCaller: ActivityResultCaller): Builder

        @BindsInstance
        fun activityResultRegistryOwner(activityResultRegistryOwner: ActivityResultRegistryOwner): Builder

        @BindsInstance
        fun paymentOptionCallback(paymentOptionCallback: (PaymentOption?) -> Unit): Builder

        @BindsInstance
        fun linkElementCallbackIdentifier(linkElementCallbackIdentifier: String): Builder

        fun build(): LinkCoordinatorComponent
    }
} 