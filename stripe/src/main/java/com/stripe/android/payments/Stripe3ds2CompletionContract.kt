package com.stripe.android.payments

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.view.Stripe3ds2CompletionActivity

internal class Stripe3ds2CompletionContract :
    ActivityResultContract<PaymentFlowResult.Unvalidated, PaymentFlowResult.Unvalidated>() {
    override fun createIntent(
        context: Context,
        input: PaymentFlowResult.Unvalidated?
    ): Intent {
        return Intent(context, Stripe3ds2CompletionActivity::class.java)
            .also { intent ->
                if (input != null) {
                    intent.putExtras(input.toBundle())
                }
            }
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): PaymentFlowResult.Unvalidated {
        return parsePaymentFlowResult(intent)
    }

    fun parsePaymentFlowResult(intent: Intent?): PaymentFlowResult.Unvalidated {
        return intent?.getParcelableExtra(EXTRA_ARGS) ?: PaymentFlowResult.Unvalidated()
    }

    internal companion object {
        const val EXTRA_ARGS = "extra_args"
    }
}
