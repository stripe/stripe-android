package com.stripe.example.activity

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.example.R
import com.stripe.example.databinding.PaymentExampleActivityBinding

class KlarnaPaymentActivity : StripeIntentActivity() {

    private val viewBinding: PaymentExampleActivityBinding by lazy {
        PaymentExampleActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewBinding.confirmWithPaymentButton.text =
            resources.getString(R.string.confirm_klarna_button)
        viewBinding.paymentExampleIntro.text =
            resources.getString(R.string.klarna_example_intro)

        viewModel.inProgress.observe(this, { enableUi(!it) })
        viewModel.status.observe(this, Observer(viewBinding.status::setText))

        viewBinding.confirmWithPaymentButton.setOnClickListener {
            createAndConfirmPaymentIntent("US", confirmParams, "klarna")
        }
    }

    private fun enableUi(enable: Boolean) {
        viewBinding.progressBar.visibility = if (enable) View.INVISIBLE else View.VISIBLE
        viewBinding.confirmWithPaymentButton.isEnabled = enable
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
