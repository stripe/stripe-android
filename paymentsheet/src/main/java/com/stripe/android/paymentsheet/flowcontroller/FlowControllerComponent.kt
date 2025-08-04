package com.stripe.android.paymentsheet.flowcontroller

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.elements.payment.FlowController.PaymentOptionDisplayData
import com.stripe.android.elements.payment.PaymentSheet
import com.stripe.android.paymentsheet.InitializedViaCompose
import dagger.BindsInstance
import dagger.Subcomponent

@FlowControllerScope
@Subcomponent
internal interface FlowControllerComponent {
    val flowController: DefaultFlowController
    val stateComponent: FlowControllerStateComponent

    @Subcomponent.Builder
    interface Builder {
        @BindsInstance
        fun lifeCycleOwner(lifecycleOwner: LifecycleOwner): Builder

        @BindsInstance
        fun activityResultCaller(
            activityResultCaller: ActivityResultCaller,
        ): Builder

        @BindsInstance
        fun activityResultRegistryOwner(
            activityResultRegistryOwner: ActivityResultRegistryOwner,
        ): Builder

        @BindsInstance
        fun paymentOptionCallback(paymentOptionCallback: PaymentOptionDisplayData.Callback): Builder

        @BindsInstance
        fun paymentResultCallback(paymentResultCallback: PaymentSheet.ResultCallback): Builder

        @BindsInstance
        fun initializedViaCompose(@InitializedViaCompose initializedViaCompose: Boolean): Builder

        fun build(): FlowControllerComponent
    }
}
