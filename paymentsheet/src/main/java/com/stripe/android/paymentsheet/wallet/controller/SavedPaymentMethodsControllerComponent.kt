package com.stripe.android.paymentsheet.wallet.controller

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerScope
import dagger.BindsInstance
import dagger.Subcomponent

@FlowControllerScope
@Subcomponent
internal interface SavedPaymentMethodsControllerComponent {
    val savedPaymentMethodsSheet: DefaultSavedPaymentMethodsController

    @Subcomponent.Builder
    interface Builder {
        @BindsInstance
        fun lifeCycleOwner(lifecycleOwner: LifecycleOwner): Builder

        @BindsInstance
        fun activityResultCaller(activityResultCaller: ActivityResultCaller): Builder

        @BindsInstance
        fun statusBarColor(statusBarColor: () -> Int?): Builder

        @BindsInstance
        fun callback(callback: SavedPaymentMethodsSheetResultCallback): Builder

        @BindsInstance
        fun injectorKey(@InjectorKey injectorKey: String): Builder

        fun build(): SavedPaymentMethodsControllerComponent
    }
}
