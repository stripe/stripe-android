package com.stripe.example.activity

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.lifecycle.Observer
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.example.databinding.NetbankingPaymentActivityBinding

class NetbankingPaymentActivity : StripeIntentActivity() {
    private val viewBinding: NetbankingPaymentActivityBinding by lazy {
        NetbankingPaymentActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewModel.inProgress.observe(this, { enableUi(!it) })
        viewModel.status.observe(this, Observer(viewBinding.status::setText))

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            arrayListOf("HDFC", "ICICI", "SBI", "Axis")
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        viewBinding.bankName.adapter = adapter

        viewBinding.submit.setOnClickListener {
            val params = PaymentMethodCreateParams.create(
                netbanking = PaymentMethodCreateParams.Netbanking(
                    bank = viewBinding.bankName.selectedItem.toString()
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

    private fun enableUi(enabled: Boolean) {
        viewBinding.submit.isEnabled = enabled
        viewBinding.progressBar.visibility = if (enabled) View.INVISIBLE else View.VISIBLE
    }
}
