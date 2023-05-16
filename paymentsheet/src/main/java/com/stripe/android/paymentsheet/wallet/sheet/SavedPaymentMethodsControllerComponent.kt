package com.stripe.android.paymentsheet.wallet.sheet

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.paymentsheet.customer.CustomerAdapter
import dagger.BindsInstance
import dagger.Subcomponent

@SavedPaymentMethodsControllerScope
@Subcomponent
internal interface SavedPaymentMethodsControllerComponent {
    val savedPaymentMethodsSheet: DefaultSavedPaymentMethodsController
    val stateComponent: SavedPaymentMethodsControllerStateComponent

    @Subcomponent.Builder
    interface Builder {
        @BindsInstance
        fun lifeCycleOwner(lifecycleOwner: LifecycleOwner): Builder

        @BindsInstance
        fun activityResultCaller(activityResultCaller: ActivityResultCaller): Builder

        @BindsInstance
        fun statusBarColor(statusBarColor: () -> Int?): Builder

        @BindsInstance
        fun savedPaymentMethodsSheetResultCallback(callback: SavedPaymentMethodsSheetResultCallback): Builder

        @BindsInstance
        fun configuration(configuration: SavedPaymentMethodsController.Configuration): Builder

        @BindsInstance
        fun customerAdapter(customerAdapter: CustomerAdapter): Builder

        @BindsInstance
        fun injectorKey(@InjectorKey injectorKey: String): Builder

        fun build(): SavedPaymentMethodsControllerComponent
    }
}
