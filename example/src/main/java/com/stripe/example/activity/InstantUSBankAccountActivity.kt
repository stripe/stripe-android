package com.stripe.example.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import com.stripe.android.ApiResultCallback
import com.stripe.android.payments.bankaccount.CollectBankAccountForPaymentResponse
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
import com.stripe.android.payments.bankaccount.CollectBankAccountParams
import com.stripe.example.Settings
import com.stripe.example.databinding.PaymentExampleActivityBinding

class InstantUSBankAccountActivity : StripeIntentActivity() {

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
                        this@InstantUSBankAccountActivity,
                        result.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onError(e: Exception) {
                    Log.e("error", "error", e)
                    Toast.makeText(
                        this@InstantUSBankAccountActivity,
                        e.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        viewBinding.confirmWithPaymentButton.text =
            "Confirm with Bank Account"
        viewBinding.paymentExampleIntro.text =
            "TODO"

        viewModel.inProgress.observe(this, { enableUi(!it) })
        viewModel.status.observe(this, Observer(viewBinding.status::setText))

        viewBinding.confirmWithPaymentButton.setOnClickListener {
            viewModel.createPaymentIntent(
                country = "us",
                supportedPaymentMethods = "us_bank_account"
            ).observe(this) { result ->
                result.onSuccess {
                    val settings = Settings(this)
                    launcher.launch(
                        publishableKey = settings.publishableKey,
                        clientSecret = it.getString("secret"),
                        params = CollectBankAccountParams.USBankAccount(
                            name = "Jane Doe",
                            email = "email@email.com"
                        )
                    )
                }
            }
        }
    }

    private fun enableUi(enable: Boolean) {
        viewBinding.progressBar.visibility = if (enable) View.INVISIBLE else View.VISIBLE
        viewBinding.confirmWithPaymentButton.isEnabled = enable
    }
}
