package com.stripe.android.paymentsheet.flowcontroller

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.paymentsheet.InitializedViaCompose
import com.stripe.android.paymentsheet.PaymentOptionResultCallback
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import dagger.BindsInstance
import dagger.Subcomponent

@FlowControllerScope
@Subcomponent
internal interface FlowControllerComponent {
    val flowController: DefaultFlowController
    val stateComponent: FlowControllerStateComponent

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance
            lifecycleOwner: LifecycleOwner,
            @BindsInstance
            activityResultCaller: ActivityResultCaller,
            @BindsInstance
            activityResultRegistryOwner: ActivityResultRegistryOwner,
            @BindsInstance
            paymentOptionResultCallback: PaymentOptionResultCallback,
            @BindsInstance
            paymentResultCallback: PaymentSheetResultCallback,
            @BindsInstance
            @InitializedViaCompose
            initializedViaCompose: Boolean,
        ): FlowControllerComponent
    }
}
