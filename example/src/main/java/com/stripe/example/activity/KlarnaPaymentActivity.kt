package com.stripe.example.activity

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.example.databinding.KlarnaPaymentActivityBinding

class KlarnaPaymentActivity : StripeIntentActivity() {

    private val viewBinding: KlarnaPaymentActivityBinding by lazy {
        KlarnaPaymentActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewModel.inProgress.observe(this, { enableUi(!it) })
        viewModel.status.observe(this, Observer(viewBinding.status::setText))

        viewBinding.confirmWithKlarnaButton.setOnClickListener {
            createAndConfirmPaymentIntent("US", confirmParams, "klarna")
        }
    }

    private fun enableUi(enable: Boolean) {
        viewBinding.progressBar.visibility = if (enable) View.INVISIBLE else View.VISIBLE
        viewBinding.confirmWithKlarnaButton.isEnabled = enable
    }

    private companion object {
        private val confirmParams = PaymentMethodCreateParams.createKlarna(
            billingDetails = PaymentMethod.BillingDetails(
                email = "jenny@example.com",
                address = Address.Builder()
                    .setCountry("US")
                    .build()
            )
        )
    }
}
