package com.stripe.android.paymentsheet.injection

import android.content.Context
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.googlepaylauncher.GooglePayLauncherModule
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.payments.core.injection.PaymentCommonModule
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.flowcontroller.DefaultFlowController
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.view.AuthActivityStarterHost
import dagger.BindsInstance
import dagger.Component
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        PaymentCommonModule::class,
        FlowControllerModule::class,
        AnalyticsRequestExecutor.DaggerModule::class,
        GooglePayLauncherModule::class
    ]
)
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
        fun lifeCycleOwner(lifecycleOwner: LifecycleOwner): Builder

        @BindsInstance
        fun activityResultCaller(activityResultCaller: ActivityResultCaller): Builder

        @BindsInstance
        fun statusBarColor(statusBarColor: () -> Int?): Builder

        @BindsInstance
        fun authHostSupplier(authHostSupplier: () -> AuthActivityStarterHost): Builder

        @BindsInstance
        fun paymentOptionFactory(paymentOptionFactory: PaymentOptionFactory): Builder

        @BindsInstance
        fun paymentOptionCallback(paymentOptionCallback: PaymentOptionCallback): Builder

        @BindsInstance
        fun paymentResultCallback(paymentResultCallback: PaymentSheetResultCallback): Builder

        fun build(): FlowControllerComponent
    }
}
