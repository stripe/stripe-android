package com.stripe.example.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.example.databinding.ActivityPaymentSheetCustomBinding

internal class LaunchPaymentSheetCustomActivity : BasePaymentSheetActivity() {
    private val viewBinding by lazy {
        ActivityPaymentSheetCustomBinding.inflate(layoutInflater)
    }

    private var paymentSheetFlowController: PaymentSheet.FlowController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewModel.inProgress.observe(this) {
            viewBinding.progressBar.visibility = if (it) View.VISIBLE else View.INVISIBLE
        }
        viewModel.status.observe(this) {
            viewBinding.status.text = it
        }

        viewBinding.paymentMethod.setOnClickListener {
            this.paymentSheetFlowController?.presentPaymentOptions(this)
        }
        viewBinding.buyButton.setOnClickListener {
            this.paymentSheetFlowController?.confirmPayment(this)
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
        PaymentSheet.FlowController.create(
            this,
            clientSecret = paymentIntentClientSecret,
            configuration = PaymentSheet.Configuration(
                merchantDisplayName = merchantName,
                customer = customerConfig,
                googlePay = googlePayConfig,
                billingAddressCollection = billingAddressCollection
            )
        ) {
            onPaymentSheetFlowControllerResult(it)
        }
    }

    private fun onPaymentSheetFlowControllerResult(
        result: PaymentSheet.FlowController.Result
    ) {
        when (result) {
            is PaymentSheet.FlowController.Result.Success -> {
                onPaymentSheetFlowController(result.flowController)
            }
            is PaymentSheet.FlowController.Result.Failure -> {
                viewModel.status.postValue(
                    "Failed to create PaymentSheetFlowController: ${result.error.message}"
                )
            }
        }
    }

    private fun onPaymentSheetFlowController(flowController: PaymentSheet.FlowController) {
        onPaymentOption(flowController.getPaymentOption())
        this.paymentSheetFlowController = flowController
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (paymentSheetFlowController?.isPaymentOptionResult(requestCode) == true) {
            val paymentOption = paymentSheetFlowController?.onPaymentOptionResult(data)
            onPaymentOption(paymentOption)
        } else if (paymentSheetFlowController?.isPaymentResult(requestCode, data) == true) {
            paymentSheetFlowController?.onPaymentResult(
                requestCode,
                data,
                object : ApiResultCallback<PaymentIntentResult> {
                    override fun onSuccess(result: PaymentIntentResult) {
                        viewModel.status.postValue(
                            "Completed payment with outcome: ${result.outcome}"
                        )
                    }

                    override fun onError(e: Exception) {
                        viewModel.status.postValue("Payment failed: ${e.message}")
                    }
                }
            )
        }
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
