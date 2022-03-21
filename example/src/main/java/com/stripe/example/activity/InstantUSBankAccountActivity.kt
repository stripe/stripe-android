package com.stripe.example.activity

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer

import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResult
import com.stripe.example.Settings
import com.stripe.example.databinding.PaymentExampleActivityBinding

class InstantUSBankAccountActivity : StripeIntentActivity() {

    private val viewBinding: PaymentExampleActivityBinding by lazy {
        PaymentExampleActivityBinding.inflate(layoutInflater)
    }

    private val settings by lazy { Settings(this) }

    lateinit var launcher: CollectBankAccountLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        launcher = CollectBankAccountLauncher.create(
            this
        ) { result: CollectBankAccountResult ->
            when (result) {
                is CollectBankAccountResult.Completed -> {
                    viewModel.status
                        .postValue(
                            "Attached bank account to paymentIntent." +
                                " secret: ${result.response.clientSecret}. Confirming..."
                        )
//                    confirmPaymentIntent(
//                        ConfirmPaymentIntentParams.createWithPaymentMethodId(
//                            paymentMethodId = result.response.intent.paymentMethodId!!,
//                            clientSecret = result.response.intent.clientSecret!!
//                        )
//                    )
                }
                is CollectBankAccountResult.Cancelled ->
                    viewModel.status.postValue(
                        "User cancelled flow."
                    )
                is CollectBankAccountResult.Failed ->
                    viewModel.status.postValue(
                        "Error attaching bank account to intent. ${result.error.message}"
                    )
            }
        }

        viewBinding.confirmWithPaymentButton.text =
            "Confirm with Bank Account"
        viewBinding.paymentExampleIntro.text =
            "Click below to create payment intent and attach a bank account to payment method."

        viewModel.inProgress.observe(this, { enableUi(!it) })
        viewModel.status.observe(this, Observer(viewBinding.status::setText))

        viewBinding.confirmWithPaymentButton.setOnClickListener {
            viewModel.createPaymentIntent(
                country = "us",
                supportedPaymentMethods = "us_bank_account"
            ).observe(this) { result ->
                result.onSuccess {
                    viewModel.status
                        .postValue("Collecting bank account information for payment")
                    launcher.presentWithPaymentIntent(
                        publishableKey = settings.publishableKey,
                        clientSecret = it.getString("client_secret"),
                        params = CollectBankAccountConfiguration.USBankAccount(
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
