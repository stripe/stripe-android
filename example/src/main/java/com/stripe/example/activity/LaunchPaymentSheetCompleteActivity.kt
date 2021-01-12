package com.stripe.example.activity

import android.os.Bundle
import android.view.View
import com.stripe.android.paymentsheet.PaymentResult
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
                    createPaymentIntent(paymentSheet, customerConfig)
                }
            } else {
                createPaymentIntent(paymentSheet, null)
            }
        }
    }

    private fun createPaymentIntent(
        paymentSheet: PaymentSheet,
        customerConfig: PaymentSheet.CustomerConfiguration?
    ) {
        viewModel.createPaymentIntent(
            "us",
            customerId = customerConfig?.id
        ).observe(this) {
            it.fold(
                onSuccess = { json ->
                    val clientSecret = json.getString("secret")

                    onPaymentIntent(
                        paymentSheet,
                        clientSecret,
                        customerConfig
                    )
                },
                onFailure = {
                    viewModel.status.postValue(viewModel.status.value + "\nFailed: ${it.message}")
                }
            )
        }
    }

    private fun onPaymentIntent(
        paymentSheet: PaymentSheet,
        paymentIntentClientSecret: String,
        customerConfig: PaymentSheet.CustomerConfiguration?
    ) {
        viewModel.inProgress.postValue(false)

        paymentSheet.present(
            paymentIntentClientSecret,
            PaymentSheet.Configuration(
                merchantDisplayName = merchantName,
                customer = customerConfig,
                googlePay = googlePayConfig,
                billingAddressCollection = billingAddressCollection
            )
        )
    }

    private fun onPaymentSheetResult(
        paymentResult: PaymentResult
    ) {
        val statusString = when (paymentResult) {
            is PaymentResult.Cancelled -> {
                "MC Completed with status: Cancelled"
            }
            is PaymentResult.Failed -> {
                "MC Completed with status: Failed(${paymentResult.error.message}"
            }
            is PaymentResult.Succeeded -> {
                "MC Completed with status: Succeeded"
            }
        }
        viewModel.status.value = viewModel.status.value + "\n\n$statusString"
    }

    override fun onRefreshEphemeralKey() {
        fetchEphemeralKey()
    }
}
