package com.stripe.example.activity

import android.os.Bundle
import android.view.View
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.example.databinding.ActivityPaymentSheetCustomBinding

internal class LaunchPaymentSheetCustomActivity : BasePaymentSheetActivity() {
    private val viewBinding by lazy {
        ActivityPaymentSheetCustomBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        val paymentOptionCallback = PaymentOptionCallback { paymentOption ->
            onPaymentOption(paymentOption)
        }
        val paymentResultCallback = PaymentSheetResultCallback { paymentResult ->
            onPaymentSheetResult(paymentResult)
        }

        viewModel.inProgress.observe(this) {
            viewBinding.progressBar.visibility = if (it) View.VISIBLE else View.INVISIBLE
        }
        viewModel.status.observe(this) {
            viewBinding.status.text = it
        }

        viewBinding.buyButton.setOnClickListener {
            // TODO(mshafrir-stripe): handle click
        }
        fetchEphemeralKey()
    }

    private fun createPaymentSheetFlowController(
        customerConfig: PaymentSheet.CustomerConfiguration?
    ) {
        viewModel.createPaymentIntent(
            COUNTRY_CODE,
            customerConfig?.id
        ).observe(this) { responseResult ->
            responseResult.fold(
                onSuccess = { json ->
                    viewModel.inProgress.postValue(false)
                    val paymentIntentClientSecret = json.getString("secret")
                    onPaymentIntent(paymentIntentClientSecret, customerConfig)
                },
                onFailure = ::onError
            )
        }
    }

    private fun onPaymentIntent(
        paymentIntentClientSecret: String,
        customerConfig: PaymentSheet.CustomerConfiguration? = null
    ) {
        // handle result
    }

    private fun onPaymentOption(paymentOption: PaymentOption?) {
        when (paymentOption) {
            is PaymentOption.Succeeded -> handlePaymentOptionSuccess(paymentOption)
            is PaymentOption.Failed -> handlePaymentOptionFailure(paymentOption.error.localizedMessage)
            is PaymentOption.Canceled -> handlePaymentOptionFailure(paymentOption.mostRecentError?.localizedMessage)
            // TODO(michelleb-stripe): This is not a handled card type, or nothing has ever been selected
            // See DefaultFlowController.getPaymentOption and PaymentOptionFactory.create
            null -> handlePaymentOptionFailure(null)
        }
    }

    private fun handlePaymentOptionSuccess(paymentOption: PaymentOption.Succeeded) {
        viewBinding.paymentMethod.text = paymentOption.label
        viewBinding.paymentMethod.setCompoundDrawablesRelativeWithIntrinsicBounds(
            paymentOption.drawableResourceId,
            0,
            0,
            0
        )
        viewBinding.buyButton.isEnabled = true
    }

    private fun handlePaymentOptionFailure(failureMessage: String?) {
        viewBinding.paymentMethod.text = "Select"
        viewBinding.paymentMethod.setCompoundDrawablesRelativeWithIntrinsicBounds(
            null,
            null,
            null,
            null
        )
        viewBinding.buyButton.isEnabled = false

        failureMessage?.let {
            viewBinding.status.text = it
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

    private companion object {
        private const val COUNTRY_CODE = "us"
    }
}
