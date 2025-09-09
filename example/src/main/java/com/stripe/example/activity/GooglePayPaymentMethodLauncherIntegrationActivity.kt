package com.stripe.example.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.example.databinding.GooglePayActivityBinding

class GooglePayPaymentMethodLauncherIntegrationActivity : AppCompatActivity() {

    private val viewBinding: GooglePayActivityBinding by lazy {
        GooglePayActivityBinding.inflate(layoutInflater)
    }

    private val googlePayButton: GooglePayButton by lazy {
        viewBinding.googlePayButton
    }

    private val snackbarController: SnackbarController by lazy {
        SnackbarController(viewBinding.coordinator)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewBinding.progressBar.isVisible = true
        viewBinding.googlePayButton.isEnabled = false

        val googlePayLauncher = GooglePayPaymentMethodLauncher(
            activity = this,
            config = GooglePayPaymentMethodLauncher.Config(
                environment = GooglePayEnvironment.Test,
                merchantCountryCode = "US",
                merchantName = "Widget Store",
                billingAddressConfig = GooglePayPaymentMethodLauncher.BillingAddressConfig(
                    isRequired = true,
                    format = GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Full,
                    isPhoneNumberRequired = true
                )
            ),
            readyCallback = ::onGooglePayReady,
            resultCallback = ::onGooglePayResult
        )

        googlePayButton.setOnClickListener {
            viewBinding.progressBar.isVisible = true

            googlePayLauncher.present(
                currencyCode = "EUR",
                amount = 2500L,
            )
        }
    }

    private fun onGooglePayReady(isReady: Boolean) {
        snackbarController.show("Google Pay ready? $isReady")
        viewBinding.progressBar.isInvisible = true
        googlePayButton.isEnabled = isReady
    }

    private fun onGooglePayResult(result: GooglePayPaymentMethodLauncher.Result) {
        viewBinding.progressBar.isInvisible = true

        when (result) {
            is GooglePayPaymentMethodLauncher.Result.Completed -> {
                "Successfully created a PaymentMethod. ${result.paymentMethod}"
            }
            GooglePayPaymentMethodLauncher.Result.Canceled -> {
                "Customer cancelled Google Pay."
            }
            is GooglePayPaymentMethodLauncher.Result.Failed -> {
                "Google Pay failed: ${result.errorCode}: ${result.error.message}"
            }
        }.let {
            snackbarController.show(it)
        }
    }
}
