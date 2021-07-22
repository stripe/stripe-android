package com.stripe.android.paymentsheet

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import com.stripe.android.StripeIntentResult
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.PaymentFlowResultProcessor
import com.stripe.android.paymentsheet.model.StripeIntentValidator
import com.stripe.android.view.AuthActivityStarterHost
import kotlin.coroutines.CoroutineContext

sealed interface StartAndConfirmResult {
    data class ErrorStripeIntentReady(val stripeIntentResult: StripeIntentResult<StripeIntent>) :
        StartAndConfirmResult

    data class StripeResultError(val throwable: Throwable) : StartAndConfirmResult
    data class Fatal(val throwable: Throwable) : StartAndConfirmResult
    data class Success(val stripeIntentResult: StripeIntentResult<StripeIntent>) :
        StartAndConfirmResult
}

interface PaymentSheetPaymentController {

    /**
     * Confirm the Stripe Intent and resolve any next actions
     */
    suspend fun startConfirmAndAuth(
        host: AuthActivityStarterHost,
        confirmStripeIntentParams: ConfirmStripeIntentParams,
        publishableKey: String,
        stripeAccountId: String?
    )

    suspend fun onPaymentFlowResultPaymentSheetViewModel(
        paymentFlowResult: PaymentFlowResult.Unvalidated,
        paymentFlowResultProcessor: PaymentFlowResultProcessor<out StripeIntent, StripeIntentResult<StripeIntent>>,
        workContext: CoroutineContext,
        stripeIntentValidator: StripeIntentValidator
    ): StartAndConfirmResult

    suspend fun onPaymentFlowResultDefaultFlowController(
        paymentFlowResult: PaymentFlowResult.Unvalidated,
        paymentFlowResultProcessor: PaymentFlowResultProcessor<out StripeIntent, StripeIntentResult<StripeIntent>>,
    ): PaymentSheetResult

    fun registerLaunchersWithActivityResultCaller(
        activityResultCaller: ActivityResultCaller,
        activityResultCallback: ActivityResultCallback<PaymentFlowResult.Unvalidated>
    )

    fun unregisterLaunchers()
}
