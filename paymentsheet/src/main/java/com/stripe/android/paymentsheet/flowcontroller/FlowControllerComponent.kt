package com.stripe.android.paymentsheet.flowcontroller

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
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
        fun activityResultCaller(activityResultCaller: ActivityResultCaller): Builder

        @BindsInstance
        fun statusBarColor(statusBarColor: () -> Int?): Builder

        @BindsInstance
        fun paymentOptionCallback(paymentOptionCallback: PaymentOptionCallback): Builder

        @BindsInstance
        fun paymentResultCallback(paymentResultCallback: PaymentSheetResultCallback): Builder

        @BindsInstance
        fun injectorKey(@InjectorKey injectorKey: String): Builder

        fun build(): FlowControllerComponent
    }
}
