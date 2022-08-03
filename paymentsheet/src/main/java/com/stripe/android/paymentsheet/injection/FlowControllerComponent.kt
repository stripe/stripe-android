package com.stripe.android.paymentsheet.injection

import android.content.Context
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.LoggingModule
import com.stripe.android.googlepaylauncher.injection.GooglePayLauncherModule
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentOptionsViewModel
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.flowcontroller.DefaultFlowController
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.ui.core.forms.resources.injection.ResourceRepositoryModule
import dagger.BindsInstance
import dagger.Component
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        StripeRepositoryModule::class,
        PaymentSheetCommonModule::class,
        FlowControllerModule::class,
        GooglePayLauncherModule::class,
        CoroutineContextModule::class,
        LoggingModule::class,
        ResourceRepositoryModule::class
    ]
)
internal interface FlowControllerComponent {
    val flowController: DefaultFlowController

    fun inject(paymentOptionsViewModel: PaymentOptionsViewModel.Factory)
    fun inject(factory: FormViewModel.Factory)

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
        fun paymentOptionFactory(paymentOptionFactory: PaymentOptionFactory): Builder

        @BindsInstance
        fun paymentOptionCallback(paymentOptionCallback: PaymentOptionCallback): Builder

        @BindsInstance
        fun paymentResultCallback(paymentResultCallback: PaymentSheetResultCallback): Builder

        @BindsInstance
        fun injectorKey(@InjectorKey injectorKey: String): Builder

        fun build(): FlowControllerComponent
    }
}
