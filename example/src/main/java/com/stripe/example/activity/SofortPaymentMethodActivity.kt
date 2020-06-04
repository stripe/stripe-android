package com.stripe.example.activity

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.example.databinding.SofortActivityBinding

class SofortPaymentMethodActivity : StripeIntentActivity() {
    private val viewBinding: SofortActivityBinding by lazy {
        SofortActivityBinding.inflate(layoutInflater)
    }
    private val keyboardController: KeyboardController by lazy {
        KeyboardController(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewModel.inProgress.observe(this, Observer { enableUi(!it) })
        viewModel.status.observe(this, Observer(viewBinding.status::setText))

        viewBinding.submit.setOnClickListener {
            keyboardController.hide()

            val country = viewBinding.country.text.toString().toLowerCase()
            createAndConfirmPaymentIntent(
                country,
                PaymentMethodCreateParams.create(
                    sofort = PaymentMethodCreateParams.Sofort(
                        country = country
                    ),
                    billingDetails = PaymentMethod.BillingDetails(
                        name = "Jenny Rosen",
                        phone = "1-800-555-1234",
                        email = "jrosen@example.com",
                        address = Address.Builder()
                            .setCity("San Francisco")
                            .setCountry("US")
                            .setLine1("123 Market St")
                            .setLine2("#345")
                            .setPostalCode("94107")
                            .setState("CA")
                            .build()
                    )
                )
            )
        }
    }

    private fun enableUi(enabled: Boolean) {
        viewBinding.submit.isEnabled = enabled
        viewBinding.progressBar.visibility = if (enabled) View.INVISIBLE else View.VISIBLE
    }
}
