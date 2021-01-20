package com.stripe.android

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.view.ActivityStarter
import com.stripe.android.view.PaymentRelayActivity

internal class PaymentRelayContract : ActivityResultContract<PaymentRelayStarter.Args, PaymentFlowResult.Unvalidated>() {
    override fun createIntent(
        context: Context,
        input: PaymentRelayStarter.Args?
    ): Intent {
        return Intent(context, PaymentRelayActivity::class.java)
            .putExtra(ActivityStarter.Args.EXTRA, input)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): PaymentFlowResult.Unvalidated {
        return PaymentFlowResult.Unvalidated.fromIntent(intent)
    }
}
