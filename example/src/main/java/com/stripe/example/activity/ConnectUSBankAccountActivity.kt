package com.stripe.example.activity

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResult
import com.stripe.example.R
import com.stripe.example.Settings
import com.stripe.example.databinding.ConnectBankAccountExampleActivityBinding

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
            viewModel.inProgress.postValue(false)
            when (result) {
                is CollectBankAccountResult.Completed -> {
                    if (result.response.intent is PaymentIntent) {
                        viewModel.status
                            .postValue(
                                "Attached bank account to paymentIntent." +
                                    " secret: ${result.response.intent.clientSecret}. Attempting " +
                                    "to confirm payment intent. You should have webhooks setup" +
                                    "to check the final state of this payment intent."
                            )
                        confirmPaymentIntent(
                            ConfirmPaymentIntentParams.create(
                                clientSecret = requireNotNull(result.response.intent.clientSecret),
                                paymentMethodType = PaymentMethod.Type.USBankAccount
                            )
                        )
                    } else {
                        viewModel.status
                            .postValue(
                                "Attached bank account to setupIntent." +
                                    " secret: ${result.response.intent.clientSecret}. Attempting " +
                                    "to confirm setup intent."
                            )
                        confirmSetupIntent(
                            ConfirmSetupIntentParams.create(
                                clientSecret = requireNotNull(result.response.intent.clientSecret),
                                paymentMethodType = PaymentMethod.Type.USBankAccount
                            )
                        )
                    }
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
        viewBinding.setupButton.text =
            getString(R.string.setup_us_bank_account)
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
                        stripeAccountId = settings.stripeAccountId,
                        clientSecret = it.getString("secret"),
                        configuration = CollectBankAccountConfiguration.usBankAccount(
                            name = viewBinding.name.text?.toString() ?: "",
                            email = viewBinding.email.text?.toString()
                        )
                    )
                }
            }
        }

        viewBinding.setupButton.setOnClickListener {
            viewModel.createSetupIntent(
                "us"
            ).observe(this) { result ->
                result.onSuccess {
                    viewModel.status
                        .postValue("Collecting bank account information for setup")
                    launcher.presentWithSetupIntent(
                        publishableKey = settings.publishableKey,
                        clientSecret = it.getString("secret"),
                        configuration = CollectBankAccountConfiguration.usBankAccount(
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
