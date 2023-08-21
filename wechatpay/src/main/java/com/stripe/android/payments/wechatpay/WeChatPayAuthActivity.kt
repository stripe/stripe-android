package com.stripe.android.payments.wechatpay

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.stripe.android.core.exception.StripeException
import com.stripe.android.payments.PaymentFlowResult
import kotlinx.coroutines.launch

private const val MISSING_DEPENDENCY = "WeChatPay dependency not found. " +
    "Add com.tencent.mm.opensdk:wechat-sdk-android:6.8.24 in app's build.gradle."

internal class WeChatPayAuthActivity : AppCompatActivity() {

    private val args: WeChatPayAuthContract.Args? by lazy {
        WeChatPayAuthContract.Args.fromIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val launcher = requireWeChatPayLauncher()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launcher.result.collect(::finishWithResult)
            }
        }

        launcher.launchWeChatPay(args?.weChat)
    }

    private fun requireWeChatPayLauncher(): WeChatPayLauncher {
        return WeChatPayLauncher.create(activity = this) ?: error(MISSING_DEPENDENCY)
    }

    private fun finishWithResult(result: WeChatPayLauncher.Result) {
        val paymentFlowResult = PaymentFlowResult.Unvalidated(
            clientSecret = args?.clientSecret,
            flowOutcome = result.outcome,
            exception = result.exception?.let { StripeException.create(it) },
        )

        setResult(
            Activity.RESULT_OK,
            Intent().putExtras(paymentFlowResult.toBundle())
        )

        finish()
    }
}
