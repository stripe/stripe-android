package com.stripe.example.activity

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.stripe.android.model.Address
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.example.databinding.AffirmPaymentActivityBinding

class AffirmPaymentActivity : StripeIntentActivity() {

    private val viewBinding: AffirmPaymentActivityBinding by lazy {
        AffirmPaymentActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewModel.inProgress.observe(this, { enableUi(!it) })
        viewModel.status.observe(this, Observer(viewBinding.status::setText))

        viewBinding.confirmWithAffirmButton.setOnClickListener {
            createAndConfirmPaymentIntent(
                country = "US",
                paymentMethodCreateParams = confirmParams,
                shippingDetails = ConfirmPaymentIntentParams.Shipping(
                    Address(
                        city = "Seattle",
                        country = "US",
                        line1 = "1201 Seattle Rd",
                        state = "WA",
                        postalCode = "98101"
                    ),
                    name = "Jane Doe",
                ),
                supportedPaymentMethods = "affirm"
            )
        }
    }

    private fun enableUi(enable: Boolean) {
        viewBinding.progressBar.visibility = if (enable) View.INVISIBLE else View.VISIBLE
        viewBinding.confirmWithAffirmButton.isEnabled = enable
    }

    private companion object {
        private val confirmParams = PaymentMethodCreateParams.createAffirm()
    }
}
