package com.stripe.example.activity

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.stripe.example.R
import com.stripe.example.databinding.PaymentExampleActivityBinding

class InstantUSBankAccountActivity : StripeIntentActivity() {

    private val viewBinding: PaymentExampleActivityBinding by lazy {
        PaymentExampleActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewBinding.confirmWithPaymentButton.text =
            getString(R.string.confirm_with_us_bank_account)
        viewBinding.paymentExampleIntro.text =
            getString(R.string.confirm_with_us_bank_account_intro)

        viewModel.inProgress.observe(this) { enableUi(!it) }
        viewModel.status.observe(this, Observer(viewBinding.status::setText))

        viewBinding.confirmWithPaymentButton.setOnClickListener {
            viewModel.createPaymentIntent(
                country = "us",
                supportedPaymentMethods = "us_bank_account"
            ).observe(this) { result ->
                result.onSuccess {
                    viewModel.status
                        .postValue("Collecting bank account information for payment")
                }
            }
        }
    }

    private fun enableUi(enable: Boolean) {
        viewBinding.progressBar.visibility = if (enable) View.INVISIBLE else View.VISIBLE
        viewBinding.confirmWithPaymentButton.isEnabled = enable
    }
}
