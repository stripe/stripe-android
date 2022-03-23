package com.stripe.example.activity

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.MandateDataParams
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResult
import com.stripe.example.R
import com.stripe.example.Settings
import com.stripe.example.databinding.ConnectBankAccountExampleActivityBinding

/**
 * This example is currently work in progress. Do not use it as a reference.
 */
class ConnectUSBankAccountActivity : StripeIntentActivity() {

    private val viewBinding: ConnectBankAccountExampleActivityBinding by lazy {
        ConnectBankAccountExampleActivityBinding.inflate(layoutInflater)
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
                    confirmPaymentIntent(
                        ConfirmPaymentIntentParams.create(
                            clientSecret = result.response.clientSecret,
                        )
                    )
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
                    launcher.presentWithPaymentIntent(
                        publishableKey = settings.publishableKey,
                        clientSecret = it.getString("client_secret"),
                        params = CollectBankAccountConfiguration.USBankAccount(
                            name = viewBinding.name.text?.toString() ?: "",
                            email = viewBinding.email.text?.toString()
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
