package com.stripe.example.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import com.stripe.android.ApiResultCallback
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.payments.bankaccount.CollectBankAccountForPaymentResponse
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
import com.stripe.android.payments.bankaccount.CollectBankAccountParams
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
            object : ApiResultCallback<CollectBankAccountForPaymentResponse> {
                override fun onSuccess(result: CollectBankAccountForPaymentResponse) {
                    Toast.makeText(
                        this@AffirmPaymentActivity,
                        result.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onError(e: Exception) {
                    Log.e("error", "error", e)
                    Toast.makeText(
                        this@AffirmPaymentActivity,
                        e.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
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
//            createAndConfirmPaymentIntent(
//                country = "US",
//                paymentMethodCreateParams = confirmParams,
//                shippingDetails = ConfirmPaymentIntentParams.Shipping(
//                    Address.Builder()
//                        .setCity("San Francisco")
//                        .setCountry("US")
//                        .setLine1("123 Market St")
//                        .setLine2("#345")
//                        .setPostalCode("94107")
//                        .setState("CA")
//                        .build(),
//                    name = "Jane Doe",
//                ),
//                supportedPaymentMethods = "affirm"
//            )
            launcher.launch(
                "key_goes_here",
                "pi_1234_secret_5678",
                CollectBankAccountParams.USBankAccount(
                    name = "Jane Doe",
                    email = "email@email.com"
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
