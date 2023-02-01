package com.stripe.android.utils

import androidx.activity.result.ActivityResultCaller
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.ConfirmationHandler
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.channels.Channel

internal class FakeConfirmationHandler : ConfirmationHandler {

    private val resultChannel = Channel<Result<PaymentResult>>(capacity = 1)

    override fun registerFromActivity(activityResultCaller: ActivityResultCaller) {
        // Nothing to do here
    }

    fun emitPaymentResult(paymentResult: PaymentResult) {
        resultChannel.trySend(Result.success(paymentResult))
    }

    fun emitFailure(t: Throwable) {
        resultChannel.trySend(Result.failure(t))
    }

    override suspend fun confirm(
        paymentSelection: PaymentSelection,
        clientSecret: ClientSecret,
        shipping: ConfirmPaymentIntentParams.Shipping?
    ): Result<PaymentResult> {
        return resultChannel.receive()
    }

    override fun unregisterFromActivity() {
        // Nothing to do here
    }
}
