package com.stripe.android.link.injection

import android.app.Activity
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.link.LinkController
import dagger.BindsInstance
import dagger.Subcomponent

@LinkControllerPresenterScope
@Subcomponent
internal interface LinkControllerPresenterComponent {
    val presenter: LinkController.Presenter

    @Subcomponent.Factory
    interface Factory {
        fun build(
            @BindsInstance activity: Activity,
            @BindsInstance lifecycleOwner: LifecycleOwner,
            @BindsInstance activityResultRegistryOwner: ActivityResultRegistryOwner,
            @BindsInstance presentPaymentMethodsCallback: LinkController.PresentPaymentMethodsCallback,
            @BindsInstance authenticationCallback: LinkController.AuthenticationCallback,
        ): LinkControllerPresenterComponent
    }
}
