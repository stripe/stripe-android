package com.stripe.example.activity

import android.os.Bundle
import android.view.View
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.example.databinding.WeChatActivityBinding

class WeChatPaymentMethodActivity : StripeIntentActivity() {
    private val viewBinding: WeChatActivityBinding by lazy {
        WeChatActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewModel.inProgress.observe(this) { enableUi(!it) }
        viewModel.status.observe(this, viewBinding.status::setText)

        viewBinding.submit.setOnClickListener {
            createAndConfirmPaymentIntent(
                country = "us",
                paymentMethodCreateParams = PaymentMethodCreateParams.createWeChatPay(),
                supportedPaymentMethods = "wechat_pay",
                currency = "usd",
                paymentMethodOptions = PaymentMethodOptionsParams.WeChatPay(
                    appId = "my_app",
                )
            )
        }
    }

    private fun enableUi(enabled: Boolean) {
        viewBinding.submit.isEnabled = enabled
        viewBinding.progressBar.visibility = if (enabled) View.INVISIBLE else View.VISIBLE
    }
}
