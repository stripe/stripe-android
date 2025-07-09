package com.stripe.android.link.injection

import android.app.Activity
import android.app.Application
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.link.LinkController
import com.stripe.android.link.LinkController.CreatePaymentMethodCallback
import com.stripe.android.link.LinkController.LookupConsumerCallback
import com.stripe.android.link.LinkController.PresentPaymentMethodsCallback
import com.stripe.android.link.LinkControllerViewModel
import dagger.BindsInstance
import dagger.Component
import dagger.Subcomponent
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        LinkControllerModule::class,
    ]
)
internal interface LinkControllerViewModelComponent {
    val viewModel: LinkControllerViewModel

    @Component.Factory
    interface Factory {
        fun build(
            @BindsInstance application: Application,
            @BindsInstance savedStateHandle: SavedStateHandle,
        ): LinkControllerViewModelComponent
    }
}

@Subcomponent
internal interface LinkControllerComponent {
    val controller: LinkController

    @Subcomponent.Factory
    interface Factory {
        fun build(
            @BindsInstance activity: Activity,
            @BindsInstance lifecycleOwner: LifecycleOwner,
            @BindsInstance activityResultRegistryOwner: ActivityResultRegistryOwner,
            @BindsInstance presentPaymentMethodCallback: PresentPaymentMethodsCallback,
            @BindsInstance lookupConsumerCallback: LookupConsumerCallback,
            @BindsInstance createPaymentMethodCallback: CreatePaymentMethodCallback,
        ): LinkControllerComponent
    }
}
