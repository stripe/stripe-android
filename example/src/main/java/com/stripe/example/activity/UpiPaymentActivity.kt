package com.stripe.example.activity

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.example.databinding.UpiPaymentActivityBinding

class UpiPaymentActivity : StripeIntentActivity() {
    private val viewBinding: UpiPaymentActivityBinding by lazy {
        UpiPaymentActivityBinding.inflate(layoutInflater)
    }

    private lateinit var paymentIntentSecret: String

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

            createAndConfirmPaymentIntent("in", params) { secret ->
                paymentIntentSecret = secret
            }
        }
    }

    override fun onConfirmSuccess() {
        startActivity(
            Intent(this@UpiPaymentActivity, UpiWaitingActivity::class.java)
                .putExtra(EXTRA_CLIENT_SECRET, paymentIntentSecret)
        )
    }

    override fun onConfirmError(failedResult: PaymentResult.Failed) {
        viewModel.status.value += "\n\nPaymentIntent confirmation failed with throwable " +
            "${failedResult.throwable} \n\n"
    }

    internal companion object {
        const val EXTRA_CLIENT_SECRET = "extra_client_secret"
    }
}
