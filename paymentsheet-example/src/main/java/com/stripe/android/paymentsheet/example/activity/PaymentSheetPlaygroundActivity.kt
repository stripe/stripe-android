package com.stripe.android.paymentsheet.example.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.lifecycle.lifecycleScope
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.databinding.ActivityPaymentSheetPlaygroundBinding
import com.stripe.android.paymentsheet.example.repository.Repository
import com.stripe.android.paymentsheet.example.viewmodel.PaymentSheetPlaygroundViewModel
import com.stripe.android.paymentsheet.model.PaymentOption
import kotlinx.coroutines.launch

internal class PaymentSheetPlaygroundActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityPaymentSheetPlaygroundBinding.inflate(layoutInflater)
    }

    private val viewModel: PaymentSheetPlaygroundViewModel by viewModels {
        PaymentSheetPlaygroundViewModel.Factory(
            application
        )
    }

    private val customer: Repository.CheckoutCustomer
        get() = when (viewBinding.customerRadioGroup.checkedRadioButtonId) {
            R.id.guest_customer_button -> Repository.CheckoutCustomer.New
            R.id.new_customer_button -> {
                viewModel.temporaryCustomerId?.let {
                    Repository.CheckoutCustomer.WithId(it)
                } ?: Repository.CheckoutCustomer.New
            }
            else -> Repository.CheckoutCustomer.Returning
        }

    private val googlePayConfig: PaymentSheet.GooglePayConfiguration?
        get() = when (viewBinding.googlePayRadioGroup.checkedRadioButtonId) {
            R.id.google_pay_on_button -> {
                PaymentSheet.GooglePayConfiguration(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                    countryCode = "US"
                )
            }
            else -> null
        }

    private val mode: Repository.CheckoutMode
        get() = when (viewBinding.modeRadioGroup.checkedRadioButtonId) {
            R.id.mode_payment_button -> Repository.CheckoutMode.Payment
            else -> Repository.CheckoutMode.Setup
        }

    private val merchantName = "Example, Inc."

    private lateinit var paymentSheet: PaymentSheet
    private lateinit var flowController: PaymentSheet.FlowController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)
        flowController = PaymentSheet.FlowController.create(
            this,
            ::onPaymentOption,
            ::onPaymentSheetResult
        )

        viewBinding.reloadButton.setOnClickListener {
            lifecycleScope.launch {
                viewModel.prepareCheckout(customer, mode)
            }
        }

        viewBinding.completeCheckoutButton.setOnClickListener {
            startCompleteCheckout()
        }

        viewBinding.customCheckoutButton.setOnClickListener {
            flowController.confirm()
        }

        viewModel.status.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_LONG).show()
        }

        viewModel.inProgress.observe(this) {
            viewBinding.progressBar.isInvisible = !it
        }

        viewModel.readyToCheckout.observe(this) { isReady ->
            if (isReady) {
                viewBinding.completeCheckoutButton.isEnabled = true
                configureCustomCheckout()
            } else {
                disableViews()
            }
        }
    }

    private fun disableViews() {
        viewBinding.completeCheckoutButton.isEnabled = false
        viewBinding.customCheckoutButton.isEnabled = false
        viewBinding.paymentMethod.setOnClickListener(null)
        viewBinding.paymentMethod.setText(R.string.loading)
        viewBinding.paymentMethod.setCompoundDrawables(null, null, null, null)
    }

    private fun startCompleteCheckout() {
        val clientSecret = viewModel.clientSecret.value ?: return
        val customerConfig = viewModel.customerConfig.value ?: return

        if (viewModel.checkoutMode == Repository.CheckoutMode.Setup) {
            paymentSheet.presentWithSetupIntent(
                clientSecret,
                PaymentSheet.Configuration(
                    merchantDisplayName = merchantName,
                    customer = customerConfig,
                    googlePay = googlePayConfig,
                )
            )
        } else {
            paymentSheet.presentWithPaymentIntent(
                clientSecret,
                PaymentSheet.Configuration(
                    merchantDisplayName = merchantName,
                    customer = customerConfig,
                    googlePay = googlePayConfig,
                )
            )
        }
    }

    private fun configureCustomCheckout() {
        val clientSecret = viewModel.clientSecret.value ?: return
        val customerConfig = viewModel.customerConfig.value ?: return

        if (viewModel.checkoutMode == Repository.CheckoutMode.Setup) {
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
            viewBinding.paymentMethod.setOnClickListener {
                flowController.presentPaymentOptions()
            }

            onPaymentOption(flowController.getPaymentOption())
        } else {
            viewModel.status.value =
                "Failed to configure PaymentSheetFlowController: ${error?.message}"
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
            viewBinding.customCheckoutButton.isEnabled = true
        } else {
            viewBinding.paymentMethod.setText(R.string.select)
            viewBinding.paymentMethod.setCompoundDrawables(null, null, null, null)
            viewBinding.customCheckoutButton.isEnabled = false
        }
    }

    private fun onPaymentSheetResult(paymentResult: PaymentSheetResult) {
        if (paymentResult !is PaymentSheetResult.Canceled) {
            disableViews()
        }

        viewModel.status.value = paymentResult.toString()
    }
}
