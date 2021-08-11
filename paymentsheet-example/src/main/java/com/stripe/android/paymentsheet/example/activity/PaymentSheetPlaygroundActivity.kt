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
            R.id.guest_customer_button -> Repository.CheckoutCustomer.Guest
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
                    countryCode = "US",
                    currencyCode = currency.value
                )
            }
            else -> null
        }

    private val currency: Repository.CheckoutCurrency
        get() = when (viewBinding.currencyRadioGroup.checkedRadioButtonId) {
            R.id.currency_usd_button -> Repository.CheckoutCurrency.USD
            else -> Repository.CheckoutCurrency.EUR
        }

    private val mode: Repository.CheckoutMode
        get() = when (viewBinding.modeRadioGroup.checkedRadioButtonId) {
            R.id.mode_payment_button -> Repository.CheckoutMode.Payment
            R.id.mode_payment_with_setup_button -> Repository.CheckoutMode.PaymentWithSetup
            else -> Repository.CheckoutMode.Setup
        }

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
                viewModel.prepareCheckout(customer, currency, mode)
            }
        }

        viewBinding.completeCheckoutButton.setOnClickListener {
            startCompleteCheckout()
        }

        viewBinding.customCheckoutButton.setOnClickListener {
            flowController.confirm()
        }

        viewBinding.paymentMethod.setOnClickListener {
            flowController.presentPaymentOptions()
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

        disableViews()
    }

    private fun disableViews() {
        viewBinding.completeCheckoutButton.isEnabled = false
        viewBinding.customCheckoutButton.isEnabled = false
        viewBinding.paymentMethod.isClickable = false
    }

    private fun startCompleteCheckout() {
        val clientSecret = viewModel.clientSecret.value ?: return

        if (viewModel.checkoutMode == Repository.CheckoutMode.Setup) {
            paymentSheet.presentWithSetupIntent(
                clientSecret,
                makeConfiguration()
            )
        } else {
            paymentSheet.presentWithPaymentIntent(
                clientSecret,
                makeConfiguration()
            )
        }
    }

    private fun configureCustomCheckout() {
        val clientSecret = viewModel.clientSecret.value ?: return

        if (viewModel.checkoutMode == Repository.CheckoutMode.Setup) {
            flowController.configureWithSetupIntent(
                clientSecret,
                makeConfiguration(),
                ::onConfigured
            )
        } else {
            flowController.configureWithPaymentIntent(
                clientSecret,
                makeConfiguration(),
                ::onConfigured
            )
        }
    }

    private fun makeConfiguration(): PaymentSheet.Configuration {
        return PaymentSheet.Configuration(
            merchantDisplayName = merchantName,
            customer = viewModel.customerConfig.value,
            googlePay = googlePayConfig
        )
    }

    private fun onConfigured(success: Boolean, error: Throwable?) {
        if (success) {
            viewBinding.paymentMethod.isClickable = true
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

    companion object {
        private const val merchantName = "Example, Inc."
    }
}
