package com.stripe.example.activity

import android.os.Bundle
import android.view.View
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.example.databinding.ActivityPaymentSheetCustomBinding

internal class LaunchPaymentSheetCustomActivity : BasePaymentSheetActivity() {
    private val viewBinding by lazy {
        ActivityPaymentSheetCustomBinding.inflate(layoutInflater)
    }

    private lateinit var flowController: PaymentSheet.FlowController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        val paymentOptionCallback = PaymentOptionCallback { paymentOption ->
            onPaymentOption(paymentOption)
        }
        val paymentResultCallback = PaymentSheetResultCallback { paymentResult ->
            onPaymentSheetResult(paymentResult)
        }

        flowController = PaymentSheet.FlowController.create(
            this,
            paymentOptionCallback,
            paymentResultCallback
        )

        viewModel.inProgress.observe(this) {
            viewBinding.progressBar.visibility = if (it) View.VISIBLE else View.INVISIBLE
        }
        viewModel.status.observe(this) {
            viewBinding.status.text = it
        }
    }

    private fun createPaymentSheetFlowController(
        customerConfig: PaymentSheet.CustomerConfiguration?
    ) {
        viewModel.createPaymentIntent(
            "us",
            customerConfig?.id
        ).observe(this) { responseResult ->
            responseResult.fold(
                onSuccess = { json ->
                    viewModel.inProgress.postValue(false)
                    val paymentIntentClientSecret = json.getString("secret")
                    onPaymentIntent(paymentIntentClientSecret, customerConfig)
                },
                onFailure = {
                    viewModel.status.postValue("${viewModel.status.value}\nFailed: ${it.message}")
                }
            )
        }
    }

    private fun onPaymentIntent(
        paymentIntentClientSecret: String,
        customerConfig: PaymentSheet.CustomerConfiguration? = null
    ) {
        flowController.configure(
            paymentIntentClientSecret = paymentIntentClientSecret,
            configuration = PaymentSheet.Configuration(
                merchantDisplayName = merchantName,
                customer = customerConfig,
                googlePay = googlePayConfig,
                billingAddressCollection = billingAddressCollection
            )
        ) { isReady, error ->
            if (isReady) {
                onFlowControllerReady()
            } else {
                viewModel.status.postValue(
                    "Failed to create PaymentSheetFlowController: ${error?.message}"
                )
            }
        }
    }

    private fun onFlowControllerReady() {
        viewBinding.paymentMethod.setOnClickListener {
            flowController.presentPaymentOptions()
        }
        viewBinding.buyButton.setOnClickListener {
            flowController.confirmPayment()
        }
        onPaymentOption(flowController.getPaymentOption())
    }

    private fun onPaymentSheetResult(
        paymentResult: PaymentResult
    ) {
        val statusString = when (paymentResult) {
            is PaymentResult.Canceled -> {
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

    private fun onPaymentOption(paymentOption: PaymentOption?) {
        if (paymentOption != null) {
            viewBinding.paymentMethod.text = paymentOption.label
            viewBinding.paymentMethod.setCompoundDrawablesRelativeWithIntrinsicBounds(
                paymentOption.drawableResourceId,
                0,
                0,
                0
            )
            viewBinding.buyButton.isEnabled = true
        } else {
            viewBinding.paymentMethod.text = "N/A"
            viewBinding.paymentMethod.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null,
                null,
                null,
                null
            )
            viewBinding.buyButton.isEnabled = false
        }
    }

    override fun onRefreshEphemeralKey() {
        if (isCustomerEnabled) {
            fetchEphemeralKey { customerConfig ->
                createPaymentSheetFlowController(customerConfig)
            }
        } else {
            createPaymentSheetFlowController(null)
        }
    }
}
