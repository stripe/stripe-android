package com.stripe.example.activity

import android.os.Bundle
import android.view.View
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.example.databinding.ActivityPaymentSheetCompleteBinding

internal class LaunchPaymentSheetCompleteActivity : BasePaymentSheetActivity() {
    private val viewBinding by lazy {
        ActivityPaymentSheetCompleteBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        val paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)

        viewModel.inProgress.observe(this) {
            viewBinding.progressBar.visibility = if (it) View.VISIBLE else View.INVISIBLE
            viewBinding.launch.isEnabled = !it
        }
        viewModel.status.observe(this) {
            viewBinding.status.text = it
        }

        viewBinding.launch.setOnClickListener {
            if (isCustomerEnabled) {
                fetchEphemeralKey { customerConfig ->
                    createIntent(paymentSheet, customerConfig)
                }
            } else {
                createIntent(paymentSheet, null)
            }
        }
    }

    private fun createIntent(
        paymentSheet: PaymentSheet,
        customerConfig: PaymentSheet.CustomerConfiguration?
    ) {
        if (isSetupIntent) {
            createSetupIntent(paymentSheet, customerConfig)
        } else (
            createPaymentIntent(paymentSheet, customerConfig)
            )
    }

    private fun createPaymentIntent(
        paymentSheet: PaymentSheet,
        customerConfig: PaymentSheet.CustomerConfiguration?
    ) {
        viewModel.createPaymentIntent(
            COUNTRY_CODE,
            customerId = customerConfig?.id
        ).observe(this) {
            it.fold(
                onSuccess = { json ->
                    val clientSecret = json.getString("secret")

                    onIntentCreated(
                        paymentSheet,
                        clientSecret,
                        customerConfig
                    )
                },
                onFailure = ::onError
            )
        }
    }

    private fun createSetupIntent(
        paymentSheet: PaymentSheet,
        customerConfig: PaymentSheet.CustomerConfiguration?
    ) {
        viewModel.createSetupIntent(
            COUNTRY_CODE,
            customerId = customerConfig?.id
        ).observe(this) {
            it.fold(
                onSuccess = { json ->
                    val clientSecret = json.getString("secret")

                    onIntentCreated(
                        paymentSheet,
                        clientSecret,
                        customerConfig
                    )
                },
                onFailure = ::onError
            )
        }
    }

    private fun onIntentCreated(
        paymentSheet: PaymentSheet,
        intentClientSecret: String,
        customerConfig: PaymentSheet.CustomerConfiguration?
    ) {
        viewModel.inProgress.postValue(false)

        paymentSheet.present(
            intentClientSecret,
            PaymentSheet.Configuration(
                merchantDisplayName = merchantName,
                customer = customerConfig,
                googlePay = googlePayConfig,
                billingAddressCollection = billingAddressCollection
            )
        )
    }

    override fun onRefreshEphemeralKey() {
        fetchEphemeralKey()
    }

    private companion object {
        private const val COUNTRY_CODE = "us"
    }
}
