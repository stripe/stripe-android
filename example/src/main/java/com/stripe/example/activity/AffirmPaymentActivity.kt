package com.stripe.example.activity

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.stripe.android.ApiResultCallback
import com.stripe.android.model.Address
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.payments.bankaccount.BillingDetails
import com.stripe.android.payments.bankaccount.CollectBankAccountForPaymentParams
import com.stripe.android.payments.bankaccount.CollectBankAccountForPaymentResponse
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
import com.stripe.example.R
import com.stripe.example.databinding.PaymentExampleActivityBinding

// TODO create dedicated activity for bank account flow.
class AffirmPaymentActivity : StripeIntentActivity() {

    private val viewBinding: PaymentExampleActivityBinding by lazy {
        PaymentExampleActivityBinding.inflate(layoutInflater)
    }

    lateinit var launcher: CollectBankAccountLauncher.ForPaymentIntent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        launcher = CollectBankAccountLauncher.ForPaymentIntent.create(
            this,
            "key_goes_here",
            object : ApiResultCallback<CollectBankAccountForPaymentResponse> {
                override fun onSuccess(result: CollectBankAccountForPaymentResponse) {
                    // do something with payment intent
                }

                override fun onError(e: Exception) {
                    // handle error
                }
            }
        )

        viewBinding.confirmWithPaymentButton.text =
            resources.getString(R.string.confirm_affirm_button)
        viewBinding.paymentExampleIntro.text =
            resources.getString(R.string.affirm_example_intro)

        viewModel.inProgress.observe(this, { enableUi(!it) })
        viewModel.status.observe(this, Observer(viewBinding.status::setText))

        viewBinding.confirmWithPaymentButton.setOnClickListener {
            createAndConfirmPaymentIntent(
                country = "US",
                paymentMethodCreateParams = confirmParams,
                shippingDetails = ConfirmPaymentIntentParams.Shipping(
                    Address.Builder()
                        .setCity("San Francisco")
                        .setCountry("US")
                        .setLine1("123 Market St")
                        .setLine2("#345")
                        .setPostalCode("94107")
                        .setState("CA")
                        .build(),
                    name = "Jane Doe",
                ),
                supportedPaymentMethods = "affirm"
            )
            launcher.launch(
                "clientSecret_of_paymentIntent",
                CollectBankAccountForPaymentParams(
                    paymentMethodType = "bank_account",
                    BillingDetails(
                        "Jane Doe",
                        "email@email.com"
                    )
                )
            )
        }
    }

    private fun enableUi(enable: Boolean) {
        viewBinding.progressBar.visibility = if (enable) View.INVISIBLE else View.VISIBLE
        viewBinding.confirmWithPaymentButton.isEnabled = enable
    }

    private companion object {
        private val confirmParams = PaymentMethodCreateParams.createAffirm()
    }
}
