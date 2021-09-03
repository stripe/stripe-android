package com.stripe.example.activity

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import com.stripe.android.PaymentIntentResult
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.example.databinding.UpiPaymentActivityBinding

class UpiPaymentActivity : StripeIntentActivity() {
    private val viewBinding: UpiPaymentActivityBinding by lazy {
        UpiPaymentActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewModel.status.observe(this, Observer(viewBinding.status::setText))
        viewBinding.submit.setOnClickListener {
            val params = PaymentMethodCreateParams.create(
                upi = PaymentMethodCreateParams.Upi(
                    vpa = viewBinding.vpa.text.toString()
                ),
                billingDetails = PaymentMethod.BillingDetails(
                    name = "Jenny Rosen",
                    phone = "(555) 555-5555",
                    email = "jenny@example.com",
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

            createAndConfirmPaymentIntent("in", params)
        }
    }

    override fun onConfirmSuccess(result: PaymentIntentResult) {
        val paymentIntent = result.intent
        startActivity(
            Intent(this@UpiPaymentActivity, UpiWaitingActivity::class.java)
                .putExtra(EXTRA_CLIENT_SECRET, paymentIntent.clientSecret)
        )
    }

    override fun onConfirmError(throwable: Throwable) {
        viewModel.status.value += "\n\nException: " + throwable.message
    }

    internal companion object {
        const val EXTRA_CLIENT_SECRET = "extra_client_secret"
    }
}
