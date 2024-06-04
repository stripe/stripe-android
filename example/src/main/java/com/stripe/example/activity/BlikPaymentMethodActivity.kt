package com.stripe.example.activity

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.example.databinding.BlikActivityBinding

class BlikPaymentMethodActivity : StripeIntentActivity() {
    private val viewBinding: BlikActivityBinding by lazy {
        BlikActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewModel.inProgress.observe(this) { enableUi(!it) }
        viewModel.status.observe(this, Observer(viewBinding.status::setText))

        viewBinding.submit.setOnClickListener {
            createAndConfirmPaymentIntent(
                country = "pl",
                paymentMethodCreateParams = PaymentMethodCreateParams.createBlik(
                    billingDetails = PaymentMethod.BillingDetails(
                        email = viewBinding.email.text.toString()
                    )
                ),
                supportedPaymentMethods = "blik",
                currency = "pln",
                paymentMethodOptions = PaymentMethodOptionsParams.Blik(
                    viewBinding.code.text.toString()
                )
            )
        }
    }

    private fun enableUi(enabled: Boolean) {
        viewBinding.submit.isEnabled = enabled
        viewBinding.progressBar.visibility = if (enabled) View.INVISIBLE else View.VISIBLE
    }
}
