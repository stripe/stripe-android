package com.stripe.android.link.injection

import android.app.Activity
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.link.LinkController
import dagger.BindsInstance
import dagger.Subcomponent

@LinkControllerScope
@Subcomponent
internal interface LinkControllerComponent {
    val controller: LinkController

    @Subcomponent.Factory
    interface Factory {
        fun build(
            @BindsInstance activity: Activity,
            @BindsInstance lifecycleOwner: LifecycleOwner,
            @BindsInstance activityResultRegistryOwner: ActivityResultRegistryOwner,
            @BindsInstance presentPaymentMethodsCallback: LinkController.PresentPaymentMethodsCallback,
            @BindsInstance createPaymentMethodCallback: LinkController.CreatePaymentMethodCallback,
            @BindsInstance authenticationCallback: LinkController.AuthenticationCallback,
            @BindsInstance registerConsumerCallback: LinkController.RegisterConsumerCallback,
        ): LinkControllerComponent
    }
}
