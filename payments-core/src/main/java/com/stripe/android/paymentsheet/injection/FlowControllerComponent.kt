package com.stripe.android.paymentsheet.injection

import android.content.Context
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.flowcontroller.ActivityLauncherFactory
import com.stripe.android.paymentsheet.flowcontroller.DefaultFlowController
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.view.AuthActivityStarter
import dagger.BindsInstance
import dagger.Component
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Singleton
@Component(modules = [FlowControllerModule::class])
internal interface FlowControllerComponent {
    val flowController: DefaultFlowController

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun appContext(appContext: Context): Builder

        @BindsInstance
        fun viewModelStoreOwner(viewModelStoreOwner: ViewModelStoreOwner): Builder

        @BindsInstance
        fun lifecycleScope(lifecycleScope: CoroutineScope): Builder

        @BindsInstance
        fun activityLauncherFactory(activityLauncherFactory: ActivityLauncherFactory): Builder

        @BindsInstance
        fun statusBarColor(statusBarColor: () -> Int?): Builder

        @BindsInstance
        fun authHostSupplier(authHostSupplier: () -> AuthActivityStarter.Host): Builder

        @BindsInstance
        fun paymentOptionFactory(paymentOptionFactory: PaymentOptionFactory): Builder

        @BindsInstance
        fun paymentOptionCallback(paymentOptionCallback: PaymentOptionCallback): Builder

        @BindsInstance
        fun paymentResultCallback(paymentResultCallback: PaymentSheetResultCallback): Builder

        fun build(): FlowControllerComponent
    }
}
