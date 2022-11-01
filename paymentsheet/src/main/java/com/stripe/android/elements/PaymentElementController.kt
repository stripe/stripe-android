package com.stripe.android.elements

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.elements.injection.DaggerPaymentElementComponent
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.CoroutineScope

/**
 * Entry point class that's held to control the PaymentElement.
 */
interface PaymentElementController {
    fun configureWithPaymentIntent(
        paymentIntentClientSecret: String,
        paymentSheetConfig: PaymentSheet.Configuration?
    )

    suspend fun getViewModelFactory(): ViewModelProvider.Factory

    fun completePayment(paymentSelection: PaymentSelection)

    companion object {
        fun create(
            activity: ComponentActivity,
            paymentResultCallback: PaymentElementResultCallback
        ) = setup(
            activity.applicationContext,
            activity.lifecycleScope,
            activity,
            activity,
            paymentResultCallback
        )

        private fun setup(
            appContext: Context,
            lifecycleScope: CoroutineScope,
            lifecycleOwner: LifecycleOwner,
            activityResultCaller: ActivityResultCaller,
            paymentResultCallback: PaymentElementResultCallback
        ): PaymentElementController {
            val paymentElementComponent = DaggerPaymentElementComponent.builder()
                .appContext(appContext)
                .lifecycleScope(lifecycleScope)
                .lifecycleOwner(lifecycleOwner)
                .activityResultCaller(activityResultCaller)
                .paymentResultCallback(paymentResultCallback)
                .build()
            val injector = object : NonFallbackInjector {
                override fun inject(injectable: Injectable<*>) {
                    when (injectable) {
                        is FormViewModel.Factory -> paymentElementComponent.inject(injectable)
                        else -> {
                            throw IllegalArgumentException("invalid Injectable $injectable requested in $this")
                        }
                    }
                }
            }
            return paymentElementComponent.paymentElementController.also {
                it.injector = injector
            }
        }
    }
}

fun interface PaymentElementResultCallback {
    fun onPaymentResult(paymentResult: PaymentResult)
}
