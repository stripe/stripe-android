package com.stripe.android.paymentsheet

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RestrictTo
import com.stripe.android.payments.paymentlauncher.PaymentResult
import java.lang.IllegalStateException

// TODO: refer to [PaymentLauncherContract] as example
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ExternalPaymentMethodContract : ActivityResultContract<ExternalPaymentMethodInput, PaymentResult>() {
    override fun createIntent(context: Context, input: ExternalPaymentMethodInput): Intent {
        // TODO: rather than require not null here, by default return a result code of failed
        return requireNotNull(
                ExternalPaymentMethodInterceptor.externalPaymentMethodCreator?.createIntent(
                    context,
                    input,
                )
            )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): PaymentResult {
        return when (resultCode) {
            1 -> PaymentResult.Completed
            -1 -> PaymentResult.Canceled // TODO: idk
                // TODO: make this failure message part of the API
            else -> PaymentResult.Failed(throwable = IllegalStateException("external payment failed"))
        }
    }
}