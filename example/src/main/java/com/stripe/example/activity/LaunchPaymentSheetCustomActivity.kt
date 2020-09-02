package com.stripe.example.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.example.databinding.ActivityPaymentSheetCustomBinding
import com.stripe.example.paymentsheet.EphemeralKey
import com.stripe.example.paymentsheet.PaymentSheetViewModel

class LaunchPaymentSheetCustomActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityPaymentSheetCustomBinding.inflate(layoutInflater)
    }

    private val viewModel: PaymentSheetViewModel by viewModels {
        PaymentSheetViewModel.Factory(
            application,
            getPreferences(Context.MODE_PRIVATE)
        )
    }

    private var paymentSheetFlowController: PaymentSheet.FlowController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewModel.inProgress.observe(this) {
            viewBinding.progressBar.visibility = if (it) View.VISIBLE else View.INVISIBLE
            viewBinding.clear.isEnabled = !it
        }
        viewModel.status.observe(this) {
            viewBinding.status.text = it
        }

        viewBinding.clear.setOnClickListener {
            viewModel.clearKeys()
            fetchEphemeralKey()
        }
        viewBinding.paymentMethod.setOnClickListener {
            this.paymentSheetFlowController?.presentPaymentOptions(this)
        }
        viewBinding.buyButton.setOnClickListener {
            this.paymentSheetFlowController?.confirmPayment(this)
        }
        fetchEphemeralKey()
    }

    private fun createPaymentSheetFlowController(
        ephemeralKey: EphemeralKey
    ) {
        viewModel.createPaymentIntent(
            "us",
            ephemeralKey.customer
        ).observe(this) { responseResult ->
            responseResult.fold(
                onSuccess = { json ->
                    viewModel.inProgress.postValue(false)
                    val secret = json.getString("secret")

                    PaymentSheet.FlowController.create(
                        this,
                        clientSecret = secret,
                        configuration = PaymentSheet.Configuration(
                            merchantDisplayName = "Widget Store",
                            customer = PaymentSheet.CustomerConfiguration(
                                id = ephemeralKey.customer,
                                ephemeralKeySecret = ephemeralKey.key
                            ),
                            googlePay = PaymentSheet.GooglePayConfiguration(
                                environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                                countryCode = "US"
                            ),
                            billingAddressCollection = PaymentSheet.BillingAddressCollectionLevel.Automatic
                        )
                    ) {
                        onPaymentSheetFlowControllerResult(it)
                    }
                },
                onFailure = {
                    viewModel.status.postValue("${viewModel.status.value}\nFailed: ${it.message}")
                }
            )
        }
    }

    private fun onPaymentSheetFlowControllerResult(
        result: PaymentSheet.FlowController.Result
    ) {
        when (result) {
            is PaymentSheet.FlowController.Result.Success -> {
                this.paymentSheetFlowController = result.flowController
            }
            is PaymentSheet.FlowController.Result.Failure -> {
                viewModel.status.postValue(
                    "Failed to create PaymentSheetFlowController: ${result.error.message}"
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (paymentSheetFlowController?.isPaymentOptionResult(requestCode) == true) {
            val paymentOption = paymentSheetFlowController?.onPaymentOptionResult(data)

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
                viewBinding.paymentMethod.text = "???"
                viewBinding.paymentMethod.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    null,
                    null,
                    null,
                    null
                )
                viewBinding.buyButton.isEnabled = false
            }
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

    private fun fetchEphemeralKey() {
        viewModel.fetchEphemeralKey()
            .observe(this) { newEphemeralKey ->
                if (newEphemeralKey != null) {
                    createPaymentSheetFlowController(newEphemeralKey)
                }
            }
    }
}
