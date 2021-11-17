package com.stripe.android

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.view.PaymentRelayActivity

internal class PaymentRelayContract : ActivityResultContract<PaymentRelayStarter.Args, PaymentFlowResult.Unvalidated>() {
    override fun createIntent(context: Context, input: PaymentRelayStarter.Args): Intent {
        val paymentFlowResult = input?.toResult() ?: PaymentFlowResult.Unvalidated()
        return Intent(context, PaymentRelayActivity::class.java)
            .putExtras(paymentFlowResult.toBundle())
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): PaymentFlowResult.Unvalidated {
        return PaymentFlowResult.Unvalidated.fromIntent(intent)
    }
}
