package com.stripe.example.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.isInvisible
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.example.R
import com.stripe.example.databinding.ActivityPaymentSheetCustomBinding

internal class LaunchPaymentSheetCustomActivity : BasePaymentSheetActivity() {
    private val viewBinding by lazy {
        ActivityPaymentSheetCustomBinding.inflate(layoutInflater)
    }

    private lateinit var flowController: PaymentSheet.FlowController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        flowController = PaymentSheet.FlowController.create(
            this,
            ::onPaymentOption,
            ::onPaymentSheetResult
        )

        viewModel.inProgress.observe(this) {
            viewBinding.progressBar.isInvisible = !it
        }

        viewModel.status.observe(this) {
            viewBinding.status.text = it
        }

        startCheckout()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.payment_sheet_custom, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.refresh_key) {
            resetViews()
            startCheckout()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun startCheckout() {
        prepareCheckout { customerConfig, clientSecret ->
            if (isSetupIntent) {
                flowController.configureWithSetupIntent(
                    clientSecret,
                    makeConfiguration(customerConfig),
                    ::onConfigured
                )
            } else {
                flowController.configureWithPaymentIntent(
                    clientSecret,
                    makeConfiguration(customerConfig),
                    ::onConfigured
                )
            }
        }
    }

    private fun makeConfiguration(
        customerConfig: PaymentSheet.CustomerConfiguration? = null
    ): PaymentSheet.Configuration {
        return PaymentSheet.Configuration(
            merchantDisplayName = merchantName,
            customer = customerConfig,
            googlePay = googlePayConfig
        )
    }

    private fun onConfigured(success: Boolean, error: Throwable?) {
        if (success) {
            onFlowControllerReady()
        } else {
            viewModel.status.postValue(
                "Failed to configure PaymentSheetFlowController: ${error?.message}"
            )
        }
    }

    private fun onFlowControllerReady() {
        viewBinding.paymentMethod.setOnClickListener {
            flowController.presentPaymentOptions()
        }
        viewBinding.buyButton.setOnClickListener {
            flowController.confirm()
        }
        onPaymentOption(flowController.getPaymentOption())
    }

    private fun onPaymentOption(paymentOption: PaymentOption?) {
        viewBinding.paymentMethod.isEnabled = true

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
            viewBinding.paymentMethod.text = "Select"
            viewBinding.paymentMethod.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null,
                null,
                null,
                null
            )
            viewBinding.buyButton.isEnabled = false
        }
    }

    private fun resetViews() {
        viewBinding.paymentMethod.text = "Loading..."
        viewBinding.paymentMethod.setCompoundDrawablesRelativeWithIntrinsicBounds(
            null,
            null,
            null,
            null
        )
        disableViews()
    }

    private fun disableViews() {
        viewBinding.paymentMethod.isEnabled = false
        viewBinding.buyButton.isEnabled = false
    }

    override fun onPaymentSheetResult(
        paymentResult: PaymentSheetResult
    ) {
        super.onPaymentSheetResult(paymentResult)
        disableViews()
    }
}
