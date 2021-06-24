package com.stripe.android.payments.wechatpay

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.StripeIntentResult
import com.stripe.android.exception.StripeException
import com.stripe.android.model.WeChat
import com.stripe.android.payments.PaymentFlowResult

internal class WeChatPayAuthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            launchWeChat(
                requireNotNull(WeChatPayAuthContract.Args.fromIntent(intent)).weChat
            )
        }.onFailure {
            finishWithResult(
                PaymentFlowResult.Unvalidated(
                    flowOutcome = StripeIntentResult.Outcome.FAILED,
                    exception = StripeException.create(it)
                )
            )
        }
    }

    private fun launchWeChat(weChat: WeChat) {
        // TODO: invoke WeChat SDK
    }

    private fun finishWithResult(
        paymentFlowResult: PaymentFlowResult.Unvalidated
    ) {
        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtras(paymentFlowResult.toBundle())
        )
        finish()
    }
}
